package io.github.lokomojo.sailfishclion.wizard

import io.github.lokomojo.sailfishclion.sfdk.SfdkDevice
import io.github.lokomojo.sailfishclion.sfdk.SfdkProjectType
import java.nio.file.Path

class SailfishProjectSettings(
    val sfdkPath: Path,
    val projectType: SfdkProjectType,
    val builder: String,
    val target: String,
    val device: SfdkDevice,
    val sshConfigName: String,
    val gdbServerPort: Int,
)
