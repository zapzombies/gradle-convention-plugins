@file:Suppress("UnstableApiUsage")

package io.github.zap.build.gradle.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.api.tasks.testing.Test
import org.gradle.language.jvm.tasks.ProcessResources
import java.net.URI

class LibraryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("org.gradle.java-library")
        project.pluginManager.apply("org.gradle.maven-publish")

        project.version = project.findProperty("ver") as String? ?: System.getenv("VERSION") ?: LOCAL_VERSION
        project.group = "io.github.zap"

        project.extensions.configure<JavaPluginExtension>("java") {
            it.toolchain {
                it.languageVersion.set(JavaLanguageVersion.of(16))
            }
        }

        project.repositories.mavenCentral()
        project.repositories.mavenLocal()

        project.repositories.maven {
            it.url = URI("https://papermc.io/repo/repository/maven-public/")
        }

        // Dependencies
        fun dummyConfig(name: String) : (dependencyNotation: String) -> Unit {
            return {
                project.dependencies.add(name, it)
            }
        }

        val testImplementation = dummyConfig("testImplementation")
        val testRuntimeOnly =  dummyConfig("testRuntimeOnly")

        // I miss kotlin-dsl
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
        testImplementation("org.mockito:mockito-core:3.11.2")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

        // Tasks
        project.tasks.withType(Test::class.java) {
            it.useJUnitPlatform()
        }

        val javaCompile: JavaCompile.() -> Unit = {
            options.compilerArgs.add("-Xlint:unchecked")
            options.compilerArgs.add("-Xlint:deprecation")
        }

        project.tasks.withType(JavaCompile::class.java, javaCompile)

        project.subprojects {
            it.afterEvaluate { aep ->
                aep.tasks.withType(JavaCompile::class.java, javaCompile)
            }
        }

        project.tasks.named("processResources", ProcessResources::class.java) {
            it.expand(mapOf("version" to project.version, "groupId" to project.group, "name" to project.name))
        }
    }
}