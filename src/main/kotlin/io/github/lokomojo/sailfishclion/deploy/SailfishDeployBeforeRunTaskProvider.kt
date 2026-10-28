package io.github.lokomojo.sailfishclion.deploy

import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Key
import io.github.lokomojo.sailfishclion.SailfishIcons
import io.github.lokomojo.sailfishclion.project.SailfishProjectBuilds
import javax.swing.Icon

class SailfishDeployBeforeRunTaskProvider : BeforeRunTaskProvider<SailfishDeployBeforeRunTask>() {

    override fun getId(): Key<SailfishDeployBeforeRunTask> = ID

    override fun getName(): String = "Deploy to Sailfish OS device"

    override fun getIcon(): Icon = SailfishIcons.Sailfish

    override fun createTask(runConfiguration: RunConfiguration): SailfishDeployBeforeRunTask? =
        if (SailfishProjectBuilds.projectDirectory(runConfiguration.project) != null) SailfishDeployBeforeRunTask() else null

    override fun executeTask(
        context: DataContext,
        configuration: RunConfiguration,
        environment: ExecutionEnvironment,
        task: SailfishDeployBeforeRunTask,
    ): Boolean {
        val indicator = ProgressManager.getInstance().progressIndicator
        return SailfishProjectBuilds.deploy(configuration.project) { indicator?.isCanceled == true }
    }

    companion object {
        val ID: Key<SailfishDeployBeforeRunTask> = Key.create("SailfishDeployBeforeRunTask")
    }
}
