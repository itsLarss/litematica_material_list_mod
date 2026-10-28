plugins {
	id("net.neoforged.moddev")
	id("me.modmuss50.mod-publish-plugin")
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

// Release-Vorbereitung für Modrinth (und optional CurseForge). Nur Trockenlauf, es wird nie hochgeladen.
val mcReleases = sc.properties.rawOrNull("mod", "mc_releases")?.asList().orEmpty().map { it.toString() }

publishMods {
	file = tasks.jar.flatMap { it.archiveFile }
	version = modVersion
	displayName = "Schematic Tracker $release for NeoForge ${mcReleases.first()}${if (mcReleases.size > 1) "–" + mcReleases.last() else ""}"
	changelog = providers.provider { rootProject.file("CHANGELOG.md").readText() }
	type = STABLE
	modLoaders.add("neoforge")
	dryRun = true

	modrinth {
		projectId = (findProperty("publish.modrinth") as String?).orEmpty().ifBlank { "schematic-tracker" }
		accessToken = providers.environmentVariable("MODRINTH_TOKEN")
		minecraftVersions.addAll(mcReleases)
	}

	if ((findProperty("publish.curseforge") as String?).orEmpty().isNotBlank()) curseforge {
		projectId = findProperty("publish.curseforge") as String
		accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
		minecraftVersions.addAll(mcReleases)
		client = true
		server = false
		javaVersions.add(JavaVersion.toVersion(javaVersion))
	}
}
