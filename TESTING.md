# Teststatus

Stand der Version `0.2.0` (Umbau auf eine Codebasis, dazu neue Funktionen aus Schritt 3).

- **gebaut** = kompiliert, Jar erzeugt (lokal bzw. in GitHub Actions)
- **im Spiel getestet** = mit `runClient` oder echtem Launcher gestartet und die Checkliste unten durchgeklickt.
  Nur eintragen, wenn das wirklich passiert ist. Kompiliert ist **nicht** getestet.

| Kombination      | gebaut | im Spiel getestet | Anmerkung |
| ---------------- | ------ | ----------------- | --------- |
| 1.21-fabric | ja (lokal) | nein              | gilt für 1.21 und 1.21.1 |
| 1.21-neoforge | ja (lokal) | nein              |           |
| 1.21.1-neoforge | ja (lokal) | nein              |           |
| 1.21.2-fabric | ja (lokal) | nein              |           |
| 1.21.3-fabric | ja (lokal) | nein              |           |
| 1.21.3-neoforge | ja (lokal) | nein              |           |
| 1.21.4-fabric | ja (lokal) | nein              |           |
| 1.21.4-neoforge | ja (lokal) | nein              |           |
| 1.21.5-fabric | ja (lokal) | nein              |           |
| 1.21.5-neoforge | ja (lokal) | nein              | startet bis zum Titelbildschirm ohne Ladefehler (28.10.) |
| 1.21.6-fabric | ja (lokal) | nein              |           |
| 1.21.7-fabric | ja (lokal) | nein              |           |
| 1.21.8-fabric | ja (lokal) | nein              |           |
| 1.21.8-neoforge | ja (lokal) | nein              |           |
| 1.21.9-fabric | ja (lokal) | nein              |           |
| 1.21.10-fabric | ja (lokal) | nein              |           |
| 1.21.10-neoforge | ja (lokal) | nein              |           |
| 1.21.11-fabric | ja (lokal) | nein              |           |
| 1.21.11-neoforge | ja (lokal) | nein              |           |
| 26.1-fabric | ja (lokal) | nein              | gilt für 26.1 – 26.1.2 |
| 26.1-neoforge | ja (lokal) | nein              | nur 26.1.2 |
| 26.2-fabric | ja (lokal) | nein              | auch vor dem Umbau nur gebaut |
| 26.2-neoforge | ja (lokal) | nein              |           |
| 26.3-fabric | ja (lokal) | nein              |           |
| 26.3-neoforge | ja (lokal) | nein              | startet bis zum Titelbildschirm ohne Ladefehler (28.10.) |

Vor dem Umbau (Fabric, 0.0.x/0.1.x) wurden laut bisheriger Doku alle Versionen außer 26.2 im Spiel geprüft. Nach dem
Umbau gilt jede Kombination wieder als ungetestet, bis sie neu geprüft ist.

## Bekannte, behobene Fehler

- 28.10.: Auf **allen NeoForge-Versionen** brach das Laden mit `NullPointerException` ab
  (`SchematicTrackerClient.init`): NeoForge erzeugt die Mod, bevor es `Minecraft` gibt. Behoben, Minecraft wird erst
  beim Aufruf geholt. Gemeldet für 26.1–26.3, 1.21.5, 1.21.8, 1.21.10, 1.21.11; betraf alle NeoForge-Jars.

## Checkliste im Spiel

1. Spiel startet ohne Fehler, Mod erscheint in der Mod-Liste (Fabric: Mod Menu, NeoForge: „Mods“).
2. `/tracker server <Adresse>`, auf der Website Code erzeugen, `/tracker link <Code>` → grüne Meldung.
3. HUD erscheint unten rechts: Icons, Namen in Spielsprache, fehlende Menge, Prozent im Titel.
4. Auf der Website eine Menge ändern → nach wenigen Sekunden im HUD.
5. `J` blendet das HUD aus/ein; im Menü (Esc) und bei F3 ist es weg.
6. `K` öffnet die ganze Liste: Filter und Sortierung umschalten, Scrollen, Tooltip, Klick → „Ich sammle das“
   (erscheint auf der Website), nochmal Klick → freigegeben.
7. Einstellungen: über Mod Menu (Fabric) bzw. Mod-Liste → Konfigurieren (NeoForge), über die Liste („Einstellungen…“)
   und `/tracker config`. Ecke, Zeilen, Größe, Hintergrund ändern → HUD passt sich an. Neustart → Einstellungen bleiben.
8. Spielsprache Deutsch ↔ Englisch: Texte und Item-Namen wechseln.
9. Tastenbelegung: Kategorie „Schematic Tracker“ mit beiden Tasten vorhanden.

### Neu: Mengen eintragen (Feature 1)

10. `K` → Rechtsklick auf ein Material → Zeile wird orange markiert, darunter Name und `Menge / Soll` plus Eingabefeld.
11. `+1 St`, `+1 SB`, `+1 DK` → Menge steigt sofort um 64 / 1728 / 3456 (bei 64er-Items); Meldung unten.
12. Feld: `3sb 5st` + Enter setzt, `+64` addiert, `-1st` zieht ab, Unsinn (`abc`) → rote Meldung.
13. Nach ca. 1 s steht die neue Menge auf der Website (Verlauf zeigt den eigenen Namen).
14. Esc schließt erst das Eingabefeld, zweites Esc die Liste. Linksklick setzt weiter „Ich sammle das“.
15. `/tracker add stone 3sb`, `/tracker set stone 100`, `/tracker add hand 64` (Item in der Hand),
    Tab-Vervollständigung schlägt die IDs der Liste vor. Unbekanntes Item / mehrdeutig → verständliche Meldung.
16. Mit Nur-Ansehen-Link: Rechtsklick tut nichts, Befehle melden „Nur-Ansehen-Link“.

Rückmeldung 28.10.: Feature 1 funktioniert im Spiel (welche Kombinationen, wurde nicht notiert – bitte in der
Tabelle oben nachtragen).

### Neu: Fertig-Meldungen (Feature 2)

17. Einstellungen → „Vorschau“: Toast „Liste fertig!“, Ton und Konfetti über den Bildschirm (ca. 4 s).
18. Ein Material voll eintragen (im Spiel oder auf der Website) → Toast „Material fertig!“ mit Namen + Ton.
19. Mehrere auf einmal fertig (Website: „Alle fertigen als gefarmt markieren“) → **ein** Toast „N Materialien fertig!“.
20. Letztes Material fertig → Toast „Liste fertig!“, Ton, Konfetti (auch bei ausgeblendetem HUD und in der Liste K).
21. Fertig-Meldung / Fertig-Ton / Konfetti einzeln ausschalten → jeweils nur das fällt weg.
22. Beim Verbinden oder Neustart mit schon fertigen Materialien: keine Meldung.

### Neu: Farm-Session (Feature 3)

23. Erster eigener Eintrag (Liste oder `/tracker add`) → im HUD unten eine türkise Zeile
    „⏱ Name: +N (Tempo ab 1 min)“.
24. Nach über einer Minute und weiteren Einträgen: „⏱ Name: X/h · noch ~Y“ – grob plausibel?
25. Anderes Material eintragen → Zeile wechselt auf dieses Material. Material voll → „⏱ Name: fertig“.
26. `/tracker session` zeigt Dauer, Zuwachs, Tempo, Restzeit; `/tracker session start` setzt zurück (Toast),
    `/tracker session stop` blendet die Zeile aus.
27. Steuerung → Tastenbelegung: „Farm-Session starten/neu starten“ (ohne Standardtaste) belegen und drücken → Toast.
28. Einstellungen: „Farm-Session im HUD“ aus → Zeile weg; „Farm-Session neu starten“-Knopf → Toast.

### Neu: Notizen (Feature 4)

Braucht eine Website mit Notizen (Stand `dev-main` der Website, z. B. lokal oder Vercel-Preview) und den
Mitbearbeiten-Link bzw. Kopplungscode – über Nur-Ansehen-Links liefert die Website keine Notizen.

29. Auf der Website Notiz zur Liste und zu einem Material schreiben → nach wenigen Sekunden im HUD: Listen-Notiz
    grau unter dem Titel (gekürzt mit „…“), Material-Notiz grau hinter dem Namen (gekürzt).
30. `K`: Materialien mit Notiz haben ein ✎; Maus darüber → unten „Notiz zu …“ mit vollem Text (bis 3 Zeilen).
    Ohne Maus über einem Material: Notiz zur Liste. Mehrzeilige Notiz wird umgebrochen.
31. Notiz auf der Website löschen → verschwindet im Spiel. Gar keine Notizen → kein Notizbereich in der Liste.
32. Einstellungen „Notizen im HUD“ aus → HUD ohne Notizen, Liste (K) zeigt sie weiter.
33. Live-Website ohne Notizen (aktueller Stand `main`) → keine Fehler, einfach keine Notizen.

### Neu: Wer sammelt was im HUD (Feature 5) und Einstellungen auf zwei Seiten

34. Auf der Website (oder von einem Freund) „Ich sammle das“ für ein Material setzen → im HUD erscheint der Name
    links neben der Menge, farbig (je Name immer dieselbe Farbe). Eigene Claims: gelber Materialname wie bisher,
    kein Name.
35. In der Liste `K` sind die Namen anderer ebenfalls farbig, „du“ bleibt grau.
36. Einstellungen Seite 2: „Wer sammelt was“ aus → keine Namen im HUD; „Namen farbig“ aus → grau.
37. Einstellungen: Seite 1 = Verbindung + Aussehen, „Weitere Einstellungen…“ → Seite 2 (Meldungen, Farm-Session,
    Notizen, Claims), „‹ Zurück“. Eingegebene Website/Name gehen beim Seitenwechsel nicht verloren.
    Mit GUI-Größe 4 ist auf beiden Seiten „Fertig“ sichtbar.
