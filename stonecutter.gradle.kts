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

// ./gradlew runClients -Pclients=<auswahl> – startet mehrere Test-Clients gleichzeitig.
// Auswahl (kommagetrennt): all, fabric, neoforge, eine Minecraft-Version (1.21.4) oder eine Kombination (26.3-neoforge).
// Beispiel: -Pclients=fabric   oder   -Pclients=1.21.4,26.3-neoforge
tasks.register("runClients") {
	group = "run"
	description = "Starts several test clients at once (-Pclients=all|fabric|neoforge|<version>|<version>-<loader>)"
	val wanted = (findProperty("clients") as String? ?: "all").split(',').map { it.trim() }.filter { it.isNotEmpty() }
	dependsOn(stonecutter.tasks.named("runClient") {
		val project = metadata.project
		wanted.any { it == "all" || project == it || project.endsWith("-$it") || project.startsWith("$it-") }
	})
}

// ./gradlew publishDryRun – legt alle Release-Dateien mit Metadaten unter versions/*/build/publishMods/ ab, lädt nichts hoch.
// ./gradlew publishRelease -Ppublish.live=true (mit MODRINTH_TOKEN) – lädt alle Kombinationen auf Modrinth hoch.
// Normalerweise startet das der Release-Workflow auf GitHub (.github/workflows/release.yml), nicht man selbst.
val live = findProperty("publish.live") == "true"

tasks.register("publishDryRun") {
	group = "publishing"
	description = "Prepares every Modrinth upload without publishing anything"
	dependsOn(stonecutter.tasks.named("publishMods"))
}

tasks.register("publishRelease") {
	group = "publishing"
	description = "Publishes every version and loader to Modrinth (needs -Ppublish.live=true and MODRINTH_TOKEN)"
	dependsOn(":core:test")
	dependsOn(stonecutter.tasks.named("publishMods"))
}

// Vor dem ersten Upload prüfen, nicht mittendrin: sonst landet nur ein Teil der Dateien auf Modrinth
gradle.taskGraph.whenReady {
	val version = rootProject.property("mod.version").toString()
	if (hasTask(":publishDryRun") && live) {
		throw GradleException("publishDryRun lädt nie hoch – ohne -Ppublish.live=true starten.")
	}
	if (hasTask(":publishRelease")) {
		if (!live) throw GradleException("publishRelease braucht -Ppublish.live=true (sonst ./gradlew publishDryRun).")
		if (System.getenv("MODRINTH_TOKEN").isNullOrBlank()) throw GradleException("MODRINTH_TOKEN fehlt.")
		val hasSection = rootProject.file("CHANGELOG.md").readLines().any { it.trim() == "## $version" }
		if (!hasSection) throw GradleException("CHANGELOG.md hat keinen Abschnitt \"## $version\".")
	}
}
