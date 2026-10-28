package io.github.lokomojo.sailfishclion.actions

import com.intellij.openapi.project.Project
import io.github.lokomojo.sailfishclion.project.SailfishProjectBuilds

class DeployAction : SailfishBuildAction("Deploying to Sailfish OS device") {
    override fun execute(project: Project, cancelled: () -> Boolean) {
        SailfishProjectBuilds.deploy(project, cancelled)
    }
}
