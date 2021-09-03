plugins {
    kotlin("jvm") version "1.5.21"
    `java-gradle-plugin`
    `maven-publish`
}

group = "io.github.zap"
version = project.findProperty("ver") as String? ?: System.getenv("VERSION")
        ?: "0.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("gradle.plugin.com.github.jengelman.gradle.plugins:shadow:7.0.0")
}

gradlePlugin {
    plugins {
        create("library") {
            id = "io.github.zap.build.gradle.convention.lib"
            implementationClass = "io.github.zap.build.gradle.convention.LibraryPlugin"
        }

        create("shadow-library") {
            id = "io.github.zap.build.gradle.convention.shadow-lib"
            implementationClass = "io.github.zap.build.gradle.convention.ShadowLibraryPlugin"
        }

        create("mc-plugin") {
            id = "io.github.zap.build.gradle.convention.mc-plugin"
            implementationClass = "io.github.zap.build.gradle.convention.MCPluginPlugin"
        }

        create("shadow-mc-plugin") {
            id = "io.github.zap.build.gradle.convention.shadow-mc-plugin"
            implementationClass = "io.github.zap.build.gradle.convention.ShadowMCPluginPlugin"
        }
    }
}

publishing {
    repositories {
        if(project.findProperty("publishLocal") != null) {
            mavenLocal()
        } else {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/zapzombies/gradle-convention-plugins")
                credentials {
                    val cre = getZgprCredential()
                    username = cre.first
                    password = cre.second
                }
            }
        }
    }
}

tasks.publish {
    dependsOn("check")
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