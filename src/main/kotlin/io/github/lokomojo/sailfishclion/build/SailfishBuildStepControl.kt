package io.github.lokomojo.sailfishclion.build

import com.intellij.openapi.project.Project
import com.jetbrains.cidr.cpp.execution.external.build.CLionExternalConfiguration
import com.jetbrains.cidr.cpp.execution.external.build.CLionExternalConfigurationControl
import com.jetbrains.cidr.cpp.execution.external.build.ui.CLionExternalBuildComponent
import com.jetbrains.cidr.cpp.execution.external.build.ui.CLionExternalTargetConfigurable
import org.jdom.Element

class SailfishBuildStepControl(project: Project) : CLionExternalConfigurationControl(project) {

    override fun serialize(configuration: CLionExternalConfiguration): Element {
        val kind = (configuration as? SailfishBuildStep)?.kind ?: SailfishStepKind.BUILD_DEBUG
        return Element(ELEMENT).setAttribute(KIND_ATTRIBUTE, kind.name)
    }

    override fun deserialize(element: Element): CLionExternalConfiguration {
        val kind = element.getAttributeValue(KIND_ATTRIBUTE)?.let { name -> SailfishStepKind.entries.firstOrNull { it.name == name } }
        return SailfishBuildStep(kind ?: SailfishStepKind.BUILD_DEBUG)
    }

    override fun getComponent(
        initial: CLionExternalConfiguration?,
        parentConfigurable: CLionExternalTargetConfigurable,
        isBuildTarget: Boolean,
    ): CLionExternalBuildComponent = SailfishBuildStepComponent(initial ?: SailfishBuildStep(SailfishStepKind.BUILD_DEBUG))

    companion object {
        private const val ELEMENT = "sfdk"
        private const val KIND_ATTRIBUTE = "kind"
    }
}
