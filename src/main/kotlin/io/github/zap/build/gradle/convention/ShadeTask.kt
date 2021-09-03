package io.github.zap.build.gradle.convention

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

abstract class ShadeTask : DefaultTask() {
    @get:Input
    abstract val configurations: SetProperty<Configuration>

    @get:Input
    abstract val shadowTask: Property<TaskProvider<ShadowJar>>

    init {
        configurations.convention(listOf())
    }

    @TaskAction
    fun configureShadow() {
        if(shadowTask.isPresent && shadowTask.get().isPresent) {
            shadowTask.get().configure {
                it.from(configurations.get().flatMap { x ->
                    x.map { f ->
                        if(f.isDirectory) f else if (f.isFile) project.zipTree(f) else null
                    }
                }.filterNotNull())
            }
        }
    }

}