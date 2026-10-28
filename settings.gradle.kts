pluginManagement {
	repositories {
		mavenCentral()
		gradlePluginPortal()
		maven("https://maven.fabricmc.net/") { name = "Fabric" }
		maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
		maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
	}
}

plugins {
	id("dev.kikugie.stonecutter") version "0.9.8"
	// Wählt die passende Loom-Variante: verschleiert bis 1.21.x, unverschleiert ab 26.1
	id("dev.kikugie.loom-back-compat") version "0.4.3"
}

stonecutter {
	create(rootProject) {
		// Ein Teilprojekt pro Minecraft-Version und Loader: versions/<projekt>-<loader>
		fun mc(project: String, vararg loaders: String, version: String = project) {
			for (loader in loaders) version("$project-$loader", version).buildscript("build.$loader.gradle.kts")
		}

		// NeoForge nur, wo es ein stabiles Release gibt (nicht für 1.21.2, 1.21.6, 1.21.7, 1.21.9)
		mc("1.21", "fabric", "neoforge")
		mc("1.21.1", "neoforge")
		mc("1.21.2", "fabric")
		mc("1.21.3", "fabric", "neoforge")
		mc("1.21.4", "fabric", "neoforge")
		mc("1.21.5", "fabric", "neoforge")
		mc("1.21.6", "fabric")
		mc("1.21.7", "fabric")
		mc("1.21.8", "fabric", "neoforge")
		mc("1.21.9", "fabric")
		mc("1.21.10", "fabric", "neoforge")
		mc("1.21.11", "fabric", "neoforge")
		mc("26.1", "fabric", "neoforge")
		mc("26.2", "fabric", "neoforge")
		mc("26.3", "fabric", "neoforge")
		vcsVersion = "26.3-fabric"
	}
}

// Reine Java-Logik ohne Minecraft (API, Sync, Filter, Mengen) mit eigenen Tests
include("core")

rootProject.name = "schematic-tracker"
