package io.github.lokomojo.sailfishclion.compiledb

import com.intellij.openapi.util.text.StringUtil
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class SysrootLocator(private val sfdkExecutable: Path) {

    class Sysroot(val root: Path, val crossDir: Path, val gccTriple: String, val libstdcxxInclude: Path)

    fun locate(target: String): Sysroot? {
        val sdkRoot = sfdkExecutable.toAbsolutePath().parent?.parent ?: return null
        val root = sdkRoot.resolve("mersdk/targets/$target.default")
        val crossDir = root.resolve("opt/cross")
        val gccDir = crossDir.resolve("lib/gcc")
        if (!gccDir.isDirectory()) {
            return null
        }
        val triple = subdirectories(gccDir).singleOrNull() ?: return null
        val version = subdirectories(gccDir.resolve(triple)).maxWithOrNull { a, b -> StringUtil.compareVersionNumbers(a, b) } ?: return null
        val include = crossDir.resolve("$triple/include/c++/$version")
        if (!include.isDirectory()) {
            return null
        }
        return Sysroot(root, crossDir, triple, include)
    }

    private fun subdirectories(dir: Path): List<String> =
        Files.list(dir).use { stream -> stream.filter { it.isDirectory() }.map { it.name }.toList() }
}
