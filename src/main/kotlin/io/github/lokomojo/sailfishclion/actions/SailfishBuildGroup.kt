package io.github.lokomojo.sailfishclion.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import io.github.lokomojo.sailfishclion.project.SailfishProjectBuilds

class SailfishBuildGroup : DefaultActionGroup() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null && SailfishProjectBuilds.projectDirectory(project) != null
    }
}
