package io.github.lokomojo.sailfishclion.settings

import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths

object SfdkLocation {

    fun parse(text: String): Path? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return null
        }
        return try {
            Paths.get(trimmed)
        } catch (e: InvalidPathException) {
            null
        }
    }

    fun isExecutableFile(path: Path): Boolean = Files.isRegularFile(path) && Files.isExecutable(path)

    fun existingExecutable(text: String): Path? = parse(text)?.takeIf { isExecutableFile(it) }
}
