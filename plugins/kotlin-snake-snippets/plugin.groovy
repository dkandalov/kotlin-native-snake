import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import liveplugin.PluginUtil

import static liveplugin.PluginUtil.*

def knBasePath = System.getProperty("user.home") + "/IdeaProjects/kotlin-native/"

registerAction("kotlinSnakeProjectPopup", "ctrl shift K") { AnActionEvent event ->
    def project = event.project
    def popupMenuDescription = [
            "gradle"     : { addNCursesToGradle(project) },
            "log"        : { pasteLog(project) },
            "shouldEqual": { pasteShouldEqual(project) },
            "--"         : Separator.instance,
            "String.kt"  : { openInEditor("$knBasePath/runtime/src/main/kotlin/kotlin/String.kt", project) },
            "KString"    : { openInEditor("$knBasePath/runtime/src/main/cpp/KString.cpp", project) },
            "ncurses.kt" : {
                def virtualFile = openInEditor("/Users/dko0618/IdeaProjects/katas/kotlin-native/hello-snake/build/konan/libs/macos_x64/ncurses.klib-build/kotlin/ncurses/ncurses.kt", project)
                if (virtualFile != null) currentEditorIn(project).caretModel.moveToLogicalPosition(new LogicalPosition(157, 0))
            },
    ]
    showPopupMenu(popupMenuDescription, "Snake")
}

registerAction("CustomBuildAll", "ctrl alt F9") { AnActionEvent event ->
    liveplugin.implementation.Actions.executeRunConfiguration("Build All", event.project)
}

static addNCursesToGradle(Project project) {
    def document = liveplugin.PluginUtil.document(findFileByName("build.gradle", project))

    runDocumentWriteAction(project, document) {
        document.text = """
buildscript {
    repositories {
        mavenCentral()
        maven {
            url "https://dl.bintray.com/jetbrains/kotlin-native-dependencies"
        }
    }
    dependencies {
        classpath "org.jetbrains.kotlin:kotlin-native-gradle-plugin:0.8.2"
    }
}

apply plugin: "konan"

konanArtifacts {
    program("snake") {
        srcDir "src"
        //libraries { artifact "ncurses" }
    }
    program("snakeTest") {
        srcDir "src"
        srcDir "test"
        //libraries { artifact "ncurses" }
    }
    //interop("ncurses") {
    //    defFile "ncurses.def"
    //}
}
""".trim()
    }
}

static pasteShouldEqual(Project project) {
    def document = currentDocumentIn(project)
    def editor = currentEditorIn(project)

    runDocumentWriteAction(project, document) {
        document.insertString(editor.caretModel.offset, """
infix fun <T> T.shouldEqual(that: T) = assertEquals(actual = this, expected = that)
""")
    }
}

static pasteLog(Project project) {
    def document = currentDocumentIn(project)
    def editor = currentEditorIn(project)

    runDocumentWriteAction(project, document) {
        document.insertString(editor.caretModel.offset, """fun log(message: String) {
    val file = fopen("log.txt", "a") ?: error("couldn't open file")
    try {
        val line = message + "\\n"
        fwrite(line.cstr, line.length.toLong(), 1, file)
    } finally {
        fclose(file)
    }
}
""")
    }
}

if (!isIdeStartup) show("Reloaded Kotlin/Native snake tools")
