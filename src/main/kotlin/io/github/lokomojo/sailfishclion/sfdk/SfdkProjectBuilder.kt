package io.github.lokomojo.sailfishclion.sfdk

import com.google.gson.JsonParseException
import com.intellij.execution.ExecutionException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import io.github.lokomojo.sailfishclion.build.BuildOutput
import io.github.lokomojo.sailfishclion.build.SailfishStepKind
import io.github.lokomojo.sailfishclion.compiledb.CompileCommandsRewriter
import io.github.lokomojo.sailfishclion.compiledb.SysrootLocator
import io.github.lokomojo.sailfishclion.generation.SailfishProjectLayout
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class SfdkProjectBuilder(
    private val sfdkPath: Path,
    private val layout: SailfishProjectLayout,
    private val options: Map<String, String>,
) {

    private val cli = SfdkCli(sfdkPath, options)

    fun run(project: Project, kind: SailfishStepKind, cancelled: () -> Boolean): Boolean =
        report(project, "sfdk ${kind.displayName}", cancelled) { output ->
            when (kind) {
                SailfishStepKind.BUILD_DEBUG -> build(output, DEBUG_OPTFLAGS)
                SailfishStepKind.BUILD_RELEASE -> build(output, RELEASE_OPTFLAGS)
                SailfishStepKind.CLEAN -> output.run(cli.commandLine(layout.projectDir, "make", "clean"))
            }
        }

    fun deploy(project: Project, cancelled: () -> Boolean): Boolean =
        report(project, "sfdk deploy", cancelled) { output ->
            output.run(cli.commandLine(layout.projectDir, "make-install")) && output.run(cli.commandLine(layout.projectDir, "deploy", "--rsync"))
        }

    private fun report(project: Project, title: String, cancelled: () -> Boolean, body: (BuildOutput) -> Boolean): Boolean {
        val output = BuildOutput(project, title, layout.projectDir.toString(), cancelled)
        var success = false
        try {
            success = body(output)
        } catch (e: SfdkException) {
            output.message(e.message ?: "sfdk failed")
        } catch (e: ExecutionException) {
            output.message(e.message ?: "could not start sfdk")
        } catch (e: IOException) {
            output.message(e.message ?: "file operation failed")
        } catch (e: JsonParseException) {
            output.message("compile_commands.json is not valid JSON: ${e.message}")
        } finally {
            output.finish(success, if (success) "$title finished" else "$title failed")
        }
        return success
    }

    private fun build(output: BuildOutput, baseFlags: String): Boolean {
        val target = cli.configuredTarget() ?: throw SfdkException("sfdk has no configured target")
        val flags = "$baseFlags ${architectureFlags(target)}".trim()
        reconfigureIfFlagsChanged(flags)
        val environment = mapOf("CFLAGS" to flags, "CXXFLAGS" to flags, EXPORT_COMPILE_COMMANDS to "ON")
        if (!output.run(cli.commandLine(layout.projectDir, "build", "-d").withEnvironment(environment))) {
            return false
        }
        if (!Files.exists(layout.cmakeLists) && !output.run(cli.commandLine(layout.projectDir, "compiledb").withEnvironment(environment))) {
            return false
        }
        rewriteCompileCommands(output, target)
        return true
    }

    private fun rewriteCompileCommands(output: BuildOutput, target: String) {
        if (!Files.exists(layout.compileCommands)) {
            return
        }
        val sysroot = SysrootLocator(sfdkPath).locate(target)
        if (sysroot == null) {
            output.message("Sysroot for $target not found next to $sfdkPath; compilation database left as generated")
        } else if (CompileCommandsRewriter(sysroot).rewriteIfNeeded(layout.compileCommands)) {
            output.message("Rewrote ${layout.compileCommands.fileName} for host indexing")
        }
        LocalFileSystem.getInstance().refreshNioFiles(listOf(layout.compileCommands), true, false, null)
    }

    private fun reconfigureIfFlagsChanged(flags: String) {
        val cache = layout.projectDir.resolve("CMakeCache.txt")
        if (!Files.exists(cache)) {
            return
        }
        val cached = Files.readAllLines(cache).firstOrNull { it.startsWith(CACHED_FLAGS_KEY) }?.removePrefix(CACHED_FLAGS_KEY)
        if (cached != flags) {
            Files.delete(cache)
        }
    }

    private fun architectureFlags(target: String): String = when {
        target.contains("aarch64") -> "-march=armv8-a"
        target.contains("armv7hl") -> "-march=armv7-a -mfloat-abi=hard -mfpu=neon -mthumb"
        else -> ""
    }

    companion object {
        const val EXPORT_COMPILE_COMMANDS = "CMAKE_EXPORT_COMPILE_COMMANDS"
        val FORWARDED_VARIABLES: List<String> = listOf("CFLAGS", "CXXFLAGS", EXPORT_COMPILE_COMMANDS)
        private const val CACHED_FLAGS_KEY = "CMAKE_CXX_FLAGS:STRING="
        private const val DEBUG_OPTFLAGS = "-O0 -g -pipe -Wall -fexceptions"
        private const val RELEASE_OPTFLAGS = "-O2 -g -pipe -Wall -Wp,-D_FORTIFY_SOURCE=2 -fexceptions -fstack-protector --param=ssp-buffer-size=4 -Wformat -Wformat-security"
    }
}
