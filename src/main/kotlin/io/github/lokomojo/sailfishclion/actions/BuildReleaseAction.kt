package io.github.lokomojo.sailfishclion.actions

import com.intellij.openapi.project.Project
import io.github.lokomojo.sailfishclion.build.SailfishStepKind
import io.github.lokomojo.sailfishclion.project.SailfishProjectBuilds

class BuildReleaseAction : SailfishBuildAction("Building Sailfish OS project (release)") {
    override fun execute(project: Project, cancelled: () -> Boolean) {
        SailfishProjectBuilds.run(project, SailfishStepKind.BUILD_RELEASE, cancelled)
    }
}
