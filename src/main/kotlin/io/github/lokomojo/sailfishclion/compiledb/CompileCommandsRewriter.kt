package io.github.lokomojo.sailfishclion.compiledb

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.util.execution.ParametersListUtil
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class CompileCommandsRewriter(private val sysroot: SysrootLocator.Sysroot) {

    fun rewriteIfNeeded(file: Path): Boolean {
        val root = Files.newBufferedReader(file).use { JsonParser.parseReader(it) }
        if (!root.isJsonArray) {
            return false
        }
        val entries = root.asJsonArray.filter { it.isJsonObject }.map { it.asJsonObject }
        if (entries.none { needsRewrite(it) }) {
            return false
        }
        for (entry in entries) {
            rewriteCommand(entry)
            rewriteArguments(entry)
        }
        val temporary = file.resolveSibling("${file.fileName}.tmp")
        Files.writeString(temporary, GSON.toJson(root))
        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        return true
    }

    private fun needsRewrite(entry: JsonObject): Boolean =
        isEngineCompiler(commandTokens(entry).firstOrNull()) || isEngineCompiler(argumentTokens(entry).firstOrNull())

    private fun rewriteCommand(entry: JsonObject) {
        val tokens = commandTokens(entry)
        if (tokens.isEmpty()) {
            return
        }
        entry.addProperty("command", ParametersListUtil.join(rewriteTokens(tokens)))
    }

    private fun rewriteArguments(entry: JsonObject) {
        val tokens = argumentTokens(entry)
        if (tokens.isEmpty()) {
            return
        }
        val rewritten = JsonArray()
        rewriteTokens(tokens).forEach { rewritten.add(it) }
        entry.add("arguments", rewritten)
    }

    private fun rewriteTokens(tokens: List<String>): List<String> {
        val result = mutableListOf<String>()
        var previousWasIsystem = false
        for ((index, token) in tokens.withIndex()) {
            when {
                index == 0 && isEngineCompiler(token) -> result += hostCompilerArguments(isCxx = CXX_COMPILER.matches(token))
                index == 0 -> result += token
                token.startsWith("-I/usr/") -> result += "-I${sysroot.root}${token.removePrefix("-I")}"
                previousWasIsystem && token.startsWith("/usr/") -> result += "${sysroot.root}$token"
                else -> result += token
            }
            previousWasIsystem = token == "-isystem"
        }
        return result
    }

    private fun hostCompilerArguments(isCxx: Boolean): List<String> {
        val common = listOf(
            if (isCxx) HOST_CXX_COMPILER else HOST_C_COMPILER,
            "--target=${sysroot.gccTriple}",
            "--sysroot=${sysroot.root}",
            "--gcc-toolchain=${sysroot.crossDir}",
        )
        if (!isCxx) {
            return common
        }
        return common + listOf(
            "-nostdinc++",
            "-isystem", sysroot.libstdcxxInclude.toString(),
            "-isystem", sysroot.libstdcxxInclude.resolve(sysroot.gccTriple).toString(),
        )
    }

    private fun commandTokens(entry: JsonObject): List<String> =
        stringOrNull(entry.get("command"))?.let { ParametersListUtil.parse(it) } ?: emptyList()

    private fun argumentTokens(entry: JsonObject): List<String> =
        entry.get("arguments")?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { stringOrNull(it) } ?: emptyList()

    private fun stringOrNull(element: JsonElement?): String? =
        element?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

    private fun isEngineCompiler(token: String?): Boolean = token != null && ENGINE_COMPILER.matches(token)

    companion object {
        private const val HOST_C_COMPILER = "clang"
        private const val HOST_CXX_COMPILER = "clang++"
        private val ENGINE_COMPILER = Regex("""^(?:/usr/bin/)?(?:[\w.-]+-)?(?:c\+\+|g\+\+|cc|gcc)$""")
        private val CXX_COMPILER = Regex("""^(?:/usr/bin/)?(?:[\w.-]+-)?(?:c\+\+|g\+\+)$""")
        private val GSON = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    }
}
