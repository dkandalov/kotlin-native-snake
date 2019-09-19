import com.intellij.execution.ExecutionListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.NotNull

import static com.intellij.execution.ExecutionManager.EXECUTION_TOPIC
import static com.intellij.openapi.progress.PerformInBackgroundOption.ALWAYS_BACKGROUND
import static liveplugin.PluginUtil.*

def userHome = System.getProperty("user.home")
def knBasePath = userHome + "/IdeaProjects/kotlin-native/"

registerAction("kotlinSnakeProjectPopup", "ctrl shift K") { AnActionEvent event ->
    def project = event.project
    def popupMenuDescription = [
            "String.kt"  : { openFile("$knBasePath/runtime/src/main/kotlin/kotlin/String.kt", 17, project) },
            "KString.cpp": { openFile("$knBasePath/runtime/src/main/cpp/KString.cpp", 1168, project) },
            "ncurses.h" : { openFile("/usr/local/Cellar/ncurses/6.1/include/ncurses.h", 679, project) },
            "valgrid"    : { openInEditor("$userHome/IdeaProjects/kotlin-native-snake/massif.out.printed", project) },
            "sdl"        : { openInEditor("$userHome/IdeaProjects/kotlin-native-snake/snake-sdl/src/main.kt", project) }
//            "log"        : { pasteLog(project) },
    ]
    showPopupMenu(popupMenuDescription, "Snake")
}

// Do this because gradle tasks don't display any notifications when running in presentation mode.
// See https://youtrack.jetbrains.com/issue/IDEA-222825
registerProjectListener(pluginDisposable) { Project project ->
    project.messageBus.connect(pluginDisposable).subscribe(EXECUTION_TOPIC, new ExecutionListener() {
        @java.lang.Override
        void processStarting(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
            if (env.runProfile.name.contains("snake [")) {
                def latch = new java.util.concurrent.CountDownLatch(1)
                registerConsoleListener("CustomBuildAll-ConsoleListener") { consoleText ->
                    if (consoleText.contains("Task execution finished")) {
                        latch.countDown()
                    }
                }
                doInBackground(env.runProfile.name, true, ALWAYS_BACKGROUND, { latch.await() }, { latch.countDown() })
            }
            //show(env.runProfile.name + "!!!")
        }
    })
}

//registerAction("CustomBuildAll", "ctrl alt F9") { AnActionEvent event ->
//    def latch = new java.util.concurrent.CountDownLatch(1)
//    registerConsoleListener("CustomBuildAll-ConsoleListener") { consoleText ->
//        if (consoleText.contains("Task execution finished 'linkDebugExecutableSnake'")) {
//            latch.countDown()
//        }
//    }
//    doInBackground("Building snake", true, ALWAYS_BACKGROUND, { latch.await() }, { latch.countDown() })
//    liveplugin.implementation.Actions.executeRunConfiguration("snake [linkDebugExecutableSnake]", event.project)
//}

static openFile(filePath, line, project) {
    def virtualFile = openInEditor(filePath, project)
    if (virtualFile != null) currentEditorIn(project).caretModel.moveToLogicalPosition(new LogicalPosition(line, 0))
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
