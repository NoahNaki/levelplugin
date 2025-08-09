# LevelPlugin

A large Minecraft RPG plugin built with Maven. It requires a Spigot 1.19 server and depends on several plugins including MythicMobs, Citizens, Vault, ProtocolLib and PlaceholderAPI.

## Building

1. Ensure Maven is installed.
2. Run `mvn package` in the repository root. The final jar will be located in `target/`.

## Usage

Copy the built jar to your server's `plugins` folder and start the server. The plugin will generate configuration files on first run. Key dependencies listed in `plugin.yml` must also be installed. LevelPlugin will disable itself at startup if required plugins such as BetterHud are missing or not enabled.

### Configuration Highlights

- `config.yml` – general settings. `debug.mythic-skill-damage` enables verbose logging for MythicMob skill damage scaling.
- `dungeon_mobs.yml` – per-room dungeon mob settings. Example:
  ```yml
  rooms:
    sample_room:
      mob: SampleMob
      count: 5
      hp: 40.0
      damage: 6.0
      move-speed: 0.25
      attack-speed: 4.0
  ```
  Rooms are referenced by the mob key used in dungeon layouts.

## Features

- Procedural dungeons with custom MythicMob spawns
- Scaled skill damage based on player stats
- Extensive item, quest and economy systems

## Cursor Menu Example

The cursor menu framework loads menu definitions from
`plugins/LevelPlugin/menus/`. On first run an example file is generated at
`plugins/LevelPlugin/menus/example.yml`:

```yml
id: example
title: "Example Menu"
```

Run `/cursormenu run example` in game to open the sample menu and `/cursormenu stop`
to close it. If the command reports the plugin as disabled, verify BetterHud is
installed and active.

Contributions are welcome. See `LICENSE` for terms.
