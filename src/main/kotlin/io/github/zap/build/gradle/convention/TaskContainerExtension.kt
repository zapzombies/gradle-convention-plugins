@file:Suppress("UNCHECKED_CAST")

package io.github.zap.build.gradle.convention

import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider

internal fun <S> TaskContainer.getOrRegister(name: String, type: Class<S>, configure: (S) -> Unit = {}) : TaskProvider<S> where S : Task {
    val task = if(find {it.name == name} != null) {
        named(name, type)
    } else {
        register(name, type)
    }
    task.configure(configure)
    return task
}

val TaskContainer.relocate: TaskProvider<ConfigureShadowRelocation>
    get() = getOrRegister("relocate", ConfigureShadowRelocation::class.java)

val TaskContainer.copyPlugins: TaskProvider<Copy>
    get() = getOrRegister("copyPlugins", Copy::class.java)

val TaskContainer.copyServerArtifact: TaskProvider<Copy>
    get() = getOrRegister("copyServerArtifact", Copy::class.java)

val TaskContainer.copyVerionlessServerArtifact: TaskProvider<Copy>
    get() = getOrRegister("copyVerionlessServerArtifact", Copy::class.java)


