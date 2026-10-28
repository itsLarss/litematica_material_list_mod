# Schematic Tracker – Minecraft-Mod (Fabric)

Projektwissen zum Nachlesen: was die Mod kann, wie sie aufgebaut ist, wie man sie baut und testet und woran man
beim Weiterbauen denken muss. Gilt auch als Anleitung für Claude Code, wenn du lokal damit arbeitest.

Die Website liegt im Repo `itsLarss/Litematica_Material_List_Webapp` (eigene `CLAUDE.md`, API-Doku in
`docs/api-v1.md`).

---

## 1. Was ist das?

Eine reine **Client-Mod** für Fabric und NeoForge. Sie zeigt die Materialliste von der Schematic-Tracker-Website als kleines HUD
im Spiel (im Stil der Litematica-Materialliste) und hält sie live mit der Website synchron. Braucht kein
Litematica und funktioniert auf jedem Server.

### Funktionen

- **Verbinden:** `/tracker link <Code>` (Code von der Website: Teilen → Minecraft-Mod → Code erzeugen) oder
  `/tracker link <Freigabe-Link>`. Die Website-Adresse einmal mit `/tracker server <Adresse>` setzen.
- **HUD:** echte Item-Icons, Name in der Spielsprache, fehlende Menge `70417 (40,75 SB)`, Fortschritt in Prozent,
  „… +N weitere“. Ausgeblendet in Menüs (einstellbar) und bei F3. Taste **J** = ein/aus.
- **Live-Sync:** alle 2,5 s nur die Änderungen (`?since=`), nach 2 Minuten Ruhe alle 10 s, nur solange die Liste
  sichtbar ist und das Spiel nicht pausiert.
- **Ganze Liste** (Taste **K**): scrollen, Filter Offen/Meine/Alle, Sortierung, Klick = „Ich sammle das“ setzen
  bzw. freigeben, Tooltip mit Menge und wer sammelt.
- **Mengen eintragen:** Rechtsklick in der ganzen Liste → Feld mit Schnelleingabe wie auf der Website
  (`core/QuickAmount`, gleiche Fälle wie `tests/features.test.ts` der Website) und Knöpfe +1 St/SB/DK.
  `/tracker add <item> <menge>` addiert, `/tracker set <item> <menge>` setzt; `<item>` = ID ohne `minecraft:`,
  Name (in Anführungszeichen, wenn mit Leerzeichen) oder `hand`. Gespeichert wird gesammelt nach 600 ms
  (`ListSync.setHave`), eigene Eingaben haben Vorrang vor Abrufen, bis der Server sie bestätigt hat.
- **Fertig-Meldungen:** Toast + Ton, wenn ein Material voll ist, bei „Liste fertig“ zusätzlich Konfetti (eigene
  Animation über `Gfx`, keine Partikel). Erkennung in `core/Completions` (beim ersten Laden still), Anzeige in
  `Notifications`. Einzeln abschaltbar (`notifyToast`, `notifySound`, `notifyConfetti`), „Vorschau“ in den
  Einstellungen. Toast-API je Version in `Compat.toast` (bis 1.21.1 `getToasts()`, ab 26.2 `gui.toastManager()`).
- **Farm-Session:** `core/FarmSession` zählt eigene Zuwächse (über `ListSync.onOwnAmount`), Tempo pro Stunde ab
  1 Minute Laufzeit, Restzeit = fehlend / Tempo für das zuletzt erhöhte Material. Start beim ersten Eintrag,
  Neustart per `/tracker session start`, Taste (ohne Standardbelegung) oder Einstellungen; HUD-Zeile abschaltbar
  (`hudSession`). Nur im Speicher, nicht in der Config.
- **Notizen (nur lesen):** `note` und `items[].note` aus API v1 (`TrackerApi.optStr`: fehlendes Feld = null, so
  bleibt es bei älteren Websites und Nur-Ansehen-Links still aus). HUD kurz (abschaltbar, `hudNotes`), Liste `K`
  voll im Notizbereich unten (Material unter der Maus, sonst Listen-Notiz). Schreiben geht nur auf der Website.
- **Wer sammelt was im HUD:** Claims anderer neben der Menge (`hudClaims`), Farbe je Name aus fester Palette
  (`Texts.claimColor`, `claimColors`). Eigene Claims weiter nur als gelber Name.
- **Einstellungen auf zwei Seiten** (`TrackerConfigScreen.morePage`): Seite 1 Verbindung + Aussehen, Seite 2
  Meldungen, Farm-Session, Notizen, Claims. Neue Optionen auf Seite 2; beide Seiten müssen bei GUI-Größe 4
  (270 px hoch) passen.
- **Deadline (nur anzeigen):** `deadline` aus API v1 und – falls vorhanden – `deadlineStatus` (`core/Deadline`,
  `TrackerApi.parseDeadline`). Ohne Status nur das Datum, die Mod rechnet nicht selbst. HUD-Zeile (`hudDeadline`)
  und Überschrift in `K`, Farbe nach Status.
- **Voreinstellungen:** `core/Preset` (MINIMAL, CLEAR = Standard, ALL) setzt nur Anzeige- und Meldungs-Optionen,
  nie Verbindung, Filter, Sortierung, Einheit, Ecke oder Name. Knöpfe oben auf Seite 2 (aktive mit „✔“) und
  `/tracker config preset <minimal|clear|all>`. Neue Anzeige-Optionen auch in `Preset.apply` und `matches` eintragen.
- **Einstellungen** (Mod Menu, `/tracker config` oder „Einstellungen…“ in der Liste): Website, Verbinden/Trennen,
  HUD an/aus, Filter, Sortierung, Einheit (Stacks/SB/DK), Ecke, Zeilen, Größe, Hintergrund, in Menüs ausblenden,
  eigener Name für „Meine“, Fertig-Meldung/-Ton/Konfetti.
- **Befehle:** `/tracker` (Status), `link`, `unlink`, `server`, `status`, `refresh`, `hud [on|off]`,
  `filter <open|mine|all>`, `sort <missing|name|list>`, `list`, `config`, `add <item> <menge>`, `set <item> <menge>`, `session [start|stop]`, `config preset <minimal|clear|all>`.
- **Sprachen:** Englisch und Deutsch (`en_us.json`, `de_de.json`), richtet sich nach der Spielsprache.

---

## 2. Branches und Versionen

Seit 0.2.0 liegen **alle Minecraft-Versionen und beide Loader in einer Codebasis** (Stonecutter).

| Branch     | Wofür                                  |
| ---------- | -------------------------------------- |
| `main`     | Releases                               |
| `dev-main` | laufende Arbeit → PR nach `main`       |

Die alten Reihen-Branches (`dev-26.x`, `main-1.21.x`, `dev-1.21.x`) werden nach dem ersten gemeinsamen Release als
Tags `archive/<branch>` gesichert und gelöscht. Bis dahin nur noch für Hotfixes der alten Versionen.

Pull Requests: **base = Ziel**, **compare = Quelle**.

### Versionsnummern

- Eine Mod-Version für alle Kombinationen: `mod.version` in `stonecutter.properties.toml` (ab `0.2.0`).
- Datei/Modrinth-Version: `<mod.version>+<minecraft>-<loader>`, z. B. `0.2.0+26.3-fabric`.
- Alte Nummern bis `0.1.3` (`0.0.x-1.21.x`, `0.1.x-26.x`, nur Fabric) bleiben, wie sie sind.

### Unterstützte Kombinationen

| Minecraft        | Fabric                  | NeoForge                                         | Java |
| ---------------- | ----------------------- | ------------------------------------------------ | ---- |
| 1.21 – 1.21.1    | ein Jar für beide       | je ein Jar (21.0 / 21.1)                          | 21   |
| 1.21.2 … 1.21.11 | je ein Jar              | 1.21.3, 1.21.4, 1.21.5, 1.21.8, 1.21.10, 1.21.11  | 21   |
| 26.1 – 26.1.2    | ein Jar für alle drei   | nur 26.1.2                                        | 25   |
| 26.2, 26.3       | je ein Jar              | je ein Jar                                        | 25   |

NeoForge nur dort, wo es ein **stabiles** NeoForge-Release gibt (für 1.21.2, 1.21.6, 1.21.7, 1.21.9 und
26.1.0/26.1.1 gibt es nur Betas). Die NeoForge-Mod-ID ist `schematic_tracker` (NeoForge erlaubt keinen Bindestrich),
auf Fabric `schematic-tracker`. Sprachdateien, Config-Datei und Texte sind gleich.

---

## 3. Bauen

### Voraussetzungen

- **JDK 25** zum Ausführen von Gradle (Loom 1.19 braucht es). Die 1.21-Jars werden trotzdem für Java 21 gebaut.
  Zeigt `JAVA_HOME` auf einen kaputten Pfad, vor den Befehl `JAVA_HOME="C:/Program Files/Java/jdk-25.0.4"` setzen.
- Internet beim ersten Build (Minecraft, Fabric, NeoForge, Mod Menu werden geladen). Der erste Gesamt-Build dauert
  lange, weil NeoForge Minecraft pro Version neu zusammenbaut (nacheinander, damit der Speicher reicht).

### Befehle

```bash
./gradlew buildAll                         # Tests + alle Kombinationen → build/libs/<mod.version>/
./gradlew :26.3-fabric:buildAndCollect     # eine Kombination → build/libs/<mod.version>/
./gradlew :1.21.4-neoforge:build           # eine Kombination → versions/1.21.4-neoforge/build/libs/
./gradlew :core:test                       # nur die Unit-Tests
./gradlew :26.3-fabric:runClient           # Minecraft mit der Mod starten (Ordner run/26.3-fabric/)
./gradlew runClients -Pclients=fabric      # mehrere Clients gleichzeitig: all | fabric | neoforge |
                                           #   Version (1.21.4) | Kombination (26.3-neoforge), kommagetrennt
./gradlew publishDryRun                    # Modrinth-Trockenlauf, lädt NICHTS hoch
```

### Jars von GitHub holen

Jeder Push baut alle Kombinationen per GitHub Action (`.github/workflows/build.yml`, ein Job pro Kombination).
**Actions → oberster Lauf → ganz unten „Artifacts“** – pro Kombination eine ZIP mit dem Jar (Login nötig, 90 Tage).
Neue Kombination = auch in die Matrix dort eintragen.

### Testen im Spiel

1. Website lokal oder live laufen lassen (siehe Website-`CLAUDE.md`).
2. `./gradlew :<version>-<loader>:runClient`, Welt anlegen.
3. `/tracker server http://localhost:3000` (bzw. die echte Adresse), auf der Website Code erzeugen,
   `/tracker link <Code>`.
4. Prüfen: HUD erscheint, Änderung auf der Website taucht nach ein paar Sekunden auf, `K` öffnet die Liste,
   Klick setzt „Ich sammle das“ (erscheint im Verlauf der Website). Auf NeoForge zusätzlich: Mod-Liste →
   Schematic Tracker → Einstellungen.

Jeder Test-Client bekommt höchstens 2 GB Speicher (`-PclientMemory=3G` ändert das). Alle 25 gleichzeitig brauchen
entsprechend viel RAM – lieber gruppenweise starten.

Im Entwicklungs-Client heißt der Spieler jedes Mal anders (`Player123`), deshalb ist „Meine“ dort meist leer.

### Teststatus

Siehe `TESTING.md`: Tabelle aller Kombinationen mit „gebaut“ / „im Spiel getestet“. **Nur eintragen, was wirklich
im Spiel geprüft wurde** – kompiliert ist nicht getestet.

---

## 4. Aufbau

```
settings.gradle.kts               Liste der Kombinationen (Stonecutter), include("core")
stonecutter.gradle.kts            aktive Version, Konstanten (fabric/neoforge), Ersetzungen, buildAll, publishDryRun
stonecutter.properties.toml       pro Version/Loader: Fabric API, NeoForge, Mod Menu, Bereiche, Modrinth-Versionen
build.fabric.gradle.kts           Build für Fabric (Loom über loom-back-compat: verschleiert bis 1.21.x, sonst nicht)
build.neoforge.gradle.kts         Build für NeoForge (ModDevGradle)
versions/<mc>-<loader>/           von Stonecutter verwaltete Teilprojekte (nur build/ darin)
core/                             reine Java-Logik ohne Minecraft (testbar), wird in jedes Jar einkompiliert
  src/main/java/…/core/
    TrackerApi.java               HTTP-Client für die Website-API v1
    ListSync.java                 regelmäßiges Abfragen, Status (OK/OFFLINE/GONE), Claims
    TrackedList.java, TrackedItem Datenmodell, Teil-Updates einmischen
    ListView.java                 Filter und Sortierung
    Amounts.java                  Umrechnung Stacks/SB/DK
    TrackerLink.java              erkennt Code vs. Freigabe-Link, Website-Adresse normalisieren
    TrackerConfig.java            Einstellungen (config/schematic-tracker.json)
  src/test/java/…/core/           JUnit-Tests (inkl. Fake-Server für die API)
src/main/java/…/                  gemeinsamer Minecraft-Teil
  SchematicTrackerClient.java     Kern: Config, Tasten, Tick (von den Loadern aufgerufen)
  TrackerHud.java                 das HUD
  TrackerCommands.java            /tracker … (generisch, Rückmeldung über Feedback)
  Texts.java                      Item-Icon/-Name zur Minecraft-ID, Mengen-Text
  screen/                         Einstellungen, ganze Liste, Schieberegler
  compat/                         Versionsunterschiede (Stonecutter-Bedingungen): Gfx, Compat, CompatScreen
  fabric/                         Fabric-Einstieg, Mod-Menu-Anbindung (nur im Fabric-Jar)
  neoforge/                       NeoForge-Einstieg inkl. Config-Screen (nur im NeoForge-Jar)
src/main/resources/               fabric.mod.json, META-INF/neoforge.mods.toml, Sprachdateien, Icon
```

### Versionsunterschiede

Der Code in `src/main/` ist immer für die **aktive Version `26.3-fabric`** geschrieben. Stonecutter kommentiert beim
Bauen die Teile für andere Versionen ein bzw. aus:

```java
//? if >=1.21.6 {
g.pose().pushMatrix();
//?} else {
/*g.pose().pushPose();
*///?}
```

Loader-Unterschiede: `//? if fabric { … }` bzw. `neoforge`; außerdem landet `fabric/` nur im Fabric-Jar und
`neoforge/` nur im NeoForge-Jar. `ResourceLocation` (bis 1.21.10) ↔ `Identifier` ersetzt Stonecutter automatisch.

| Ab Version | Unterschied                                                                    | Stelle                           |
| ---------- | ------------------------------------------------------------------------------ | -------------------------------- |
| 1.21.6     | HUD über `HudElementRegistry` (vorher `HudRenderCallback`), 2D-Matrix, Tooltip | `Gfx`, `fabric/`                 |
| 1.21.9     | Tasten-Kategorien als Objekt, Maus-Events (`MouseButtonEvent`)                 | `Compat`, `CompatScreen`         |
| 1.21.11    | `Identifier` statt `ResourceLocation`, neuer CycleButton-Builder               | Ersetzung, `Compat`              |
| 26.1       | unverschleiert, Java 25, `GuiGraphicsExtractor`, `KeyMappingHelper`            | `Gfx`, `CompatScreen`, `fabric/` |
| 26.2       | aktueller Bildschirm über `Minecraft.gui`                                      | `Compat`                         |

Auf NeoForge ab 1.21.9 die Tasten-Kategorie **nicht** über `KeyMapping.Category.register` anlegen (wirft sonst
„already registered“), sondern über `RegisterKeyMappingsEvent#registerCategory` (siehe `Compat`).

Eine andere Version aktiv schalten (z. B. zum Bearbeiten in der IDE): `./gradlew "Set active project to 1.21.4-fabric"`.
**Vor dem Commit mit `./gradlew "Reset active project"` zurück auf `26.3-fabric`**, sonst ändert sich jede Datei mit Bedingungen.

Neue Minecraft-Version hinzufügen:
1. In `settings.gradle.kts` eintragen (`mc("26.4", "fabric", "neoforge")`), Abschnitte in
   `stonecutter.properties.toml` ergänzen (Fabric API: maven.fabricmc.net, Mod Menu: maven.terraformersmc.com,
   NeoForge: maven.neoforged.net – nur stabile Versionen).
2. `./gradlew :26.4-fabric:build` – Kompilierfehler zeigen, was sich geändert hat; Bedingungen in `compat/` ergänzen.
3. In `.github/workflows/build.yml` und `TESTING.md` eintragen, im Spiel testen.

---

## 5. Wichtige Details

- **Token = Passwort.** Steht nur in `config/schematic-tracker.json`, nie im Chat oder Log, nie in der URL
  (nur Header `X-Share-Token`).
- **HTTP/1.1 erzwingen** (`TrackerApi`): Javas HttpClient versucht bei `http://` sonst ein HTTP/2-Upgrade, das
  z. B. Node-Server abbrechen.
- Befehle, Tasten und HUD meldet die jeweilige Loader-Schicht an; gemeinsamer Code importiert nie Fabric- oder
  NeoForge-Klassen.
- Antworten werden immer über `Minecraft.execute` angewendet, damit HUD und Menüs nie einen halben Stand sehen.
- Unbekannte Item-IDs (z. B. aus neueren Versionen) → kein Icon, englischer Name aus der Liste.
- Befehle, die einen Bildschirm öffnen, merken ihn vor (`openLater`), weil der Chat sonst gleich wieder schließt.
- Bis 1.21.5 können frische Chat-Nachrichten das HUD kurz überdecken (Zeichenreihenfolge von Minecraft).
- Die Mod nutzt die feste API v1 der Website. Ändert sich dort etwas Inkompatibles, kommt eine `/api/v2` –
  die Mod muss dann angepasst werden.

---

## 6. Arbeitsweise und Regeln

- Commits **nur als itsLarss** (`itsLarss <161070259+itsLarss@users.noreply.github.com>`).
- **Keine** KI-/Claude-Nennungen in Commits, PRs, Kommentaren, Doku, Changelogs.
- Commit-Nachrichten auf Deutsch, kurz und sachlich.
- Neue Texte immer in `en_us.json` **und** `de_de.json` (gleiche Schlüssel).
- Neue Logik möglichst in `core/` (ohne Minecraft) mit JUnit-Test.
- Versionsunterschiede nur über Stonecutter-Bedingungen in `compat/` bzw. den Loader-Schichten, kein Copy-Paste.
- Modrinth-Texte (Beschreibung, Changelogs): Englisch, ohne Emojis.
- Veröffentlicht wird **nur auf Modrinth**, nicht auf CurseForge (bewusste Entscheidung).

### Modrinth

- Environment: Client only · Loader: Fabric bzw. NeoForge · Abhängigkeiten (Fabric): Fabric API (required),
  Mod Menu (optional).
- Game Versions pro Jar stehen in `stonecutter.properties.toml` (`mod.mc_releases`) und werden beim Release
  übernommen.
- `./gradlew publishDryRun` legt alle Release-Dateien mit Metadaten unter `versions/*/build/publishMods/` ab,
  **ohne** etwas hochzuladen. Echtes Veröffentlichen ist bewusst nicht eingebaut.

---

## 7. Offene Punkte / Ideen

- **Website-Änderung für die Deadline-Anzeige** (abwärtskompatibel, nur ein neues Feld in API v1):
  `GET /api/v1/list` bekommt `deadlineStatus`, berechnet auf dem Server mit dem vorhandenen `deadlineInfo` und
  `paceBase` (`src/lib/deadline.ts`), `null` wenn keine Deadline:
  `{ "status": "onTrack|tight|behind|done|overdue|unknown", "daysLeft": 57, "forecast": "2026-12-28" | null }`.
  Die Mod liest das Feld schon (`TrackerApi.parseDeadline`); alte Mods ignorieren es.
- **Website: Notizen und Deadline live bringen** (`dev-main` → `main`), sonst sieht die Mod sie nicht.

- **Standard-Website-Adresse** eintragen, sobald die Domain feststeht (dann entfällt `/tracker server`).
- **„Aus Inventar zählen“** (Menge aus dem eigenen Inventar übernehmen) – noch nicht gebaut.
- 26.2 im Spiel testen (bisher nur gebaut).
