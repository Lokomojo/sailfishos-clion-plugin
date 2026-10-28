package io.github.lokomojo.sailfishclion.sfdk

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.progress.ProgressManager
import java.nio.file.Path

class SfdkCli(private val sfdk: Path, private val options: Map<String, String> = emptyMap()) {

    fun listProjectTypes(): List<SfdkProjectType> {
        val lines = run("init", "--list-types").stdoutLines
        val types = mutableListOf<SfdkProjectType>()
        var pending: Pair<String, String>? = null
        for (line in lines) {
            val header = TYPE_HEADER.matchEntire(line)
            if (header != null) {
                pending = header.groupValues[1] to header.groupValues[2]
                continue
            }
            val builders = BUILDERS_LINE.matchEntire(line)
            if (builders != null && pending != null) {
                types += SfdkProjectType(
                    id = pending.first,
                    description = pending.second,
                    builders = builders.groupValues[1].trim().split(WHITESPACE),
                    defaultBuilder = builders.groupValues[2],
                )
                pending = null
            }
        }
        return types
    }

    fun listTargets(): List<String> =
        run("tools", "list").stdoutLines
            .mapNotNull { TARGET_LINE.matchEntire(it)?.groupValues?.get(1) }

    fun listDevices(): List<SfdkDevice> {
        val lines = run("device", "list").stdoutLines
        val devices = mutableListOf<SfdkDevice>()
        var pendingName: String? = null
        for (line in lines) {
            val header = DEVICE_HEADER.matchEntire(line)
            if (header != null) {
                pendingName = header.groupValues[1]
                continue
            }
            val address = DEVICE_ADDRESS.find(line)
            if (address != null && pendingName != null) {
                devices += SfdkDevice(
                    name = pendingName,
                    user = address.groupValues[1],
                    host = address.groupValues[2],
                    port = address.groupValues[3].toInt(),
                )
                pendingName = null
            }
        }
        return devices
    }

    fun configuredTarget(): String? =
        options["target"] ?: run("config", "--show").stdoutLines.firstNotNullOfOrNull { CONFIG_TARGET.matchEntire(it)?.groupValues?.get(1) }

    fun initProject(directory: Path, type: String, builder: String, name: String) {
        run(directory, "init", "--force", "-t", type, "-b", builder, name)
    }

    fun ensureEngineForwardsVariables(variables: List<String>) {
        val current = run("engine", "show").stdoutLines
            .firstNotNullOfOrNull { ENGINE_FORWARD.matchEntire(it)?.groupValues?.get(1) }
            ?.split(WHITESPACE)
            ?.filter { it.isNotBlank() }
            ?: throw SfdkException("Could not read environment.forward from 'sfdk engine show'")
        val wanted = (current + variables).distinct()
        if (wanted != current) {
            run("engine", "set", "environment.forward=${wanted.joinToString(" ")}")
        }
    }

    fun commandLine(workingDirectory: Path?, vararg args: String): GeneralCommandLine {
        val commandLine = GeneralCommandLine(sfdk.toString()).withCharset(Charsets.UTF_8)
        for ((name, value) in options) {
            commandLine.addParameters("-c", "$name=$value")
        }
        commandLine.addParameters(*args)
        if (workingDirectory != null) {
            commandLine.withWorkingDirectory(workingDirectory)
        }
        return commandLine
    }

    private fun run(vararg args: String): ProcessOutput = run(null, *args)

    private fun run(workingDirectory: Path?, vararg args: String): ProcessOutput {
        val handler = try {
            CapturingProcessHandler(commandLine(workingDirectory, *args))
        } catch (e: ExecutionException) {
            throw SfdkException("Could not start $sfdk: ${e.message}")
        }
        val indicator = ProgressManager.getInstance().progressIndicator
        val output = if (indicator != null) handler.runProcessWithProgressIndicator(indicator, TIMEOUT_MS) else handler.runProcess(TIMEOUT_MS)
        if (output.isCancelled) {
            throw SfdkException("sfdk ${args.joinToString(" ")} was cancelled")
        }
        if (output.isTimeout) {
            throw SfdkException("sfdk ${args.joinToString(" ")} timed out")
        }
        if (output.exitCode != 0) {
            throw SfdkException("sfdk ${args.joinToString(" ")} failed (${output.exitCode}):\n${output.stderr}\n${output.stdout}")
        }
        return output
    }

    companion object {
        private const val TIMEOUT_MS = 10 * 60 * 1000
        private val WHITESPACE = Regex("\\s+")
        private val TYPE_HEADER = Regex("""^(\S+) \.\.\. (.+)$""")
        private val BUILDERS_LINE = Regex("""^\s+Builders: (.+?) \(Default: (\S+)\)$""")
        private val TARGET_LINE = Regex("""^[├└]── (\S+)\s.*$""")
        private val DEVICE_HEADER = Regex("""^#\d+ "(.+)"$""")
        private val DEVICE_ADDRESS = Regex("""(\S+)@([^:\s]+):(\d+)""")
        private val CONFIG_TARGET = Regex("""^target = (\S+)$""")
        private val ENGINE_FORWARD = Regex("""^environment\.forward:\s*(.*)$""")
    }
}
