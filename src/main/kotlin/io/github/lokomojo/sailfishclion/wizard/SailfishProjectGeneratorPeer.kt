package io.github.lokomojo.sailfishclion.wizard

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.remote.PresentableId
import com.intellij.platform.GeneratorPeerImpl
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.ssh.config.unified.SshConfigManager
import com.intellij.ssh.ui.unified.SshConfigComboBox
import com.intellij.ssh.ui.unified.SshConfigVisibility
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import io.github.lokomojo.sailfishclion.sfdk.SfdkDevice
import io.github.lokomojo.sailfishclion.sfdk.SfdkException
import io.github.lokomojo.sailfishclion.sfdk.SfdkProjectBuilder
import io.github.lokomojo.sailfishclion.sfdk.SfdkProjectType
import io.github.lokomojo.sailfishclion.settings.SfdkLocation
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.nio.file.Path
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.event.DocumentEvent

class SailfishProjectGeneratorPeer(initialSfdkPath: Path?) : GeneratorPeerImpl<SailfishProjectSettings>(), Disposable {

    private val sfdkField = TextFieldWithBrowseButton()
    private val sshConfigBox = SshConfigComboBox(ProjectManager.getInstance().defaultProject, this, SshConfigVisibility.App)
    private val typeBox = ComboBox(DefaultComboBoxModel<SfdkProjectType>(), COMBO_WIDTH)
    private val builderBox = ComboBox(DefaultComboBoxModel<String>(), COMBO_WIDTH)
    private val targetBox = ComboBox(DefaultComboBoxModel<String>(), COMBO_WIDTH)
    private val deviceBox = ComboBox(DefaultComboBoxModel<SfdkDevice>(), COMBO_WIDTH)
    private val portField = JBTextField(DEFAULT_GDBSERVER_PORT.toString())

    private var loadedPath: Path? = null
    private var inventory: SfdkInventory = SfdkInventory.empty(MISSING_SFDK)
    private var checkValid: Runnable? = null

    init {
        sfdkField.text = initialSfdkPath?.toString() ?: ""
        sfdkField.addBrowseFolderListener(null, FileChooserDescriptorFactory.singleFile().withTitle("Select sfdk"))
        sfdkField.textField.addActionListener { reloadIfPathChanged() }
        sfdkField.textField.addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent) {
                reloadIfPathChanged()
            }
        })
        typeBox.addActionListener { refreshBuilders() }
        deviceBox.addActionListener { selectSshConfigForDevice() }
        reloadIfPathChanged()
    }

    override fun getComponent(myLocationField: TextFieldWithBrowseButton, checkValid: Runnable): JComponent {
        this.checkValid = checkValid
        sshConfigBox.comboBox.addActionListener { checkValid.run() }
        typeBox.addActionListener { checkValid.run() }
        builderBox.addActionListener { checkValid.run() }
        targetBox.addActionListener { checkValid.run() }
        deviceBox.addActionListener { checkValid.run() }
        portField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                checkValid.run()
            }
        })
        return panel {
            row("sfdk:") { cell(sfdkField).align(AlignX.FILL) }
            row("Project type:") { cell(typeBox).align(AlignX.FILL).resizableColumn() }
            row("Builder:") { cell(builderBox).align(AlignX.FILL) }
            row("Build target:") { cell(targetBox).align(AlignX.FILL) }
            row("Device:") { cell(deviceBox).align(AlignX.FILL) }
            row("SSH configuration:") { cell(sshConfigBox).align(AlignX.FILL) }
            row("gdbserver port:") { cell(portField) }
            row {
                comment("Creating the project sets the build engine to forward ${SfdkProjectBuilder.FORWARDED_VARIABLES.joinToString(", ")} (sfdk engine set environment.forward).", COMMENT_WIDTH)
            }
        }
    }

    override fun getSettings(): SailfishProjectSettings = SailfishProjectSettings(
        sfdkPath = SfdkLocation.existingExecutable(sfdkField.text) ?: error("sfdk path was not validated"),
        projectType = typeBox.selectedItem as SfdkProjectType,
        builder = builderBox.selectedItem as String,
        target = targetBox.selectedItem as String,
        device = deviceBox.selectedItem as SfdkDevice,
        sshConfigName = sshConfigBox.selectedSshConfig?.name ?: "",
        gdbServerPort = portField.text.trim().toInt(),
    )

    override fun validate(): ValidationInfo? {
        if (SfdkLocation.existingExecutable(sfdkField.text) == null) {
            return ValidationInfo("sfdk executable not found", sfdkField)
        }
        inventory.error?.let { return ValidationInfo(it, sfdkField) }
        if (typeBox.selectedItem == null) {
            return ValidationInfo("No project types reported by sfdk", typeBox)
        }
        if (targetBox.selectedItem == null) {
            return ValidationInfo("No build targets installed; run sfdk tools install first", targetBox)
        }
        if (deviceBox.selectedItem == null) {
            return ValidationInfo("No devices registered with the SDK", deviceBox)
        }
        if (sshConfigBox.selectedSshConfig == null) {
            return ValidationInfo("Select the SSH configuration that reaches the device", sshConfigBox)
        }
        if (portField.text.trim().toIntOrNull() !in 1..65535) {
            return ValidationInfo("Port must be between 1 and 65535", portField)
        }
        return null
    }

    private fun reloadIfPathChanged() {
        val path = SfdkLocation.parse(sfdkField.text)
        if (path != null && path == loadedPath) {
            return
        }
        inventory = when {
            path == null -> SfdkInventory.empty(MISSING_SFDK)
            !SfdkLocation.isExecutableFile(path) -> SfdkInventory.empty("No executable at $path")
            else -> queryInventory(path)
        }
        loadedPath = if (inventory.error == null) path else null
        applyInventory()
        checkValid?.run()
    }

    private fun queryInventory(path: Path): SfdkInventory =
        try {
            ProgressManager.getInstance().runProcessWithProgressSynchronously<SfdkInventory, SfdkException>(
                { SfdkInventory.load(path) },
                "Querying Sailfish SDK",
                true,
                null,
            )
        } catch (e: SfdkException) {
            SfdkInventory.empty(e.message)
        }

    private fun applyInventory() {
        typeBox.model = DefaultComboBoxModel(inventory.projectTypes.toTypedArray())
        targetBox.model = DefaultComboBoxModel(inventory.targets.toTypedArray())
        deviceBox.model = DefaultComboBoxModel(inventory.devices.toTypedArray())
        inventory.configuredTarget?.let { targetBox.selectedItem = it }
        refreshBuilders()
        selectSshConfigForDevice()
    }

    private fun refreshBuilders() {
        val type = typeBox.selectedItem as? SfdkProjectType
        if (type == null) {
            builderBox.model = DefaultComboBoxModel()
            return
        }
        builderBox.model = DefaultComboBoxModel(type.builders.toTypedArray())
        builderBox.selectedItem = if (PREFERRED_BUILDER in type.builders) PREFERRED_BUILDER else type.defaultBuilder
    }

    private fun selectSshConfigForDevice() {
        val device = deviceBox.selectedItem as? SfdkDevice
        val match = device?.let { chosen ->
            SshConfigManager.getInstance(ProjectManager.getInstance().defaultProject).configs
                .firstOrNull { matchesDevice(it, chosen) }
        }
        sshConfigBox.reload(PresentableId.createId(match?.id, null), SshConfigVisibility.App)
        checkValid?.run()
    }

    private fun matchesDevice(config: SshConfig, device: SfdkDevice): Boolean =
        config.host == device.host && config.port == device.port && config.username == device.user

    override fun dispose() {
    }

    companion object {
        private const val DEFAULT_GDBSERVER_PORT = 10000
        private const val PREFERRED_BUILDER = "cmake"
        private const val COMBO_WIDTH = 200
        private const val COMMENT_WIDTH = 60
        private const val MISSING_SFDK = "sfdk not found; enter its location above"
    }
}
