package io.github.lokomojo.sailfishclion.project

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
class SailfishBuildLock {

    private val running = AtomicBoolean(false)

    val isRunning: Boolean get() = running.get()

    fun <T> runExclusively(busy: () -> T, body: () -> T): T {
        if (!running.compareAndSet(false, true)) {
            return busy()
        }
        try {
            return body()
        } finally {
            running.set(false)
        }
    }

    companion object {
        fun getInstance(project: Project): SailfishBuildLock = project.getService(SailfishBuildLock::class.java)
    }
}
