package io.github.lokomojo.sailfishclion.generation

import com.intellij.openapi.progress.ProgressIndicator
import io.github.lokomojo.sailfishclion.sfdk.SfdkCli
import io.github.lokomojo.sailfishclion.sfdk.SfdkProjectBuilder
import io.github.lokomojo.sailfishclion.wizard.SailfishProjectSettings
import java.nio.file.Path

class ProjectScaffolder(sfdkPath: Path) {

    private val sfdk = SfdkCli(sfdkPath)

    fun scaffold(projectDir: Path, settings: SailfishProjectSettings, indicator: ProgressIndicator): SailfishProjectLayout {
        val layout = SailfishProjectLayout(projectDir)
        indicator.text = "Running sfdk init"
        sfdk.initProject(projectDir, settings.projectType.id, settings.builder, layout.packageName)
        indicator.text = "Configuring the build engine"
        sfdk.ensureEngineForwardsVariables(SfdkProjectBuilder.FORWARDED_VARIABLES)
        return layout
    }
}
