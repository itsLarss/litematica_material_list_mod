plugins {
	// Wendet je nach Minecraft-Version die passende Loom-Variante an
	id("dev.kikugie.loom-back-compat")
}

val release = property("mod.version") as String
val modVersion = "${release}+${sc.current.version}-fabric"
version = modVersion
base.archivesName = property("mod.id") as String

val javaVersion = if (sc.current.parsed >= "26.1") 25 else 21

repositories {
	maven("https://maven.terraformersmc.com/releases/") { name = "Terraformers" }
}

dependencies {
	minecraft("com.mojang:minecraft:${sc.current.version}")
	// Mojang-Namen für die verschleierten Versionen (bis 1.21.x); ab 26.1 ohne Wirkung
	loomx.applyMojangMappings()

	modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
	modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

	// Optional: Einstellungen über Mod Menu
	modCompileOnly("com.terraformersmc:modmenu:${property("deps.modmenu")}")
}

sourceSets.main {
	java.srcDir(rootProject.file("core/src/main/java"))
	java.exclude("**/neoforge/**")
}

loom {
	runConfigs.all {
		runDirectory = rootProject.file("run/${sc.current.project}")
	}
}

tasks {
	processResources {
		val props = mapOf(
			"version" to modVersion,
			"minecraft" to sc.properties.get<String>("mod.mc_compat"),
			"java" to javaVersion.toString(),
			// 26.x braucht einen Loader, der unverschleierte Versionen kennt
			"loader" to if (javaVersion >= 25) "0.19.5" else "0.16.0",
		)
		inputs.properties(props)
		filesMatching("fabric.mod.json") { expand(props) }
		exclude("META-INF/neoforge.mods.toml")
	}

	withType<JavaCompile>().configureEach {
		options.release = javaVersion
		options.encoding = "UTF-8"
	}

	withType<Jar>().configureEach {
		from(rootProject.file("LICENSE")) { rename { "${it}_schematic-tracker" } }
	}

	// Fertiges Jar nach build/libs/<mod.version>/ im Hauptordner kopieren
	register<Copy>("buildAndCollect") {
		group = "build"
		dependsOn("build")
		from(loomx.modJar.flatMap { it.archiveFile })
		into(rootProject.layout.buildDirectory.dir("libs/$release"))
	}
}

java {
	sourceCompatibility = JavaVersion.toVersion(javaVersion)
	targetCompatibility = JavaVersion.toVersion(javaVersion)
}
