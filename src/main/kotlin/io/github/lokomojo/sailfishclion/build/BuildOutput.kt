package io.github.lokomojo.sailfishclion.build

import com.intellij.build.BuildViewManager
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.events.FinishBuildEvent
import com.intellij.build.events.OutputBuildEvent
import com.intellij.build.events.StartBuildEvent
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.build.events.impl.SuccessResultImpl
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import java.util.UUID

class BuildOutput(project: Project, title: String, workingDirectory: String, private val cancelled: () -> Boolean) {

    private val buildId: Any = UUID.randomUUID()
    private val viewManager = project.getService(BuildViewManager::class.java)

    init {
        val descriptor = DefaultBuildDescriptor(buildId, title, workingDirectory, System.currentTimeMillis())
        descriptor.setActivateToolWindowWhenAdded(true)
        viewManager.onEvent(buildId, StartBuildEvent.builder(title, descriptor).build())
    }

    fun message(text: String) {
        viewManager.onEvent(buildId, OutputBuildEvent.builder("$text\n").withParentId(buildId).withOutputType(ProcessOutputType.SYSTEM).build())
    }

    fun run(commandLine: GeneralCommandLine): Boolean {
        message(commandLine.commandLineString)
        val handler = OSProcessHandler(commandLine)
        handler.addProcessListener(object : ProcessListener {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                val type = if (ProcessOutputType.isStderr(outputType)) ProcessOutputType.STDERR else ProcessOutputType.STDOUT
                viewManager.onEvent(buildId, OutputBuildEvent.builder(event.text).withParentId(buildId).withOutputType(type).build())
            }
        })
        handler.startNotify()
        while (!handler.waitFor(POLL_INTERVAL_MS)) {
            if (cancelled()) {
                message("Cancelled")
                handler.destroyProcess()
                handler.waitFor()
                return false
            }
        }
        return handler.exitCode == 0
    }

    fun finish(success: Boolean, message: String) {
        val result = if (success) SuccessResultImpl() else FailureResultImpl(message)
        viewManager.onEvent(buildId, FinishBuildEvent.builder(buildId, message, result).withTime(System.currentTimeMillis()).build())
    }

    companion object {
        private const val POLL_INTERVAL_MS = 200L
    }
}
