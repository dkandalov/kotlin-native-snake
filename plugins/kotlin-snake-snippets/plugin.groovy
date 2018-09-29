import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import liveplugin.PluginUtil

import static liveplugin.PluginUtil.*

def userHome = System.getProperty("user.home")
def knBasePath = userHome + "/IdeaProjects/kotlin-native/"

registerAction("kotlinSnakeProjectPopup", "ctrl shift K") { AnActionEvent event ->
    def project = event.project
    def popupMenuDescription = [
            "String.kt"  : { openFile("$knBasePath/runtime/src/main/kotlin/kotlin/String.kt", 17, project) },
            "KString.cpp": { openFile("$knBasePath/runtime/src/main/cpp/KString.cpp", 1168, project) },
            "ncurses.kt" : { openFile("$userHome/IdeaProjects/katas/kotlin-native/hello-snake/build/konan/libs/macos_x64/ncurses.klib-build/kotlin/ncurses/ncurses.kt", 679, project) },
            "gradle"     : { addNCursesToGradle(project) },
            "valgrid"    : { openInEditor("$userHome/IdeaProjects/kotlin-native-snake/massif.out.printed", project) },
            "sdl"        : { openInEditor("$userHome/IdeaProjects/kotlin-native-snake/snake-sdl/src/main.kt", project) }
//            "log"        : { pasteLog(project) },
//            "shouldEqual": { pasteShouldEqual(project) },
//            "--"         : Separator.instance,
    ]
    showPopupMenu(popupMenuDescription, "Snake")
}

// Do this because CLion builds each "program" separately,
// i.e. compiling and running tests won't compile the main program.
// And because compilation is quite slow it might be better to build everything in one go.
registerAction("CustomBuildAll", "ctrl alt F9") { AnActionEvent event ->
    liveplugin.implementation.Actions.executeRunConfiguration("Build All", event.project)
}

static openFile(filePath, line, project) {
    def virtualFile = openInEditor(filePath, project)
    if (virtualFile != null) currentEditorIn(project).caretModel.moveToLogicalPosition(new LogicalPosition(line, 0))
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
    interop("ncurses") {
        defFile "ncurses.def"
    }
    program("snake") {
        srcDir "src"
        libraries { artifact "ncurses" }
    }
    program("snakeTest") {
        srcDir "src"
        srcDir "test"
        libraries { artifact "ncurses" }
    }
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
