package io.github.zap.build.gradle.convention

import org.gradle.api.Action
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler


fun DependencyHandler.paperApi(version: String = "1.16.5-R0.1-SNAPSHOT", dependencyConfiguration: (ExternalModuleDependency) -> Unit = {}) {
    val dependency = create("com.destroystokyo.paper:paper-api:$version") as ExternalModuleDependency
    dependency.exclude(mapOf("group" to "junit", "module" to "junit"))
    dependencyConfiguration.invoke(dependency)

    add("compileOnlyApi", dependency)
    add("testRuntimeOnly", dependency)
}


fun DependencyHandler.paperNms(version: String = "1.16.5-R0.1-SNAPSHOT", dependencyConfiguration: (ExternalModuleDependency) -> Unit = {}) {
    val dependency = create("com.destroystokyo.paper:paper:$version") as ExternalModuleDependency
    dependency.exclude(mapOf("group" to "io.papermc", "module" to "minecraft-server"))
    dependencyConfiguration.invoke(dependency)

    add("compileOnly", dependency)
}