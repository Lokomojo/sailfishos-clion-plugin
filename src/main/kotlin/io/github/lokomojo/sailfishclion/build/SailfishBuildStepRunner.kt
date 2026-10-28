package io.github.lokomojo.sailfishclion.build

import com.intellij.openapi.project.Project
import com.intellij.task.ProjectTaskContext
import com.intellij.task.ProjectTaskRunner
import com.jetbrains.cidr.cpp.execution.external.build.CLionExternalConfiguration
import com.jetbrains.cidr.cpp.execution.external.build.runners.CLionAbstractExternalConfigurationRunner
import com.jetbrains.cidr.execution.build.CidrBuildEvent
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise
import io.github.lokomojo.sailfishclion.project.SailfishProjectBuilds

class SailfishBuildStepRunner : CLionAbstractExternalConfigurationRunner() {

    override val type: CLionExternalConfiguration.Type get() = SailfishStepType

    override suspend fun run(
        project: Project,
        configuration: CLionExternalConfiguration,
        sessionId: Any,
        buildEvent: CidrBuildEvent,
        context: ProjectTaskContext,
    ): Promise<ProjectTaskRunner.Result> {
        val step = configuration as? SailfishBuildStep ?: return resolvedPromise(SailfishTaskResult(failed = true))
        val job = coroutineContext[Job]
        val success = withContext(Dispatchers.IO) { SailfishProjectBuilds.run(project, step.kind) { job?.isActive == false } }
        return resolvedPromise(SailfishTaskResult(failed = !success))
    }
}
