# LevelPlugin

A large Minecraft RPG plugin built with Maven. It requires a Spigot 1.19 server and depends on several plugins including MythicMobs, Citizens, Vault, ProtocolLib and PlaceholderAPI.

## Building

1. Ensure Maven is installed.
2. Run `mvn package` in the repository root. The final jar will be located in `target/`.

## Usage

Copy the built jar to your server's `plugins` folder and start the server. The plugin will generate configuration files on first run. Key dependencies listed in `plugin.yml` must also be installed.

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

- `screenmenus.yml` – define on-screen text or item menus. Example:
  ```yml
  menus:
    example:
      start:
        text: "Start"
        x: 0
        y: 0
        command: "say starting"
    item_demo:
      sword:
        item: DIAMOND_SWORD
        x: -0.3
        y: 0.2
        command: "say sword"
  ```
  Use `/cursormenu run <menu>` to show a menu or `/cursormenu items <material>` to spawn a sample item display.

## Features

- Procedural dungeons with custom MythicMob spawns
- Scaled skill damage based on player stats
- Extensive item, quest and economy systems
- Lightweight screen menu system with configurable text and item displays

Contributions are welcome. See `LICENSE` for terms.
