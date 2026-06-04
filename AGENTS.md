# AGENTS.md

## Project Overview

LevelPlugin is a Paper/Spigot-style Minecraft plugin. Java code lives under `src/main/java/me/nakilex/levelplugin`, and plugin resources live under `src/main/resources`.

The project uses Maven and Java 21.

## Build

- Build the plugin with `mvn clean package`.
- The deployable JAR is `target/levelplugin.jar`.
- For local remote-server testing, use `scripts\deploy-dev.bat` after configuring `scripts\deploy-config.ps1`.

## Development Workflow

Before modifying or creating anything, inspect the repository for similar systems, helpers and patterns. Avoid creating new functions or classes that duplicate existing behavior.

If a new function is needed and an existing function does something similar, prefer making the existing method more generic so it can be reused in multiple scenarios.

Keep changes minimal and directly related to the requested task. Do not commit changes unless explicitly asked.

## Reuse and UX

Use the existing utility classes before creating new formatting or UI code:

- `ChatMessageUtil` for player-facing chat messages.
- `ChatUtil` / `ChatFormatter` for shared chat formatting.
- `TooltipUtil` for tooltip/lore UX.
- Shared GUI widget classes under `utils/gui/widgets`.

For gameplay systems, look for existing managers, registries and utility classes before adding parallel systems.

## Docs

Relevant docs live under `docs/`:

- `docs/deployment.md` — one-command build/upload/restart workflow.
- `docs/CHAT_STYLE.md` — chat message styling.
- `docs/gui.md` — shared GUI widget framework.
- `docs/spells.md` — spell system architecture.
- `docs/custom_mob_authoring_checklist.md` — custom mob YAML/spell checklist.
- `docs/QUEST_TEMPLATE.md` — quest definition template.
