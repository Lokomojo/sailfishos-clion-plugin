package io.github.lokomojo.sailfishclion.wizard

import io.github.lokomojo.sailfishclion.sfdk.SfdkCli
import io.github.lokomojo.sailfishclion.sfdk.SfdkDevice
import io.github.lokomojo.sailfishclion.sfdk.SfdkProjectType
import java.nio.file.Path

class SfdkInventory(
    val projectTypes: List<SfdkProjectType>,
    val targets: List<String>,
    val devices: List<SfdkDevice>,
    val configuredTarget: String?,
    val error: String?,
) {
    companion object {
        fun empty(error: String?): SfdkInventory =
            SfdkInventory(emptyList(), emptyList(), emptyList(), null, error)

        fun load(sfdkPath: Path): SfdkInventory {
            val cli = SfdkCli(sfdkPath)
            return SfdkInventory(
                projectTypes = cli.listProjectTypes(),
                targets = cli.listTargets(),
                devices = cli.listDevices(),
                configuredTarget = cli.configuredTarget(),
                error = null,
            )
        }
    }
}
