package io.github.zap.build.gradle.convention

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskAction

abstract class CleanOldPluginsTask : DefaultTask() {
    @TaskAction
    fun deleteOldPlugins() {
        project.file(project.pluginDir).listFiles()
            ?.filter {  it.isFile && it.extension == "jar" }
            ?.filter {
                // Re-iterate this is better since there is no storage calls compare to re-iterate listFiles
                project.bukkitPlugin.dependencies.any { dep ->
                    it.name.contains(dep.name) && !(dep.version?.let { it1 -> it1 in it.name } ?: false)
                }
            }?.forEach {
                logger.info("Found mismatch version artifact, deleting: $it ")
                it.delete()
            }

    }
}