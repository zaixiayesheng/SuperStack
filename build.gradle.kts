plugins {
    id("java")
    id("idea")
    id("net.neoforged.moddev.legacyforge") version "2.0.141"
}

val modId = "superstack"
val modVersion = "20.1-1.0.3"
val mcVersion = "1.20.1"
val forgeVersion = "47.4.0"
val parchmentMappings = "2023.09.03"

base {
    archivesName = "SuperStack-$mcVersion"
    version = modVersion
    group = "com.zaixiayesheng.superstack"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

repositories {
    maven {
        name = "Sponge (Mixin)"
        url = uri("https://repo.spongepowered.org/repository/maven-public/")
    }
    mavenCentral()
}

dependencies {
    // Mixin 注解处理器：生成 refmap（正式环境的 SRG 方法名映射），旧版 Forge 构建必需
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")

    // 只在编译期存在，让 Mixin 注解处理器能解析 compat mixin 的字符串目标；
    // 运行时不带、由玩家的模组自己提供。libs/ 里的 jar 不进仓库。
    val compatJars = fileTree("libs") { include("*.jar") }
    if (!compatJars.isEmpty) {
        compileOnly(compatJars)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

legacyForge {
    version = "$mcVersion-$forgeVersion"

    parchment {
        mappingsVersion = parchmentMappings
        minecraftVersion = mcVersion
    }

    runs {
        create("client", Action {
            client()
            gameDirectory.set(file("run"))
            jvmArgument("-Dmixin.debug.export=true")
        })

        create("server", Action {
            server()
            gameDirectory.set(file("run-server"))
            programArgument("--nogui")
            jvmArgument("-Dmixin.debug.export=true")
        })
    }

    mods {
        create(modId, Action {
            sourceSet(sourceSets.main.get())
        })
    }
}

mixin {
    add(sourceSets.main.get(), "$modId.refmap.json")
    config("$modId.mixins.json")
    config("$modId.compat.mixins.json")
}

val expandProps = mapOf(
    "version" to modVersion,
    "minecraft_version" to mcVersion,
    "forge_version" to forgeVersion
)

tasks.processResources {
    inputs.properties(expandProps)
    // mods.toml 里有中文，按 UTF-8 读写（默认会用平台编码，中文会糊掉）
    filteringCharset = "UTF-8"
    filesMatching("META-INF/mods.toml") {
        expand(expandProps)
    }
}

tasks.jar {
    manifest {
        attributes(
            "MixinConfigs" to "$modId.mixins.json,$modId.compat.mixins.json"
        )
    }
}

dependencies {
    // Unit tests for the bulk crafting arithmetic (no game classes involved).
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
    }
}