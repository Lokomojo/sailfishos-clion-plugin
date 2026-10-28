package io.github.lokomojo.sailfishclion.actions

import com.intellij.openapi.project.Project
import io.github.lokomojo.sailfishclion.build.SailfishStepKind
import io.github.lokomojo.sailfishclion.project.SailfishProjectBuilds

class BuildDebugAction : SailfishBuildAction("Building Sailfish OS project (debug)") {
    override fun execute(project: Project, cancelled: () -> Boolean) {
        SailfishProjectBuilds.run(project, SailfishStepKind.BUILD_DEBUG, cancelled)
    }
}
