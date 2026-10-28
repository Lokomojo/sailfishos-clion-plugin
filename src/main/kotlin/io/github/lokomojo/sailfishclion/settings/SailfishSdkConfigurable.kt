package io.github.lokomojo.sailfishclion.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel

class SailfishSdkConfigurable : BoundConfigurable("Sailfish OS SDK") {

    override fun createPanel(): DialogPanel {
        val settings = SailfishSdkSettings.getInstance()
        val detected = SailfishSdkSettings.detectSfdk()?.toString() ?: "not found in ~/SailfishOS/bin"
        return panel {
            row("sfdk executable:") {
                textFieldWithBrowseButton(FileChooserDescriptorFactory.singleFile().withTitle("Select sfdk"))
                    .bindText(settings::sfdkPath)
                    .align(AlignX.FILL)
                    .comment("Leave empty to auto-detect. Detected: $detected")
            }
        }
    }
}
