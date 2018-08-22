import com.intellij.execution.ExecutionManager
import com.intellij.execution.Executor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.ExecutionConsole
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.actions.CloseAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import liveplugin.implementation.Misc
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

import javax.swing.*
import java.awt.*
import java.util.concurrent.atomic.AtomicReference

import static liveplugin.PluginUtil.*

// https://gist.github.com/dkandalov/cccc4b5be7ffa897727dc69854d1ec22

CloseAction lastCloseAction = null

registerAction("Show man page", "ctrl shift M") { event ->
	def editor = currentEditorIn(event.project)

	def term = editor.selectionModel.selectedText?.toLowerCase()
	if (term == null) {
		def offset = editor.caretModel.offset
		def text = editor.document.charsSequence.toString()
		def from = (0..offset).reverse().find { i -> !Character.isJavaIdentifierPart(text.charAt(i)) } + 1
		def to = (offset..text.size()).find { i -> !Character.isJavaIdentifierPart(text.charAt(i)) }
		term = text.substring(from, to)?.toLowerCase()
	}
	if (term == null || term.isEmpty()) return

	def manText = execute2("${pluginPath}/man-plain-text", term).stdout as String
	def (consoleView, closeAction) = showInConsole2(manText, "man $term", event.project, ConsoleViewContentType.NORMAL_OUTPUT)
	consoleView.scrollTo(0)

	if (lastCloseAction != null) {
		lastCloseAction.actionPerformed(anActionEvent())
	}
	lastCloseAction = closeAction
}

static execute2(String... commandAndArgs) {
	def cmdProc = Runtime.getRuntime().exec(commandAndArgs)
	def stdout = ""
	def stderr = ""

	String line
	def stdoutReader = new BufferedReader(new InputStreamReader(cmdProc.getInputStream()))
	while ((line = stdoutReader.readLine()) != null) {
		stdout += line + "\n"
	}
	BufferedReader stderrReader = new BufferedReader(new InputStreamReader(cmdProc.getErrorStream()))
	while ((line = stderrReader.readLine()) != null) {
		stderr += line + "\n"
	}

	[exitCode: cmdProc.exitValue(), stdout: stdout, stderr: stdout]
}

// Copied from LivePlugin source to be able to invoke CloseAction.
static showInConsole2(@Nullable message, String consoleTitle = "", @NotNull Project project,
                      ConsoleViewContentType contentType = guessContentTypeOf(message)) {
	AtomicReference result = new AtomicReference(null)
	// Use reference for consoleTitle because get groovy Reference class like in this bug http://jira.codehaus.org/browse/GROOVY-5101
	AtomicReference<String> titleRef = new AtomicReference(consoleTitle)

	invokeOnEDT {
		ConsoleView console = TextConsoleBuilderFactory.instance.createBuilder(project).console
		console.print(Misc.asString(message), contentType)

		DefaultActionGroup toolbarActions = new DefaultActionGroup()
		def consoleComponent = new MyConsolePanel(console, toolbarActions)
		RunContentDescriptor descriptor = new RunContentDescriptor(console, null, consoleComponent, titleRef.get()) {
			@Override boolean isContentReuseProhibited() { true }
			@Override Icon getIcon() { AllIcons.Nodes.Plugin }
		}
		Executor executor = DefaultRunExecutor.runExecutorInstance

		def closeAction = new CloseAction(executor, descriptor, project)
		toolbarActions.add(closeAction)
		console.createConsoleActions().each { toolbarActions.add(it) }

		ExecutionManager.getInstance(project).contentManager.showRunContent(executor, descriptor)
		result.set([console, closeAction])
	}
	result.get()
}

class MyConsolePanel extends JPanel {
	MyConsolePanel(ExecutionConsole consoleView, ActionGroup toolbarActions) {
		super(new BorderLayout())
		def toolbarPanel = new JPanel(new BorderLayout())
		toolbarPanel.add(ActionManager.instance.createActionToolbar(ActionPlaces.UNKNOWN, toolbarActions, false).component)
		add(toolbarPanel, BorderLayout.WEST)
		add(consoleView.component, BorderLayout.CENTER)
	}
}

if (!isIdeStartup) {
	show("Reloaded man page plugin")
}