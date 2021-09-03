package io.github.zap.build.gradle.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.net.URI

class ShadowMCPluginPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(MCPluginPlugin::class.java)
        project.pluginManager.apply(ShadowLibraryPlugin::class.java)

        project.configureShadow(File(project.pluginDir))
    }
}