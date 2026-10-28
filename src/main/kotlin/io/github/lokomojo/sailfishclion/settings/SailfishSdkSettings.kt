package io.github.lokomojo.sailfishclion.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.util.SystemInfo
import java.nio.file.Path
import java.nio.file.Paths

@State(name = "SailfishSdkSettings", storages = [Storage(value = "sailfishSdk.xml", roamingType = RoamingType.DISABLED)])
class SailfishSdkSettings : PersistentStateComponent<SailfishSdkSettings.State> {

    class State {
        var sfdkPath: String = ""
    }

    private var currentState = State()

    override fun getState(): State = currentState

    override fun loadState(state: State) {
        currentState = state
    }

    var sfdkPath: String
        get() = currentState.sfdkPath
        set(value) {
            currentState.sfdkPath = value
        }

    fun resolveSfdk(): Path? {
        if (sfdkPath.isBlank()) {
            return detectSfdk()
        }
        return SfdkLocation.existingExecutable(sfdkPath)?.toRealPath()
    }

    companion object {
        fun getInstance(): SailfishSdkSettings = ApplicationManager.getApplication().getService(SailfishSdkSettings::class.java)

        private val EXECUTABLE_NAME: String = if (SystemInfo.isWindows) "sfdk.exe" else "sfdk"

        fun detectSfdk(): Path? {
            val candidate = Paths.get(System.getProperty("user.home"), "SailfishOS", "bin", EXECUTABLE_NAME)
            return if (SfdkLocation.isExecutableFile(candidate)) candidate.toRealPath() else null
        }
    }
}
