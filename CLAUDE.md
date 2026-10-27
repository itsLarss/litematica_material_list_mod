# Schematic Tracker – Minecraft-Mod (Fabric)

Projektwissen zum Nachlesen: was die Mod kann, wie sie aufgebaut ist, wie man sie baut und testet und woran man
beim Weiterbauen denken muss. Gilt auch als Anleitung für Claude Code, wenn du lokal damit arbeitest.

Die Website liegt im Repo `itsLarss/Litematica_Material_List_Webapp` (eigene `CLAUDE.md`, API-Doku in
`docs/api-v1.md`).

---

## 1. Was ist das?

Eine reine **Client-Mod** für Fabric. Sie zeigt die Materialliste von der Schematic-Tracker-Website als kleines HUD
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
- **Einstellungen** (Mod Menu, `/tracker config` oder „Einstellungen…“ in der Liste): Website, Verbinden/Trennen,
  HUD an/aus, Filter, Sortierung, Einheit (Stacks/SB/DK), Ecke, Zeilen, Größe, Hintergrund, in Menüs ausblenden,
  eigener Name für „Meine“.
- **Befehle:** `/tracker` (Status), `link`, `unlink`, `server`, `status`, `refresh`, `hud [on|off]`,
  `filter <open|mine|all>`, `sort <missing|name|list>`, `list`, `config`.
- **Sprachen:** Englisch und Deutsch (`en_us.json`, `de_de.json`), richtet sich nach der Spielsprache.

---

## 2. Branches und Versionen

`main` ist **immer die neueste Minecraft-Reihe**. Ältere Reihen haben eigene Branches. Die Reihen haben
unterschiedliche Build-Setups – **niemals ineinander mergen**.

| Branch        | Minecraft | Wofür                                              |
| ------------- | --------- | -------------------------------------------------- |
| `main`        | 26.x      | Release der neuesten Reihe                         |
| `dev-26.x`    | 26.x      | Entwicklung 26.x → PR nach `main`                  |
| `dev-main`    | 26.x      | laufende Arbeit → PR nach `dev-26.x`               |
| `main-1.21.x` | 1.21.x    | Release der 1.21-Reihe                             |
| `dev-1.21.x`  | 1.21.x    | Entwicklung 1.21.x → PR nach `main-1.21.x`         |

Pull Requests: **base = Ziel**, **compare = Quelle**. Wege: `dev-main → dev-26.x → main` und
`dev-1.21.x → main-1.21.x`. Der GitHub-Hinweis „ahead of main“ bei den 1.21-Branches ist gewollt und kann
ignoriert werden.

### Versionsnummern

Ein Jar pro Minecraft-Version, Name `schematic-tracker-<modversion>.jar`:

| Minecraft      | Mod-Version     |
| -------------- | --------------- |
| 1.21 – 1.21.1  | `0.0.1-1.21`    |
| 1.21.2 … 1.21.11 | `0.0.<x>-1.21.<x>` (z. B. `0.0.9-1.21.9`) |
| 26.1 – 26.1.2  | `0.1.1-26.1`    |
| 26.2           | `0.1.2-26.2`    |
| 26.3           | `0.1.3-26.3`    |

Die Nummer steht in `versions/<minecraft>.properties` (`mod_version`). Bei einem Update der Mod den vorderen Teil
hochzählen und in **allen** Versionsdateien der Reihe anpassen.

---

## 3. Bauen

### Voraussetzungen

- **JDK 25** zum Ausführen von Gradle (Loom 1.19 braucht es) – auch für den 1.21-Branch.
  Die 1.21-Jars werden trotzdem für Java 21 gebaut (`release = 21`), die 26.x-Jars für Java 25.
- Internet beim ersten Build (Minecraft, Fabric, Mod Menu werden geladen).

### Befehle

```bash
./gradlew build                 # Standardversion (26.x-Branches: 26.3, 1.21-Branches: 1.21.11)
./gradlew build -Pmc=26.1       # bestimmte Version, Liste siehe versions/
./gradlew build -Pmc=1.21.4
./gradlew test                  # nur die Unit-Tests
./gradlew runClient -Pmc=26.3   # Minecraft mit der Mod starten (eigener Ordner run/ bzw. run-<version>/)
```

Das fertige Jar liegt in `build/libs/` (die Datei **ohne** `-sources`).

### Jars von GitHub holen

Jeder Push baut alle Versionen per GitHub Action (`.github/workflows/build.yml`).
**Actions → oberster Lauf → ganz unten „Artifacts“** – pro Version eine ZIP mit dem Jar (Login nötig, 90 Tage).

### Testen im Spiel

1. Website lokal oder live laufen lassen (siehe Website-`CLAUDE.md`).
2. `./gradlew runClient -Pmc=<version>`, Welt anlegen.
3. `/tracker server http://localhost:3000` (bzw. die echte Adresse), auf der Website Code erzeugen,
   `/tracker link <Code>`.
4. Prüfen: HUD erscheint, Änderung auf der Website taucht nach ein paar Sekunden auf, `K` öffnet die Liste,
   Klick setzt „Ich sammle das“ (erscheint im Verlauf der Website).

Im Entwicklungs-Client heißt der Spieler jedes Mal anders (`Player123`), deshalb ist „Meine“ dort meist leer.

---

## 4. Aufbau

```
build.gradle, gradle.properties   Build; Zielversion über -Pmc=…
versions/<mc>.properties          pro Minecraft-Version: Minecraft, Fabric API, Mod Menu, Versionsbereich, Mod-Version
src/main/java/…/core/             reine Java-Logik ohne Minecraft (testbar)
  TrackerApi.java                 HTTP-Client für die Website-API v1
  ListSync.java                   regelmäßiges Abfragen, Status (OK/OFFLINE/GONE), Claims
  TrackedList.java, TrackedItem   Datenmodell, Teil-Updates einmischen
  ListView.java                   Filter und Sortierung
  Amounts.java                    Umrechnung Stacks/SB/DK
  TrackerLink.java                erkennt Code vs. Freigabe-Link, Website-Adresse normalisieren
  TrackerConfig.java              Einstellungen (config/schematic-tracker.json)
src/client/java/…/                Minecraft-Teil
  SchematicTrackerClient.java     Einstiegspunkt: Config, Tasten, HUD, Befehle, Tick
  TrackerHud.java                 das HUD
  TrackerCommands.java            /tracker …
  Texts.java                      Item-Icon/-Name zur Minecraft-ID, Mengen-Text
  screen/                         Einstellungen, ganze Liste, Schieberegler
  modmenu/                        Mod-Menu-Anbindung (optional)
src/compat/<gruppe>/java/…/compat Code, der sich zwischen Minecraft-Versionen unterscheidet
src/main/resources/               fabric.mod.json, Sprachdateien, Icon
src/test/java/…/core/             JUnit-Tests (inkl. Fake-Server für die API)
```

### Versionsunterschiede (`src/compat/`)

Gemeinsamer Code ruft nur `Compat.*` bzw. erbt von `CompatScreen`; welche Gruppe gebaut wird, entscheidet
`build.gradle` anhand von `-Pmc`.

**26.x-Branches** (Mojang-Namen ohne Verschleierung, Plugin `net.fabricmc.fabric-loom`, Java 25):

| Gruppe | Versionen     | Unterschied                                        |
| ------ | ------------- | -------------------------------------------------- |
| `26.1` | 26.1 – 26.1.2 | aktueller Bildschirm über `Minecraft.screen`       |
| `26.2` | 26.2, 26.3    | aktueller Bildschirm über `Minecraft.gui.screen()` |

**1.21-Branches** (verschleiert, Plugin `fabric-loom` mit Mojang-Mappings, Java 21):

| Gruppe    | Versionen        | Unterschied                                                      |
| --------- | ---------------- | ---------------------------------------------------------------- |
| `1.21`    | 1.21 – 1.21.5    | HUD über `HudRenderCallback`, `PoseStack`, `renderTooltip`       |
| `1.21.6`  | 1.21.6 – 1.21.8  | neues GUI-Rendering: `HudElementRegistry`, 2D-Matrix             |
| `1.21.9`  | 1.21.9 – 1.21.10 | Tasten-Kategorien, Maus-Events (`MouseButtonEvent`)              |
| `1.21.11` | 1.21.11          | `ResourceLocation` heißt `Identifier`, neuer CycleButton-Builder |

Neue Minecraft-Version hinzufügen:
1. `versions/<mc>.properties` anlegen (Fabric-API-Version: maven.fabricmc.net, Mod Menu: maven.terraformersmc.com).
2. `./gradlew build -Pmc=<mc>` – Kompilierfehler zeigen, was sich geändert hat.
3. Wenn nötig neue Compat-Gruppe anlegen und in `build.gradle` zuordnen.
4. Im Spiel testen (Abschnitt 3), Version in `.github/workflows/build.yml` ergänzen.

---

## 5. Wichtige Details

- **Token = Passwort.** Steht nur in `config/schematic-tracker.json`, nie im Chat oder Log, nie in der URL
  (nur Header `X-Share-Token`).
- **HTTP/1.1 erzwingen** (`TrackerApi`): Javas HttpClient versucht bei `http://` sonst ein HTTP/2-Upgrade, das
  z. B. Node-Server abbrechen.
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
- Änderungen an einer Reihe **getrennt** in die andere übernehmen (Code kopieren und anpassen, nicht mergen).
- Modrinth-Texte (Beschreibung, Changelogs): Englisch, ohne Emojis.

### Modrinth

- Environment: Client only · Loader: Fabric · Abhängigkeiten: Fabric API (required), Mod Menu (optional).
- Game Versions pro Jar wie in der Tabelle in Abschnitt 2 (beim 1.21-Jar 1.21 **und** 1.21.1 anhaken,
  beim 26.1-Jar 26.1, 26.1.1 und 26.1.2).

---

## 7. Offene Punkte / Ideen

- **Standard-Website-Adresse** eintragen, sobald die Domain feststeht (dann entfällt `/tracker server`).
- **Mengen in der Mod eintragen:** Zahlenfeld mit Schnelleingabe und Knöpfen (+1 Stack/SB/DK), „Aus Inventar zählen“,
  `/tracker add <item> <menge>` / `/tracker set …`. Die Website-API kann das schon (`PATCH /api/v1/list/items`).
- 26.2 im Spiel testen (bisher nur gebaut).
