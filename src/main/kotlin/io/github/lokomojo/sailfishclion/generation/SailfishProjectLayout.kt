package io.github.lokomojo.sailfishclion.generation

import java.nio.file.Path

class SailfishProjectLayout(val projectDir: Path) {
    val packageName: String = projectDir.fileName.toString()
    val remoteExecutable: String = "/opt/sdk/$packageName/usr/bin/$packageName"
    val localExecutable: Path = projectDir.resolve(packageName)
    val cmakeLists: Path = projectDir.resolve("CMakeLists.txt")
    val compileCommands: Path = projectDir.resolve("compile_commands.json")
}
