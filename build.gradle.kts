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
}