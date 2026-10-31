# Schematic Tracker – Minecraft mod

Client mod for Fabric and NeoForge for the Schematic Tracker website. It shows your material list as a small HUD in the corner,
in the style of the Litematica material list, and keeps it in sync with the website in both directions.

- **Connect** with a short code from the website (*Share → Minecraft mod → Create code*) or with a share link.
- **HUD** in a corner of your choice: item icon, name in your game language, and what's still missing
  (`70417 (40.75 SB)`), sorted by most missing. Shows only as many rows as you set.
- **Live:** changes from the website or from friends appear within a few seconds.
- **Full list** (default key `K`): scroll through everything, click a material to claim it ("I'll collect this")
  or release it. The website shows it right away.
- **Enter amounts in game:** right-click a material in the full list, then type an amount (same quick input as on
  the website: `3sb 5st`, `2dc`, `+64`, `-1st`) or use the *+1 st / SB / DC* buttons. Or by command:
  `/tracker add <item> <amount>` adds, `/tracker set <item> <amount>` corrects. `hand` stands for the item in
  your hand. Changes are saved after a short pause and show up on the website like any other edit.
- **Done notifications:** a toast and a sound when a material is complete (yours or a friend's), and confetti when
  the whole list is done. Each can be turned off in the settings; *Preview* shows how it looks.
- **Farm session:** counts what you enter in the mod and shows items per hour and the estimated time left for the
  material you're farming right now (an extra line in the HUD). Starts with your first entry; restart it with
  `/tracker session start`, a key (unbound by default, see Controls) or the settings.
- **Notes:** notes from the website (on the list and on materials, e.g. coordinates) show up short in the HUD and in
  full in the list (`K`, hover a material). Read only; only with a co-edit connection, never via view-only links.
- **Who collects what:** other players' claims appear next to the amount in the HUD, each name in its own color
  (can be turned off or shown in gray).
- **Deadline:** the list's target date from the website in the HUD and the full list, with "on track / tight /
  behind" once the website provides it. The mod only displays what the website calculates.
- **Presets:** *Minimal*, *Clear* (default) and *Everything* on the second settings page or with
  `/tracker config preset <minimal|clear|all>`. Single options stay changeable afterwards.
- **Settings** (two pages) via Mod Menu, the NeoForge mod list, the full list (*Settings…*) or `/tracker config`: filter (open / mine / all), sort,
  unit (stacks / shulker boxes / double chests), position, rows, size, background, hide in menus, your name,
  done message, sound and confetti.
- English and German, following the game language.
- Client only; works on any server. Doesn't need Litematica.

## Versions

One jar per Minecraft version and loader, named `schematic-tracker-<mod version>+<minecraft>-<loader>.jar`,
for example `schematic-tracker-0.2.0+26.3-fabric.jar`.

| Minecraft      | Fabric | NeoForge | Java |
| -------------- | ------ | -------- | ---- |
| 26.1 – 26.1.2  | ✓ (one jar for all three) | ✓ (26.1.2 only) | 25 |
| 26.2, 26.3     | ✓ | ✓ | 25 |
| 1.21 – 1.21.1  | ✓ (one jar for both) | ✓ (one jar each) | 21 |
| 1.21.2 … 1.21.11 | ✓ (one jar each) | ✓ for 1.21.3, 1.21.4, 1.21.5, 1.21.8, 1.21.10, 1.21.11 | 21 |

NeoForge is only built where NeoForge has a stable release (not for 1.21.2, 1.21.6, 1.21.7 and 1.21.9).
Fabric needs Fabric API; Mod Menu is optional. On NeoForge the settings open from the mod list.

Older releases (up to `0.1.3`) used the scheme `0.0.<x>-1.21.<x>` and `0.1.<x>-26.<x>` and were Fabric only.

## Usage

1. Install the jar matching your Minecraft version and loader into `mods/` (on Fabric also Fabric API).
2. On [schematictracker.app](https://schematictracker.app), open your list → *Share* → *Minecraft mod* → *Create code*.
3. In game: `/tracker link K7QM-4XPA`. Done – the HUD appears bottom right.

The website `https://schematictracker.app` is preset. Only if you run your own instance, change it with
`/tracker server https://<your website>` or in the settings (an empty field goes back to the default).

Instead of a code you can also paste a share link: `/tracker link https://<website>/s/<token>`
(a view-only link shows the list but can't change anything).

### Commands

| Command                              | What it does                              |
| ------------------------------------ | ----------------------------------------- |
| `/tracker`                           | Status                                    |
| `/tracker link <code or link>`       | Connect a list                            |
| `/tracker unlink`                    | Disconnect                                |
| `/tracker server <address>`          | Set the website                           |
| `/tracker refresh`                   | Load now                                  |
| `/tracker hud [on\|off]`             | Show/hide the HUD (default key `J`)       |
| `/tracker filter <open\|mine\|all>`  | What the HUD shows                        |
| `/tracker sort <missing\|name\|list>`| Order                                     |
| `/tracker add <item> <amount>`       | Add to the amount (`/tracker add stone 3sb`, `/tracker add hand 64`) |
| `/tracker set <item> <amount>`       | Correct the amount (`/tracker set glass 2dc 5st`) |
| `/tracker session [start\|stop]`     | Farm session: rate and time left, restart or stop |
| `/tracker list`                      | Full list (default key `K`)               |
| `/tracker config`                    | Settings                                  |
| `/tracker config preset <minimal\|clear\|all>` | Apply a preset                 |

"Mine" compares the list's "collecting" name with your name from the settings (default: your Minecraft name),
so use the same name on the website.

### Privacy

The mod only talks to the website you set. The connection token is stored in
`config/schematic-tracker.json` and is never shown in chat or logs; treat that file like a password.
Polling happens every 2.5 s while the HUD or list is visible (every 10 s after two quiet minutes) and stops
while the game is paused.

## Branches

All Minecraft versions and both loaders live in one code base.

| Branch     | Purpose                                     |
| ---------- | ------------------------------------------- |
| `main`     | Releases                                    |
| `dev-main` | Development → pull request into `main`      |

## Building

Needs JDK 25 to run Gradle (the 1.21 jars are still built for Java 21).

```bash
./gradlew buildAll                         # tests + every version and loader → build/libs/<mod version>/
./gradlew :26.3-fabric:buildAndCollect     # one combination → build/libs/<mod version>/
./gradlew :1.21.4-neoforge:build           # one combination → versions/1.21.4-neoforge/build/libs/
./gradlew :core:test                       # unit tests only
./gradlew :26.3-fabric:runClient           # test client (folder run/26.3-fabric/)
./gradlew runClients -Pclients=fabric      # several test clients at once (all | fabric | neoforge |
                                           #   1.21.4 | 26.3-neoforge, comma separated)
```

Releases go to Modrinth through the *Release* GitHub workflow (see `CLAUDE.md`).

The build uses [Stonecutter](https://stonecutter.kikugie.dev/): every combination is a subproject in
`versions/<minecraft>-<loader>/`, and all of them share the sources in `src/main/`.

- `core/` – plain Java without Minecraft (website API, sync, filter, amounts) with its own tests.
- `src/main/java/…/compat/` – the few places that differ between Minecraft versions, written as
  Stonecutter conditions (`//? if >=1.21.6 { … }`). The rest of the code only calls these.
- `src/main/java/…/fabric/` and `…/neoforge/` – thin loader layers (entry point, keys, HUD, commands, settings).
- `stonecutter.properties.toml` – versions of Minecraft, Fabric API, NeoForge, Mod Menu and the supported ranges.

The code in `src/main/` is always written for the active version (`26.3-fabric`); Stonecutter comments out the
parts for other versions. To work on another version, switch with `./gradlew "Set active project to 1.21.4-fabric"`
and switch back with `./gradlew "Reset active project"` before committing.

The website API is documented in the website repo (`docs/api-v1.md`).

## License

GPL-3.0, see [LICENSE](LICENSE).
