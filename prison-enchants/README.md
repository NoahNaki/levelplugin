# PrisonEnchants

Standalone Paper 1.21.8 companion plugin that supplies four cinematic EdPrison pickaxe enchants:

- **Tornado** lifts mined blocks into a rotating vortex.
- **Black Hole** turns mined blocks into displays and pulls them into a void core.
- **Meteor Shower** launches several small, independent mine impacts.
- **Acid Rain** melts exposed mine blocks downward by column.

Every affected block is restricted to a matching WorldGuard mine region and is passed to Prison's `ExplosiveBlockBreakEvent`, preserving Prison pickup, autosell, Fortune, statistics, and mine-reset behavior. EdPrison's `pickaxeblocks` currency is credited for the extra blocks.

## Build

```powershell
mvn clean package
```

The output is `target/PrisonEnchants.jar`. The server needs EdPrison and WorldGuard; Prison is strongly recommended for reward processing.

## Configuration

Runtime limits, durations, radii, block counts, the mine-region pattern, and reward integration are in `plugins/PrisonEnchants/config.yml`. Defaults are intentionally capped to protect server tick time.

EdPrison invokes the internal command configured in each enchant action:

```text
prisonenchants trigger <effect> <player> <world> <x> <y> <z>
```

Administrators can aim at a block inside a mine and preview an effect with:

```text
/prisonenchants test <tornado|blackhole|meteors|acidrain> [player]
```

Use `/prisonenchants reload` after changing the companion plugin's config. Restart the server after changing EdPrison's enchant definitions.
