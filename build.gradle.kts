plugins {
    id("fabric-loom") version "1.15.4"
    id("java")
}

group = "me.totalchaos01"
version = "1.3.0"

val minecraftVersion = "1.21.11"
val yarnMappings = "1.21.11+build.4"
val fabricLoaderVersion = "0.18.4"
val fabricApiVersion = "0.141.3+1.21.11"

loom {
    // Access widener for deeper Minecraft access without mixins
    accessWidenerPath = file("src/main/resources/chaosclient.accesswidener")
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://repo.viaversion.com")
    flatDir { dirs("libs") }
}

dependencies {
    minecraft("com.mojang:minecraft:${minecraftVersion}")
    mappings("net.fabricmc:yarn:${yarnMappings}:v2")
    modImplementation("net.fabricmc:fabric-loader:${fabricLoaderVersion}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricApiVersion}")

    // JSON config
    implementation("com.google.code.gson:gson:2.11.0")

    // Baritone — embedded pathfinding (jar-in-jar)
    modImplementation("baritone:baritone-fabric:1.15.0")
    include("baritone:baritone-fabric:1.15.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
    manifest {
        attributes(
            // Java Agent support — allows loading via -javaagent: for deep integration
            "Premain-Class" to "me.totalchaos01.chaosclient.bootstrap.ChaosAgent",
            "Agent-Class" to "me.totalchaos01.chaosclient.bootstrap.ChaosAgent",
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true"
        )
    }
}

// Task to copy the built jar to ~/.chaosclient/libraries/ for classpath loading
tasks.register<Copy>("deployLibrary") {
    dependsOn("remapJar")
    from(tasks.named("remapJar").map { (it as org.gradle.jvm.tasks.Jar).archiveFile })
    into(file("${System.getProperty("user.home")}/.chaosclient/libraries/chaosclient"))
    rename { "ChaosClient-${project.version}.jar" }
    doLast {
        println("Deployed ChaosClient to ~/.chaosclient/libraries/chaosclient/")
    }
}

// Task to also copy to mods for backward compatibility
tasks.register<Copy>("deployMod") {
    dependsOn("remapJar")
    from(tasks.named("remapJar").map { (it as org.gradle.jvm.tasks.Jar).archiveFile })
    into(file("${System.getProperty("user.home")}/.chaosclient/mods"))
    rename { "ChaosClient-${project.version}.jar" }
}