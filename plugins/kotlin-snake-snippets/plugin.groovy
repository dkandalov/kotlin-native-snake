import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

import static liveplugin.PluginUtil.*

registerAction("kotlinSnakeProjectPopup", "ctrl shift K") { AnActionEvent event ->
	def project = event.project
	def popupMenuDescription = [
			"gradle":{ pasteGradleRepos(project) },
			"log":{ pasteLog(project) },
			"shouldEqual":{ pasteShouldEqual(project) }
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
