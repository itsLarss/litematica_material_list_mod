plugins {
	id("net.neoforged.moddev")
}

val release = property("mod.version") as String
val modVersion = "${release}+${sc.current.version}-neoforge"
version = modVersion
base.archivesName = property("mod.id") as String

val javaVersion = if (sc.current.parsed >= "26.1") 25 else 21

sourceSets.main {
	java.srcDir(rootProject.file("core/src/main/java"))
	java.exclude("**/fabric/**")
}

neoForge {
	version = property("deps.neoforge") as String

	mods {
		register(property("mod.id") as String) {
			sourceSet(sourceSets.main.get())
		}
	}

	runs {
		register("client") {
			client()
			gameDirectory = rootProject.file("run/${sc.current.project}")
		}
	}
}

// NeoForge baut Minecraft pro Version neu zusammen; nie mehrere gleichzeitig, sonst reicht der Speicher nicht
interface NeoForgeMutex : BuildService<BuildServiceParameters.None>

val mutex = gradle.sharedServices.registerIfAbsent("createMinecraftArtifactsMutex", NeoForgeMutex::class.java) {
	maxParallelUsages = 1
}

tasks {
	named("createMinecraftArtifacts") {
		dependsOn("stonecutterGenerate")
		usesService(mutex)
	}

	processResources {
		val props = mapOf(
			"version" to modVersion,
			"minecraft" to sc.properties.get<String>("mod.mc_compat"),
		)
		inputs.properties(props)
		filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
		exclude("fabric.mod.json")
	}

	withType<JavaCompile>().configureEach {
		options.release = javaVersion
		options.encoding = "UTF-8"
	}

	withType<Jar>().configureEach {
		from(rootProject.file("LICENSE")) { rename { "${it}_schematic-tracker" } }
	}

	register<Copy>("buildAndCollect") {
		group = "build"
		dependsOn("build")
		from(jar.flatMap { it.archiveFile })
		into(rootProject.layout.buildDirectory.dir("libs/$release"))
	}
}

java {
	sourceCompatibility = JavaVersion.toVersion(javaVersion)
	targetCompatibility = JavaVersion.toVersion(javaVersion)
}
