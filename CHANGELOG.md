## 0.2.0

- NeoForge support (where NeoForge has a stable release). On NeoForge the settings open from the mod list.
- One release for all supported Minecraft versions: 1.21 to 1.21.11 and 26.1 to 26.3.
- New file names: `schematic-tracker-<version>+<minecraft>-<loader>.jar`.
- Enter amounts in game: right-click a material in the full list (quick input like `3sb 5st`, `+64`, or the
  +1 stack / shulker box / double chest buttons), or use `/tracker add <item> <amount>` and
  `/tracker set <item> <amount>` (`hand` = the item in your hand).
- Done notifications: a toast and a sound when a material is complete, confetti when the whole list is done.
  Each can be turned off in the settings.
- Your settings in `config/schematic-tracker.json` are kept.
