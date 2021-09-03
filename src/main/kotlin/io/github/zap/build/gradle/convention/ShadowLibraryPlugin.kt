package io.github.zap.build.gradle.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenArtifactSet
import org.gradle.api.publish.maven.MavenPublication
import java.io.File

class ShadowLibraryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(LibraryPlugin::class.java)
        project.pluginManager.apply("com.github.johnrengelman.shadow")

        project.configurations.named("api").get().extendsFrom(project.shade)
        project.configurations.named("implementation").get().extendsFrom(project.relocate)

        project.configureShadow(File("${project.buildDir}/libs"))

        project.tasks.named("jar").get().enabled = false
    }
}