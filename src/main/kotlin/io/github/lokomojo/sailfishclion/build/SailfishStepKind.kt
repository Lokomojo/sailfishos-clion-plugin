package io.github.lokomojo.sailfishclion.build

enum class SailfishStepKind(val displayName: String) {
    BUILD_DEBUG("Build (debug)"),
    BUILD_RELEASE("Build (release)"),
    CLEAN("Clean");

    override fun toString(): String = displayName
}
