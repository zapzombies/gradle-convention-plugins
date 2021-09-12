package io.github.zap.build.gradle.convention

import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

import org.gradle.api.plugins.*
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.logging.LogLevel
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import java.io.File
import java.net.URI
import kotlin.collections.List;

internal val Project.ext: ExtraPropertiesExtension get() =
    (this as ExtensionAware).extensions.getByName("ext") as ExtraPropertiesExtension

fun Project.zgpr(name: String): (MavenArtifactRepository) -> Unit {
    return {
        it.url = URI.create("https://maven.pkg.github.com/zapzombies/$name")
        it.credentials { cre ->
            val zgprCredential =  getZgprCredential()
            cre.username = zgprCredential.first
            cre.password = zgprCredential.second
        }
    }
}

fun Project.getZgprCredential(): Pair<String, String> {
    // Due to security concerns about using service pat
    // every developers will need to generate their own
    // and supply them on first build
    val path = if (project.path == ":") "root" else project.path
    return if(ext.has("zgprCredential-Name") && ext.has("zgprCredential-PAT")) {
        ext["zgprCredential-Name"] as String to ext["zgprCredential-PAT"] as String
    } else {
        val patFile = file("${rootProject.projectDir}/.pat")
        if(patFile.exists()) {
            val parts = patFile.readText().split("\n")
            val username = parts[0].trim()
            val pat = parts[1].trim()
            logger.lifecycle("zgpr: Credential found! Project: $path, Username: $username, PAT: ${pat.substring(0..10)}*** ")
            ext.set("zgprCredential-Name", username)
            ext.set("zgprCredential-PAT", pat)
            parts[0].trim() to parts[1].trim()
        } else {
            // Regular old project props & system env combo
            val username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
            val pat = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
            if(username == null || pat == null) {
                logger.error("zgpr: Credential not found!")
                "" to ""
            } else {
                logger.lifecycle("zgpr: Credential found! Project: $path, Username: $username, PAT: ${pat.substring(0..10)}*** ")
                ext.set("zgprCredential-Name", username)
                ext.set("zgprCredential-PAT", pat)
                username to pat
            }
        }
    }
}

fun Project.publishToZGpr(name: String = project.name, profile: PublicationProfile = PublicationProfile.Auto ) {
    extensions.configure<PublishingExtension>("publishing") {
        it.repositories { repos ->
            if(findPropertyAndEnv("publishLocal") != null) {
                repos.mavenLocal()
            }

            if(findPropertyAndEnv("publishZgpr") != null) {
                repos.maven(project.zgpr(name))
            }
        }

        it.publications {
            it.register("gpr", MavenPublication::class.java) { mp ->
                when(profile) {
                    PublicationProfile.Auto -> {
                        if(pluginManager.hasPlugin("io.github.zap.build.gradle.convention.shadow-lib")) {
                            mp.artifact(tasks.named("shadowJar").get())
                        } else {
                            mp.from(components.getByName("java"))
                        }
                    }
                    PublicationProfile.Java -> mp.from(components.getByName("java"))
                    PublicationProfile.Shadow -> mp.artifact(tasks.named("shadowJar").get())
                    else -> {} // IntelliJ tell me to do this :/
                }
            }
        }
    }
}

fun Project.findPropertyAndEnv(name: String, envName: String = name): Any? {
    return project.findProperty(name) ?: System.getenv(envName)
}

internal fun Project.getOrCreateConfiguration(name: String, configureAction: (Configuration) -> Unit = {}): Configuration {
    return configurations.find { it.name == name } ?: configurations.create(name, configureAction)
}

val Project.shade: Configuration
    get() = getOrCreateConfiguration("shade")

val Project.relocate: Configuration
    get() = getOrCreateConfiguration("relocate")

val Project.bukkitPlugin: Configuration
    get() = getOrCreateConfiguration("bukkitPlugin") {
        it.isTransitive = false
    }

val Project.serverArtifact: Configuration
    get() = getOrCreateConfiguration("serverArtifact") {
        it.isTransitive = false
    }

val Project.serverArtifactVerless: Configuration
    get() = getOrCreateConfiguration("serverArtifactVerless") {
        it.isTransitive = false
    }

val Project.pluginDir: String
    get() = project.ext.get("pluginDir") as String

val Project.outputDir: String
    get() = project.ext.get("outputDir") as String

fun Project.setPluginDir(path: String) {
    project.ext.set("pluginDir", path)
}

fun Project.setOutputDir(path: String) {
    project.ext.set("outputDir", path)
}

fun Project.configureShadow(destination: File,
                            prefix: String = "",
                            relocateConfigs: List<Configuration> = listOf(),
                            shadeConfigs: List<Configuration> = listOf()) {
    val sj = tasks.named("shadowJar", ShadowJar::class.java).get()

    val relocateTask = tasks.getOrRegister("relocate", ConfigureShadowRelocation::class.java ) {
        it.target = sj
        it.prefix = prefix.ifEmpty { "$group.$name.shadow".replace(" ", "").replace("-", "") }
    }

    val configs = mutableListOf(relocate)
    configs.addAll(relocateConfigs)

    val shadeTask = tasks.getOrRegister("shade", ShadeTask::class.java) {
        it.configurations.empty()
        it.configurations.addAll(shadeConfigs)
        it.configurations.add(shade)

        it.shadowTask.set(tasks.named("shadowJar", ShadowJar::class.java))
    }

    tasks.named("shadowJar", ShadowJar::class.java) {
        it.dependsOn(relocateTask.get(), shadeTask.get())

        it.configurations = configs
        it.archiveClassifier.set("")
        it.destinationDirectory.set(destination)
    }

    tasks.named("build", DefaultTask::class.java) {
        it.dependsOn(sj)
    }
}

fun Project.qs(name: String = "", options: ExternalModuleDependency.() -> Unit = {}) : ExternalModuleDependency.() -> Unit {
    return {
        val actualName = name.ifEmpty { this.name }
        version {
            if (isLocalTag(actualName)) {
                it.require(getLocalVersion(actualName))
            } else {
                if(version != null && version!!.isNotEmpty()) {
                    ext["qs-$actualName-ver"] = version
                } else {
                    val ver = getVerFromAncestor(actualName)
                    if (ver != null) {
                        it.require(ver)
                    } else {
                        logger.error("No version specified for dependency $actualName, fallback to 0.0.0-SNAPSHOT")
                        it.require("0.0.0-SNAPSHOT")
                    }
                }
            }
        }
        options()
    }
}

@Suppress("UNCHECKED_CAST")
internal fun Project.getQsLocals(): Set<String>? {
    if(ext.has("qs-useLocals")) {
        val cached = ext["qs-useLocals"]
        if(cached is Set<*>) {
            return cached as Set<String>
        }
    }

    val useLocalClause = project.findProperty("qs.useLocal") as String? ?: System.getenv("QS_USE_LOCAL")
    if(useLocalClause == null) {
        ext["qs-useLocals"] = null
        return null
    }

    val useLocals = useLocalClause.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()
        ext["qs-useLocals"] = useLocals
        return useLocals
}

internal fun Project.isLocalTag(name: String): Boolean {
    val locals = getQsLocals() ?: return false
    if(locals.isEmpty()) return true

    return name in locals
}

internal fun Project.getLocalVersion(name: String = ""): String {
    if(name.isEmpty()) {
        if(ext.has("qs-localVer")) {
            val cached = ext["qs-localVer"]
            if(cached is String) return cached
        }

        val localVersion = (project.findProperty("qs.localVer") as String? ?: System.getenv("QS_LOCAL_VER"))
            ?.ifEmpty { LOCAL_VERSION } ?: LOCAL_VERSION

        ext["qs-localVer"] = localVersion
        return localVersion
    } else { // I should refactor this later, kinda duplicate code
        if(ext.has("qs-localVer-$name")) {
            val cached = ext["qs-localVer-$name"]
            if(cached is String) return cached
        }

        val localVersion = project.findProperty("qs.localVer.$name") as String?
        return if(localVersion != null) {
            ext["qs-localVer-$name"] = localVersion
            localVersion
        } else {
            getLocalVersion()
        }
    }
}

internal fun Project.getVerFromAncestor(name: String): String? {
    return getVerFromAncestorImpl(parent, name)
}

internal fun getVerFromAncestorImpl(project: Project?, name: String): String? {
    if (project == null) return null
    if (!project.ext.has("qs-$name-ver")) return getVerFromAncestorImpl(project.parent, name)

    val ver = project.ext["qs-$name-ver"]
    return if(ver is String) ver else getVerFromAncestorImpl(project.parent, name)

}