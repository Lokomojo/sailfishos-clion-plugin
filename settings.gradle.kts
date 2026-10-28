rootProject.name = "sailfishos-clion-plugin"

pluginManagement {
    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.4.21"
        id("org.jetbrains.intellij.platform") version "2.19.0"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
