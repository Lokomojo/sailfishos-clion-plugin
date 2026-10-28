package io.github.lokomojo.sailfishclion.project

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@State(name = "SailfishProject", storages = [Storage("sailfish.xml")])
class SailfishProjectState : PersistentStateComponent<SailfishProjectState.State> {

    class State {
        var target: String = ""
        var device: String = ""
    }

    private var currentState = State()

    override fun getState(): State = currentState

    override fun loadState(state: State) {
        currentState = state
    }

    var target: String
        get() = currentState.target
        set(value) {
            currentState.target = value
        }

    var device: String
        get() = currentState.device
        set(value) {
            currentState.device = value
        }

    fun sfdkOptions(): Map<String, String> =
        buildMap {
            if (target.isNotEmpty()) put("target", target)
            if (device.isNotEmpty()) put("device", device)
        }

    companion object {
        fun getInstance(project: Project): SailfishProjectState = project.getService(SailfishProjectState::class.java)
    }
}
