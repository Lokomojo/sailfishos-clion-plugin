package io.github.lokomojo.sailfishclion.project

import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.cidr.cpp.compdb.ID
import com.jetbrains.cidr.cpp.compdb.settings.CompDBProjectSettings
import com.jetbrains.cidr.cpp.execution.external.build.CLionExternalBuildConfiguration
import com.jetbrains.cidr.cpp.execution.external.build.CLionExternalBuildManager
import com.jetbrains.cidr.cpp.execution.external.build.CLionExternalBuildTarget
import com.jetbrains.cidr.cpp.execution.gdbserver.DownloadType
import com.jetbrains.cidr.cpp.execution.gdbserver.remote.RemoteGdbServerConfigurationType
import com.jetbrains.cidr.cpp.execution.gdbserver.remote.RemoteGdbServerRunConfiguration
import com.jetbrains.cidr.execution.BuildTargetAndConfigurationData
import com.jetbrains.cidr.execution.ExecutableData
import io.github.lokomojo.sailfishclion.build.SailfishBuildStep
import io.github.lokomojo.sailfishclion.build.SailfishStepKind
import io.github.lokomojo.sailfishclion.build.SailfishStepType
import io.github.lokomojo.sailfishclion.deploy.SailfishDeployBeforeRunTaskProvider
import io.github.lokomojo.sailfishclion.generation.SailfishProjectLayout
import io.github.lokomojo.sailfishclion.wizard.SailfishProjectSettings
import java.util.UUID

class SailfishProjectConfigurator(private val project: Project, private val layout: SailfishProjectLayout) {

    fun configure(settings: SailfishProjectSettings) {
        val state = SailfishProjectState.getInstance(project)
        state.target = settings.target
        state.device = settings.device.name
        val target = registerBuildTarget()
        createDebugConfiguration(target, settings)
    }

    fun linkCompilationDatabase() {
        val settings = CompDBProjectSettings.default()
        settings.externalProjectPath = layout.projectDir.toString()
        settings.setModules(setOf(layout.projectDir.toString()))
        ExternalSystemUtil.linkExternalProject(settings, ImportSpecBuilder(project, ID).use(ProgressExecutionMode.IN_BACKGROUND_ASYNC))
    }

    private fun registerBuildTarget(): CLionExternalBuildTarget {
        val manager = CLionExternalBuildManager.getInstance(project)
        manager.targets.firstOrNull { it.name == TARGET_NAME }?.let { return it }
        val debug = CLionExternalBuildConfiguration(
            "Debug", SailfishBuildStep(SailfishStepKind.BUILD_DEBUG), SailfishBuildStep(SailfishStepKind.CLEAN), null, UUID.randomUUID(),
        )
        val release = CLionExternalBuildConfiguration(
            "Release", SailfishBuildStep(SailfishStepKind.BUILD_RELEASE), SailfishBuildStep(SailfishStepKind.CLEAN), null, UUID.randomUUID(),
        )
        val target = CLionExternalBuildTarget(TARGET_NAME, layout.packageName, listOf(debug, release), SailfishStepType, UUID.randomUUID())
        manager.targets = manager.targets + target
        return target
    }

    private fun createDebugConfiguration(target: CLionExternalBuildTarget, settings: SailfishProjectSettings) {
        val runManager = RunManager.getInstance(project)
        if (runManager.allSettings.any { it.name == RUN_CONFIGURATION_NAME }) {
            return
        }
        val type = ConfigurationTypeUtil.findConfigurationType(RemoteGdbServerConfigurationType.TYPE_ID) ?: return
        val runSettings = runManager.createConfiguration(RUN_CONFIGURATION_NAME, type.configurationFactories.first())
        val configuration = runSettings.configuration as RemoteGdbServerRunConfiguration
        val gdbServerArgs = ParametersListUtil.join(listOf(":${settings.gdbServerPort}", layout.remoteExecutable))
        configuration.targetAndConfigurationData = BuildTargetAndConfigurationData(target, target.buildConfigurations.first())
        configuration.executableData = ExecutableData(layout.localExecutable.toString())
        configuration.programParameters = gdbServerArgs
        configuration.gdbRemoteString = "${settings.device.host}:${settings.gdbServerPort}"
        configuration.serverExecutable = REMOTE_GDBSERVER
        configuration.downloadType = DownloadType.NONE
        configuration.uploadFile = layout.remoteExecutable
        configuration.defaultGdbServerArgs = gdbServerArgs
        configuration.sshConfigName = settings.sshConfigName
        SailfishDeployBeforeRunTaskProvider().createTask(configuration)?.let { deploy ->
            deploy.isEnabled = true
            configuration.beforeRunTasks = configuration.beforeRunTasks + deploy
        }
        runSettings.storeInDotIdeaFolder()
        runManager.addConfiguration(runSettings)
        runManager.selectedConfiguration = runSettings
    }

    companion object {
        private const val TARGET_NAME = "sfdk"
        private const val RUN_CONFIGURATION_NAME = "Debug on device"
        private const val REMOTE_GDBSERVER = "/usr/bin/gdbserver"
    }
}
