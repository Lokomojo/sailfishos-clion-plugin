package io.github.lokomojo.sailfishclion.project

import com.intellij.notification.NotificationAction
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import io.github.lokomojo.sailfishclion.SailfishNotifications
import io.github.lokomojo.sailfishclion.build.SailfishStepKind
import io.github.lokomojo.sailfishclion.generation.SailfishProjectLayout
import io.github.lokomojo.sailfishclion.settings.SailfishSdkConfigurable
import io.github.lokomojo.sailfishclion.settings.SailfishSdkSettings
import io.github.lokomojo.sailfishclion.sfdk.SfdkProjectBuilder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object SailfishProjectBuilds {

    fun projectDirectory(project: Project): Path? {
        val base = project.basePath?.let { Paths.get(it) } ?: return null
        return if (isSailfishProject(base)) base else null
    }

    fun run(project: Project, kind: SailfishStepKind, cancelled: () -> Boolean): Boolean =
        exclusively(project) { builderFor(project)?.run(project, kind, cancelled) ?: false }

    fun deploy(project: Project, cancelled: () -> Boolean): Boolean =
        exclusively(project) { builderFor(project)?.deploy(project, cancelled) ?: false }

    private fun exclusively(project: Project, body: () -> Boolean): Boolean =
        SailfishBuildLock.getInstance(project).runExclusively(
            busy = {
                SailfishNotifications.error(project, "Another sfdk build or deploy is still running in this project")
                false
            },
            body = body,
        )

    private fun isSailfishProject(directory: Path): Boolean {
        val rpmDir = directory.resolve("rpm")
        if (!Files.isDirectory(rpmDir)) {
            return false
        }
        Files.list(rpmDir).use { files -> return files.anyMatch { it.fileName.toString().endsWith(".spec") } }
    }

    private fun builderFor(project: Project): SfdkProjectBuilder? {
        val directory = projectDirectory(project) ?: return null
        val sfdk = SailfishSdkSettings.getInstance().resolveSfdk()
        if (sfdk == null) {
            notifyMissingSfdk(project)
            return null
        }
        return SfdkProjectBuilder(sfdk, SailfishProjectLayout(directory), SailfishProjectState.getInstance(project).sfdkOptions())
    }

    private fun notifyMissingSfdk(project: Project) {
        SailfishNotifications.error(
            project,
            "The sfdk executable was not found. Set its location in the Sailfish OS SDK settings.",
            NotificationAction.createSimpleExpiring("Open settings") {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, SailfishSdkConfigurable::class.java)
            },
        )
    }
}
