plugins {
    java
    `java-library`
    id ("com.github.node-gradle.node") version "3.0.1"
    id ("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "de.bluecolored.bluemap"
version = System.getProperty("bluemap.version") ?: "?" // set by BlueMapCore

val javaTarget = 21;
java {
    sourceCompatibility = JavaVersion.toVersion(javaTarget)
    targetCompatibility = JavaVersion.toVersion(javaTarget)
}

repositories {
    mavenLocal()
    mavenCentral()
    maven ("https://www.jitpack.io/")
    maven ("https://repo.opencollab.dev/maven-releases/")
    maven ("https://repo.opencollab.dev/maven-snapshots/")
    maven ("https://storehouse.okaeri.eu/repository/maven-public/")
    maven ("https://libraries.minecraft.net")
    maven ("https://oss.sonatype.org/content/repositories/snapshots")
    maven ("https://repo.bluecolored.de/releases")
}

dependencies {
    api ("de.bluecolored.bluemap:BlueMapCommon") {
        //exclude dependencies provided by allay
        exclude( group = "com.google.guava", module = "guava" )
        exclude( group = "com.google.code.gson", module = "gson" )
    }

    compileOnly ("org.allaymc", "Allay-API", "1.0.0")

    testImplementation ("org.junit.jupiter:junit-jupiter:5.8.2")
    testRuntimeOnly ("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

tasks.withType(JavaCompile::class).configureEach {
    options.apply {
        encoding = "utf-8"
    }
}

tasks.withType(AbstractArchiveTask::class).configureEach {
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    from("src/main/resources") {
        include("plugin.json")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        expand (
            "version" to project.version
        )
    }
}
tasks.shadowJar {
    destinationDirectory.set(file("../../build/release"))
    archiveFileName.set("BlueMap-${project.version}-${project.name}.jar")
}