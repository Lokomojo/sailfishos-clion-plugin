package io.github.lokomojo.sailfishclion.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import io.github.lokomojo.sailfishclion.project.SailfishBuildLock
import io.github.lokomojo.sailfishclion.project.SailfishProjectBuilds

abstract class SailfishBuildAction(private val title: String) : DumbAwareAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val sailfishProject = project != null && SailfishProjectBuilds.projectDirectory(project) != null
        e.presentation.isVisible = sailfishProject
        e.presentation.isEnabled = sailfishProject && !SailfishBuildLock.getInstance(project).isRunning
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, title, true) {
            override fun run(indicator: ProgressIndicator) {
                execute(project) { indicator.isCanceled }
            }
        })
    }

    protected abstract fun execute(project: Project, cancelled: () -> Boolean)
}
