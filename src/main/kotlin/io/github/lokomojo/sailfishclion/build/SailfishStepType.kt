package io.github.lokomojo.sailfishclion.build

import com.jetbrains.cidr.cpp.execution.external.build.api.AbstractExternalConfigurationType
import io.github.lokomojo.sailfishclion.SailfishIcons
import javax.swing.Icon

object SailfishStepType : AbstractExternalConfigurationType("SAILFISH_SFDK") {
    override val displayName: String get() = "Sailfish SDK (sfdk)"
    override val icon: Icon get() = SailfishIcons.Sailfish
}
