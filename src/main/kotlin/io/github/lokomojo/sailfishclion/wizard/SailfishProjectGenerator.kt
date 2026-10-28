package io.github.lokomojo.sailfishclion.wizard

import com.intellij.facet.ui.ValidationResult
import com.intellij.ide.util.projectWizard.AbstractNewProjectStep
import com.intellij.ide.util.projectWizard.CustomStepProjectGenerator
import com.intellij.ide.util.projectWizard.ProjectSettingsStepBase
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.welcomeScreen.AbstractActionWithPanel
import com.intellij.platform.DirectoryProjectGenerator
import com.intellij.platform.DirectoryProjectGeneratorBase
import com.intellij.platform.ProjectGeneratorPeer
import io.github.lokomojo.sailfishclion.SailfishIcons
import io.github.lokomojo.sailfishclion.SailfishNotifications
import io.github.lokomojo.sailfishclion.build.SailfishStepKind
import io.github.lokomojo.sailfishclion.generation.ProjectScaffolder
import io.github.lokomojo.sailfishclion.generation.SailfishProjectLayout
import io.github.lokomojo.sailfishclion.project.SailfishProjectBuilds
import io.github.lokomojo.sailfishclion.project.SailfishProjectConfigurator
import io.github.lokomojo.sailfishclion.settings.SailfishSdkSettings
import io.github.lokomojo.sailfishclion.settings.SfdkLocation
import io.github.lokomojo.sailfishclion.sfdk.SfdkException
import java.nio.file.Files
import javax.swing.Icon

class SailfishProjectGenerator :
    DirectoryProjectGeneratorBase<SailfishProjectSettings>(),
    CustomStepProjectGenerator<SailfishProjectSettings> {

    private var step: ProjectSettingsStepBase<SailfishProjectSettings>? = null

    override fun createStep(
        projectGenerator: DirectoryProjectGenerator<SailfishProjectSettings>,
        callback: AbstractNewProjectStep.AbstractCallback<SailfishProjectSettings>,
    ): AbstractActionWithPanel {
        val created = object : ProjectSettingsStepBase<SailfishProjectSettings>(projectGenerator, callback) {
            override fun dispose() {
                super.dispose()
                if (step === this) {
                    step = null
                }
            }
        }
        step = created
        return created
    }

    override fun getName(): String = "Sailfish OS Application"

    override fun getDescription(): String = "A Sailfish OS application created with sfdk init, with build, deploy and on-device debugging configured."

    override fun getLogo(): Icon = SailfishIcons.Sailfish

    override fun createPeer(): ProjectGeneratorPeer<SailfishProjectSettings> {
        val peer = SailfishProjectGeneratorPeer(SailfishSdkSettings.getInstance().resolveSfdk())
        val owner = step
        if (owner == null || !Disposer.tryRegister(owner, peer)) {
            Disposer.register(ApplicationManager.getApplication(), peer)
        }
        return peer
    }

    override fun validate(baseDirPath: String): ValidationResult {
        val name = SfdkLocation.parse(baseDirPath)?.fileName?.toString() ?: ""
        return if (PACKAGE_NAME.matches(name)) ValidationResult.OK
        else ValidationResult("The directory name becomes the RPM package name; use letters, digits and . _ + - only, starting with a letter or digit")
    }

    override fun generateProject(project: Project, baseDir: VirtualFile, settings: SailfishProjectSettings, module: Module) {
        rememberSfdkPath(settings)
        val projectDir = baseDir.toNioPath()
        ProgressManager.getInstance().run(object : Task.Modal(project, "Creating Sailfish OS Project", true) {
            private var layout: SailfishProjectLayout? = null

            override fun run(indicator: ProgressIndicator) {
                layout = ProjectScaffolder(settings.sfdkPath).scaffold(projectDir, settings, indicator)
            }

            override fun onThrowable(error: Throwable) {
                if (error is SfdkException) SailfishNotifications.error(project, error.message ?: "Project creation failed")
                else LOG.error("Sailfish OS project creation failed", error)
            }

            override fun onSuccess() {
                val generated = layout ?: return
                LocalFileSystem.getInstance().refreshNioFiles(listOf(projectDir), true, true, null)
                val configurator = SailfishProjectConfigurator(project, generated)
                configurator.configure(settings)
                runFirstBuild(project, generated, configurator)
            }
        })
    }

    private fun rememberSfdkPath(settings: SailfishProjectSettings) {
        val detected = SailfishSdkSettings.detectSfdk()
        SailfishSdkSettings.getInstance().sfdkPath = if (settings.sfdkPath == detected) "" else settings.sfdkPath.toString()
    }

    private fun runFirstBuild(project: Project, layout: SailfishProjectLayout, configurator: SailfishProjectConfigurator) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Building Sailfish OS project", true) {
            private var built = false

            override fun run(indicator: ProgressIndicator) {
                built = SailfishProjectBuilds.run(project, SailfishStepKind.BUILD_DEBUG) { indicator.isCanceled }
            }

            override fun onFinished() {
                if (built && Files.exists(layout.compileCommands)) {
                    configurator.linkCompilationDatabase()
                } else {
                    SailfishNotifications.error(project, "The first build did not produce a compilation database; build the project and then attach ${layout.compileCommands.fileName} via Tools | Compilation Database")
                }
            }
        })
    }

    companion object {
        private val LOG = logger<SailfishProjectGenerator>()
        private val PACKAGE_NAME = Regex("""^[A-Za-z0-9][A-Za-z0-9._+-]*$""")
    }
}
