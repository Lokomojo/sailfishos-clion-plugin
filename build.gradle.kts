import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
}

group = "io.github.lokomojo"
version = "0.1.0"

kotlin {
    jvmToolchain(25)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        clion("2026.2.4")
        bundledPlugin("com.intellij.clion")
        bundledPlugin("com.intellij.clion-compdb")
        bundledPlugin("com.intellij.nativeDebug")
        bundledPlugin("com.jetbrains.plugins.webDeployment")
        bundledModule("intellij.clion.execution")
        bundledModule("intellij.cidr.execution")
        bundledModule("intellij.cidr.projectModel")
        bundledModule("intellij.cidr.runner")
    }
}

intellijPlatform {
    buildSearchableOptions = false

    pluginConfiguration {
        id = "io.github.lokomojo.sailfishos-clion-plugin"
        name = "Sailfish OS SDK Integration"
        version = project.version.toString()
        ideaVersion {
            sinceBuild = "262"
            untilBuild = provider { null }
        }
    }

    pluginVerification {
        ides {
            recommended()
            select {
                types = listOf(IntelliJPlatformType.CLion)
                channels = listOf(ProductRelease.Channel.RELEASE, ProductRelease.Channel.EAP, ProductRelease.Channel.BETA, ProductRelease.Channel.RC)
                sinceBuild = "262"
                untilBuild = "263.*"
            }
        }
    }
}
