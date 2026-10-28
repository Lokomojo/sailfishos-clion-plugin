package io.github.lokomojo.sailfishclion.build

import com.intellij.openapi.util.UserDataHolderBase
import com.jetbrains.cidr.cpp.execution.external.build.CLionExternalConfiguration

class SailfishBuildStep(val kind: SailfishStepKind) : UserDataHolderBase(), CLionExternalConfiguration {
    override val type: CLionExternalConfiguration.Type get() = SailfishStepType
    override val name: String get() = kind.displayName
    override val isEnabled: Boolean get() = true
    override fun copy(): CLionExternalConfiguration = SailfishBuildStep(kind)
}
