# Stage Progression Systems

This document summarises the gameplay loops implemented for the stage-based progression features and points to the relevant configuration entry-points inside the codebase. Each section covers the gameplay loop, how it ties into existing systems, and which definitions to tweak when adjusting balance or requirements.

## Frontier Rifts
- **Gameplay:** Guild parties queue rift stages through `/rift`. Runs consume dungeon layouts such as `ember_chamber` and rotate daily mutators. Success awards guild coins/XP, battle pass progress, and quest credit.
- **Integration:** Utilises `DungeonManager` instancing, party membership, guild quest hooks, and siege ownership checks. Timeouts feed back into guild progression data for decay tracking.
- **Configuration:** Default definitions live in `FrontierRiftManager#registerDefaults` (`src/main/java/me/nakilex/levelplugin/dungeon/rift/FrontierRiftManager.java`). Edit or add `FrontierRiftDefinition` entries to adjust stages, rewards, recommended power text, or mutator tables.
- **UX:** GUI logic in `dungeon/rift/gui/FrontierRiftBoard.java` follows `GuiBuilder` and `TooltipUtil` conventions for progress display and click affordances.

## Life-Skill Supply Chains
- **Gameplay:** Guild members deposit gathered materials via `/supplychain` to power multi-stage production orders that reward guild XP/coins and timed buffs.
- **Integration:** Reads environment building stages, consumes guild progression storage, advances guild quest objectives, and grants battle pass XP. Production timers reuse the shared scheduler tick.
- **Configuration:** Recipes are defined in `SupplyChainManager#registerDefaults` (`src/main/java/me/nakilex/levelplugin/environment/supply/SupplyChainManager.java`). Each `SupplyChainDefinition` specifies required buildings, stage recipes, rewards, and production durations.
- **UX:** Inventory layout resides in `environment/supply/gui/SupplyChainBoard.java`, relying on `TooltipUtil.progressBar` for per-item tracking.

## Arcane Trials
- **Gameplay:** Individual players open `/trial` to attempt solo dungeon challenges scaled by tier. Clearing tiers grants “marks,” unlocks prestige bonuses, and deposits battle pass progress.
- **Integration:** Leverages dungeon instancing, player settlement data (`PlayerConfig`), environment building requirements (sanctum shrine), and guild quests for participation credit.
- **Configuration:** Trial definitions are in `ArcaneTrialManager#registerDefaults` (`src/main/java/me/nakilex/levelplugin/dungeon/trial/ArcaneTrialManager.java`). Adjust tiers by editing `ArcaneTrialDefinition` builder calls (layout, rewards, recommended level, time limits, mark payouts).
- **UX:** GUI in `dungeon/trial/gui/ArcaneTrialBoard.java` uses standard tooltip helpers for tier previews and prestige instructions.

## Expedition Relic Reliquary
- **Gameplay:** Guilds with siege ownership invest treasury coins via `/expedition` to fill a shared progress bar. Once full, the guild leader (or party leader) launches a dungeon expedition; completing it unlocks a timed relic buff. Members can extend relic uptime by depositing upkeep materials.
- **Integration:** Connects siege ownership (`GuildSiegeManager`), environment building stages for gating access, guild progression storage, dungeon instancing, battle pass progress, mercenary bonuses, and guild quest objectives. Daily rotation (via `CalendarManager`) expires relics when timers elapse.
- **Configuration:** Relic definitions are registered in `ExpeditionRelicManager#registerDefaults` (`src/main/java/me/nakilex/levelplugin/guild/expedition/ExpeditionRelicManager.java`). Each `ExpeditionRelicDefinition` controls:
  - `layoutKey`, `progressRequired`, `investmentCost`, and `progressPerInvestment` for the filling loop.
  - Guild rewards, battle pass progress, active duration, required building stage, and upkeep material/bundle values.
  - Time limits for the expedition run and effect description strings shown in the GUI.
  Adjust or add definitions to expand the relic deck without altering persistence logic.
- **UX:** `guild/expedition/gui/ExpeditionRelicBoard.java` renders the reliquary board with consistent filler panes, progress bars, and click instructions aligned to `TooltipUtil` and `ChatFormatter` styling.

## General Notes
- All systems persist data under the guild progression configuration section (`Guild#getProgressionData()`). The structures are intentionally generic so new managers can share the same root without conflicting keys.
- When adding new GUI actions, prefer `GuiBuilder` for inventory scaffolding and `TooltipUtil` for progress bars or click legends to keep the look and feel consistent.
- Calendar-driven systems expect daily rotation through `CalendarManager.advanceDay()`. Any additional stage loop should expose a `rotateDaily(long epochDay)` hook and register it alongside the existing managers.
