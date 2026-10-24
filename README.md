# Schematic Tracker – Minecraft mod

Fabric client mod for the Schematic Tracker website. It shows your material list as a small HUD in the corner,
in the style of the Litematica material list, and keeps it in sync with the website in both directions.

- **Connect** with a short code from the website (*Share → Minecraft mod → Create code*) or with a share link.
- **HUD** in a corner of your choice: item icon, name in your game language, and what's still missing
  (`70417 (40.75 SB)`), sorted by most missing. Shows only as many rows as you set.
- **Live:** changes from the website or from friends appear within a few seconds.
- **Full list** (default key `K`): scroll through everything, click a material to claim it ("I'll collect this")
  or release it. The website shows it right away.
- **Settings** via Mod Menu, the full list (*Settings…*) or `/tracker config`: filter (open / mine / all), sort,
  unit (stacks / shulker boxes / double chests), position, rows, size, background, hide in menus, your name.
- English and German, following the game language.
- Client only; works on any server. Doesn't need Litematica.

## Versions

This branch covers Minecraft 1.21.x. Every Minecraft version has its own jar:

| Minecraft      | Mod version | Jar                                  |
| -------------- | ----------- | ------------------------------------ |
| 1.21 – 1.21.1  | 0.0.1       | `schematic-tracker-0.0.1-1.21.jar`   |
| 1.21.2         | 0.0.2       | `schematic-tracker-0.0.2-1.21.2.jar` |
| 1.21.3         | 0.0.3       | `schematic-tracker-0.0.3-1.21.3.jar` |
| 1.21.4         | 0.0.4       | `schematic-tracker-0.0.4-1.21.4.jar` |
| 1.21.5         | 0.0.5       | `schematic-tracker-0.0.5-1.21.5.jar` |
| 1.21.6         | 0.0.6       | `schematic-tracker-0.0.6-1.21.6.jar` |
| 1.21.7         | 0.0.7       | `schematic-tracker-0.0.7-1.21.7.jar` |
| 1.21.8         | 0.0.8       | `schematic-tracker-0.0.8-1.21.8.jar` |
| 1.21.9         | 0.0.9       | `schematic-tracker-0.0.9-1.21.9.jar` |
| 1.21.10        | 0.0.10      | `schematic-tracker-0.0.10-1.21.10.jar` |
| 1.21.11        | 0.0.11      | `schematic-tracker-0.0.11-1.21.11.jar` |

Requires Fabric Loader ≥ 0.16, Fabric API and Java 21. Mod Menu is optional.
Minecraft 26.x lives on the `dev-26.x` branch.

## Usage

1. Install the jar matching your Minecraft version (plus Fabric API) into `mods/`.
2. Set the website once: `/tracker server https://<your website>` (or in the settings).
3. On the website, open your list → *Share* → *Minecraft mod* → *Create code*.
4. In game: `/tracker link K7QM-4XPA`. Done – the HUD appears bottom right.

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
| `/tracker list`                      | Full list (default key `K`)               |
| `/tracker config`                    | Settings                                  |

"Mine" compares the list's "collecting" name with your name from the settings (default: your Minecraft name),
so use the same name on the website.

### Privacy

The mod only talks to the website you set. The connection token is stored in
`config/schematic-tracker.json` and is never shown in chat or logs; treat that file like a password.
Polling happens every 2.5 s while the HUD or list is visible (every 10 s after two quiet minutes) and stops
while the game is paused.

## Branches

`main` always holds the newest Minecraft version. Older Minecraft versions get their own branches.
Never merge branches of different Minecraft lines into each other – each line has its own build setup.

| Branch        | Minecraft | Purpose                                   |
| ------------- | --------- | ----------------------------------------- |
| `main`        | 26.x      | Release of the newest Minecraft line      |
| `dev-26.x`    | 26.x      | Development for 26.x → merge into `main`  |
| `dev-main`    | 26.x      | Work in progress → merge into `dev-26.x`  |
| `main-1.21.x` | 1.21.x    | Release of the 1.21 line                  |
| `dev-1.21.x`  | 1.21.x    | Development for 1.21.x → merge into `main-1.21.x` |

Pull requests always go from the work branch (compare) into the next branch of the **same** line (base):
`dev-main → dev-26.x → main` and `dev-1.21.x → main-1.21.x`.

When a new Minecraft line becomes the newest, its branches take over `main`, and the previous line moves to its
own `main-<version>` branch.

## Building

```bash
./gradlew build                  # Minecraft 1.21.11
./gradlew build -Pmc=1.21.4      # any other 1.21.x (see versions/)
./gradlew runClient -Pmc=1.21.4  # test client
```

Each Minecraft version has a file in `versions/` (Minecraft, Fabric API, Mod Menu, supported range, mod
version). Code that differs between versions lives in `src/compat/<group>/`; within a group the game API is the
same:

| Group     | Versions          | Differences                                                       |
| --------- | ----------------- | ----------------------------------------------------------------- |
| `1.21`    | 1.21 – 1.21.5     | HUD via `HudRenderCallback`, `PoseStack`, `renderTooltip`         |
| `1.21.6`  | 1.21.6 – 1.21.8   | new GUI rendering: `HudElementRegistry`, 2D matrix stack          |
| `1.21.9`  | 1.21.9 – 1.21.10  | key binding categories, mouse input events                        |
| `1.21.11` | 1.21.11           | `ResourceLocation` renamed to `Identifier`, new cycle button API  |

The website API is documented in the website repo (`docs/api-v1.md`).

## License

GPL-3.0, see [LICENSE](LICENSE).
