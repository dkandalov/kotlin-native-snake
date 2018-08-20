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
            "gradle"             : { pasteGradleRepos(project) },
            "log"                : { pasteLog(project) },
            "shouldEqual"        : { pasteShouldEqual(project) },
            "--"                 : Separator.instance,
            "KString"            : { openInEditor("$knBasePath/runtime/src/main/cpp/KString.cpp", project) },
            "backend/Collections": {
                def virtualFile = openInEditor("$knBasePath/backend.native/build/stdlib/kotlin/collections/Collections.kt", project)
                if (virtualFile != null) currentEditorIn(project).caretModel.moveToLogicalPosition(new LogicalPosition(75, 0))
            },
            "runtime/Collections": {
                def virtualFile = openInEditor("$knBasePath/runtime/src/main/kotlin/kotlin/collections/Collections.kt", project)
                if (virtualFile != null) currentEditorIn(project).caretModel.moveToLogicalPosition(new LogicalPosition(53, 0))
            },
            "Array.kt"           : { openInEditor("$knBasePath/runtime/src/main/kotlin/kotlin/Array.kt", project) },
            "Array.cpp"          : {
                def virtualFile = openInEditor("$knBasePath/runtime/src/main/cpp/Arrays.cpp", project)
                if (virtualFile != null) currentEditorIn(project).caretModel.moveToLogicalPosition(new LogicalPosition(59, 0))
            },
    ]
    showPopupMenu(popupMenuDescription, "Snake")
}

static pasteGradleRepos(Project project) {
    def document = currentDocumentIn(project)
    def editor = currentEditorIn(project)

    runDocumentWriteAction(project, document) {
        document.insertString(editor.caretModel.offset, """
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
    }
    program("snakeTest") {
        srcDir "src"
        srcDir "test"
    }
}
""")
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

if (!isIdeStartup) show("Reloaded Kotlin snake popup")
