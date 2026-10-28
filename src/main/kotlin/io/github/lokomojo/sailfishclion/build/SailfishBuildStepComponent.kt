package io.github.lokomojo.sailfishclion.build

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.cidr.cpp.execution.external.build.CLionExternalConfiguration
import com.jetbrains.cidr.cpp.execution.external.build.ui.CLionExternalBuildComponent
import java.awt.event.ActionListener
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent

class SailfishBuildStepComponent(initial: CLionExternalConfiguration) : CLionExternalBuildComponent {

    private val kindBox = ComboBox(DefaultComboBoxModel(SailfishStepKind.entries.toTypedArray()))

    override var configuration: CLionExternalConfiguration?
        get() = SailfishBuildStep(kindBox.selectedItem as SailfishStepKind)
        set(value) {
            kindBox.selectedItem = (value as? SailfishBuildStep)?.kind ?: SailfishStepKind.BUILD_DEBUG
        }

    init {
        configuration = initial
    }

    override fun createComponent(): JComponent = panel {
        row("sfdk step:") { cell(kindBox) }
    }

    override fun update() {
    }

    override fun validate() {
    }

    override fun addConfiguredListener(listener: ActionListener) {
        kindBox.addActionListener(listener)
    }
}
