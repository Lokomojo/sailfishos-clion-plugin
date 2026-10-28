package io.github.lokomojo.sailfishclion.actions

import com.intellij.openapi.project.Project
import io.github.lokomojo.sailfishclion.build.SailfishStepKind
import io.github.lokomojo.sailfishclion.project.SailfishProjectBuilds

class CleanAction : SailfishBuildAction("Cleaning Sailfish OS project") {
    override fun execute(project: Project, cancelled: () -> Boolean) {
        SailfishProjectBuilds.run(project, SailfishStepKind.CLEAN, cancelled)
    }
}
