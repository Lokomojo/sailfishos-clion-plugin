package io.github.lokomojo.sailfishclion.build

import com.intellij.openapi.project.Project
import com.jetbrains.cidr.cpp.execution.external.build.CLionExternalConfiguration
import com.jetbrains.cidr.cpp.execution.external.build.CLionExternalConfigurationControl
import com.jetbrains.cidr.cpp.execution.external.build.api.CLionExternalConfigurationProvider
import com.jetbrains.cidr.cpp.execution.external.build.runners.CLionExternalConfigurationRunner

class SailfishBuildStepProvider : CLionExternalConfigurationProvider {
    override val type: CLionExternalConfiguration.Type get() = SailfishStepType
    override val runner: CLionExternalConfigurationRunner get() = SailfishBuildStepRunner()
    override fun newControl(project: Project): CLionExternalConfigurationControl = SailfishBuildStepControl(project)
}
