package io.github.zap.build.gradle.convention

import org.gradle.api.artifacts.ExternalModuleDependency

class QuickSwitcherOptions(
    var useLocal: Boolean = true,
    var version: String,
    var name: String) {

    private val configurations: MutableList<ExternalModuleDependency.() -> Unit> = mutableListOf()
    fun configure(configuration: ExternalModuleDependency.() -> Unit) {
        configurations.add(configuration)
    }
}