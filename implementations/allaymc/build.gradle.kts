import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar // 确保导入

plugins {
    bluemap.implementation
    kotlin("jvm") version "2.1.0" // 与你的版本一致
    id("com.gradleup.shadow") version "8.3.0" // 与你的版本一致
}

// 定义项目属性，这些将被用于 plugin.json
group = "de.bluecolored.bluemap.allay" // 根据 BlueMap 的实际 group 修改
version = project.version // 从 common 项目或父项目继承，或者在此定义
description = "A 3d-map of your Minecraft worlds view-able in your browser using three.js (WebGL)" // 插件描述

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
    maven("https://repo.opencollab.dev/maven-releases/") // AllayMC 仓库
    maven("https://repo.opencollab.dev/maven-snapshots/")// AllayMC 仓库
    // 如果 BlueMap 有自己的 Maven 仓库，也需要添加
    // e.g., maven("https://repo.bluecolored.de/releases")
}

dependencies {
    // BlueMap common 模块，假设它不依赖 Spigot API
    api ( project( ":common" ) ) {
        exclude( group = "com.google.code.gson", module = "gson" )
    }

    // Adventure API (Gson Serializer)
    api ( libs.adventure.gson ) { // 假设 libs.adventure.gson 在 version catalog 中定义
        exclude ( group = "com.google.code.gson", module = "gson" )
    }

    api ( libs.adventure.legacy ) // 用于解析 Component 为带颜色符号的字符串

    // AllayMC API
    compileOnly(group = "org.allaymc.allay", name = "api", version = "0.4.1") // 检查并使用最新的 Allay API 版本


    api ( libs.bluecommands.brigadier )
//    {
//        exclude ( group = "com.mojang", module = "brigadier" )
//    }
}

kotlin {
    jvmToolchain(21) // 或更高版本，与 AllayMC 兼容的 JDK 版本，示例中使用 21
}

tasks.shadowJar {
    archiveClassifier.set("") // 生成的 jar 文件名不带 -all 或 -shadow 后缀

    // Relocations 保持不变，这些对于避免库冲突很重要
    relocate ("net.kyori", "de.bluecolored.shadow.adventure")
    relocate ("io.airlift", "de.bluecolored.shadow.airlift")
    relocate ("com.github.benmanes.caffeine", "de.bluecolored.shadow.caffeine")
    relocate ("org.checkerframework", "de.bluecolored.shadow.checkerframework")
    relocate ("com.google.errorprone", "de.bluecolored.shadow.errorprone")
    relocate ("org.apache.commons", "de.bluecolored.shadow.apache.commons")
    relocate ("org.spongepowered.configurate", "de.bluecolored.shadow.configurate")
    relocate ("com.typesafe.config", "de.bluecolored.shadow.typesafe.config")
    relocate ("io.leangen.geantyref", "de.bluecolored.shadow.geantyref")
    relocate ("net.jpountz", "de.bluecolored.shadow.jpountz")
//    relocate ("org.bstats", "de.bluecolored.shadow.bstats") // 如果使用 bStats
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE // 或 WARN，根据需要调整
    from("src/main/resources") {
        include("plugin.json") // 处理 plugin.json 而不是 plugin.yml
        expand(
            "version" to project.version,
            "description" to project.description,
            // 你需要在这里定义插件主入口，或者直接在 plugin.json 中硬编码然后替换group部分
            // 例如，如果你的主类是 de.bluecolored.bluemap.allay.AllayBlueMapPlugin
            "entrance_class_name" to "allay.AllayBlueMapPlugin", // 相对于 project.group 的部分
            "project_group" to project.group // 将 group 传递给 plugin.json
        )
    }
    // 从 AllayMC 示例复制并调整 plugin.json 的处理方式
    // 如果你的 plugin.json 文件结构更简单，可以直接使用 expand
    // 如果需要更复杂的文本替换，可以使用 doLast：
    doLast {
        val origin = project.file("src/main/resources/plugin.json") // 确保路径正确
        val processed = layout.buildDirectory.file("resources/main/plugin.json").get().asFile

        // 如果 plugin.json 模板中使用了 ${project_group} 和 ${entrance_class_name}
        // 并且你的 "entrance" 字段是 "${project_group}.${entrance_class_name}"
        // 或者，如果 "entrance" 字段模板是 "your.fixed.package.${entrance_class_name_suffix}"
        // 你需要相应调整这里的替换逻辑
        // 以下是一个更通用的替换，假设 plugin.json 中有 ${version}, ${description}, ${entrance}

        var content = origin.readText()
        content = content.replace("\${version}", project.version.toString())
        content = content.replace("\${description}", project.description.toString())
        // 假设你的 plugin.json 中 entrance 字段是类似 "${group}.your.main.Class"
        // 并且你在上面 expand 中定义了 "main_class_path" to "your.main.Class"
        // content = content.replace("\${group}", project.group.toString())
        // content = content.replace("\${main_class_path}", "your.main.Class") // 或者从属性获取

        // 基于AllayMC示例的入口替换方式：
        // 假设 plugin.json 中 entrance 字段是： "entrance": ".YourMainClass"
        // 那么替换逻辑是：
        val mainClassSimpleName = "allay.AllayBlueMapPlugin" // 你的主类的简单名称或相对于group的路径
        content = content.replaceFirst("\"entrance\": \".", "\"entrance\": \"" + project.group.toString() + ".")
            .replaceFirst("\"", mainClassSimpleName + "\"") // 这是一个简化的示例，你需要确保这个替换正确

        // 更稳妥的方式是直接在 plugin.json 模板中使用 `${full_entrance_class}`
        // 然后在 expand 中: "full_entrance_class" to "${project.group}.allay.AllayBlueMapPlugin"
        // 这种方式下，这里的 doLast 逻辑会更简单，只需替换 version 和 description

        processed.writeText(content)
    }
}

// 可选: 添加 runServer 任务，方便测试 (从 AllayMC 示例借鉴)
tasks.register<JavaExec>("runServer") {
    outputs.upToDateWhen { false }
    dependsOn("shadowJar")

    val shadowJarTask = tasks.named("shadowJar", ShadowJar::class).get()
    val pluginJar = shadowJarTask.archiveFile.get().asFile
    val cwd = layout.buildDirectory.file("run").get().asFile
    val pluginsDir = cwd.resolve("plugins").apply { mkdirs() }
    doFirst { pluginJar.copyTo(pluginsDir.resolve(pluginJar.name), overwrite = true) }

    val allayGroup = "org.allaymc.allay"
    val allayApiDep = configurations.compileOnly.get().dependencies.find { it.group == allayGroup && it.name == "api" }
        ?: throw GradleException("Allay API dependency not found in compileOnly configuration.")
    val serverDependency = dependencies.create("$allayGroup:server:${allayApiDep.version}") // 使用API的版本号来确定服务器版本

    classpath = files(configurations.detachedConfiguration(serverDependency).resolve())
    mainClass.set("org.allaymc.server.Allay")
    workingDir = cwd
    standardInput = System.`in` // 允许在控制台输入服务器命令
}
