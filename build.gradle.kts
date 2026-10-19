plugins {
    id("java")
    id("idea")
    id("net.neoforged.moddev") version "2.0.134"
}

val modId = "superstack"
val modVersion = "21.1-1.0.5"
val mcVersion = "1.21.1"
val neoVersion = "21.1.251"
val parchmentMappings = "2024.11.17"

base {
    archivesName = "SuperStack-$mcVersion"
    version = modVersion
    group = "com.zaixiayesheng.superstack"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

repositories {
    mavenCentral()
}

neoForge {
    enable {
        version = neoVersion
    }

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

val expandProps = mapOf(
    "version" to modVersion,
    "minecraft_version" to mcVersion,
    "neo_version" to neoVersion
)

tasks.processResources {
    inputs.properties(expandProps)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(expandProps)
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}