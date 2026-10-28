// Reine Java-Logik ohne Minecraft. Wird in jedes Mod-Jar mit einkompiliert; die Tests laufen nur hier.
plugins {
	java
}

repositories {
	mavenCentral()
}

dependencies {
	// Gson bringt Minecraft selbst mit; hier nur zum Kompilieren und Testen
	compileOnly("com.google.code.gson:gson:2.11.0")
	testImplementation("com.google.code.gson:gson:2.11.0")
	testImplementation(platform("org.junit:junit-bom:5.13.4"))
	testImplementation("org.junit.jupiter:junit-jupiter")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
	// 21, damit der Code auch in den Jars für Minecraft 1.21 läuft
	options.release = 21
	options.encoding = "UTF-8"
}

tasks.test {
	useJUnitPlatform()
}
