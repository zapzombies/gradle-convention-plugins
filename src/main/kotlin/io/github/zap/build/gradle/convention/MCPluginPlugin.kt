package io.github.zap.build.gradle.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.compile.JavaCompile
import java.io.File
import java.io.FilenameFilter
import java.net.URI

class MCPluginPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(LibraryPlugin::class.java)

        project.setOutputDir((project.properties["outputDir"] ?: "${project.projectDir}/run/server-1") as String)
        project.setPluginDir("${project.outputDir}/plugins")

        project.configurations.named("compileOnlyApi").get().extendsFrom(project.bukkitPlugin)

        val dp = project.tasks.register("deleteOldPlugins", CleanOldPluginsTask::class.java)
        val cp = project.tasks.register("copyPlugins", Copy::class.java) {
            it.dependsOn(dp)
            it.from(project.bukkitPlugin)
            it.into(project.pluginDir)
        }

        val csa = project.tasks.register("copyServerArtifact", Copy::class.java) {
            it.from(project.serverArtifact)
            it.into(project.outputDir)
        }

        val csavl = project.tasks.register("copyVerionlessServerArtifact") {
            it.doLast {
                project.copy { cp ->
                    cp.from(project.serverArtifactVerless).into(project.outputDir)

                    project.serverArtifactVerless.allDependencies.forEach { deps ->
                        cp.rename("-${deps.version}", "")
                    }
                }
            }
        }

        project.tasks.named("compileJava", JavaCompile::class.java) {
            it.dependsOn(cp.get(), csa.get(), csavl.get())
        }
    }
}