package io.github.lokomojo.sailfishclion.build

import com.intellij.task.ProjectTaskRunner

class SailfishTaskResult(private val failed: Boolean) : ProjectTaskRunner.Result {
    override fun isAborted(): Boolean = false
    override fun hasErrors(): Boolean = failed
}
