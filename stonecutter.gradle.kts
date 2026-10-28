plugins {
	id("dev.kikugie.stonecutter")
	id("net.neoforged.moddev") version "2.0.148" apply false
	id("me.modmuss50.mod-publish-plugin") version "2.2.1" apply false
}

stonecutter active "26.3-fabric"

stonecutter parameters {
	val (version, loader) = current.project.split('-', limit = 2)

	// Werte aus stonecutter.properties.toml je nach Version und Loader
	properties {
		tags(version, loader)
	}

	// Für //? if fabric { … } bzw. //? if neoforge { … }
	constants {
		match(loader, "fabric", "neoforge")
	}

	replacements {
		string(current.parsed >= "1.21.11") {
			replace("ResourceLocation", "Identifier")
		}
	}
}

// ./gradlew buildAll – Tests, dann alle Kombinationen bauen; die Jars landen in build/libs/<mod.version>/
tasks.register("buildAll") {
	group = "build"
	description = "Builds every Minecraft version and loader and collects the jars"
	dependsOn(":core:test")
	dependsOn(stonecutter.tasks.named("buildAndCollect"))
}

// ./gradlew publishDryRun – legt alle Release-Dateien mit Metadaten unter versions/*/build/publishMods/ ab.
// Lädt nichts hoch: dryRun ist in den Build-Skripten fest eingeschaltet.
tasks.register("publishDryRun") {
	group = "publishing"
	description = "Prepares every Modrinth/CurseForge upload without publishing anything"
	dependsOn(stonecutter.tasks.named("publishMods"))
}
