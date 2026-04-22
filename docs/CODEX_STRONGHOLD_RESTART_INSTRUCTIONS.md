# Codex Instruction: Rebuild Stronghold Branch From a Known-Good Generator

## Goal
Rebuild the Stronghold feature set **without breaking generation stability**. Use this file as the single instruction context when restarting from a commit where Stronghold generation/asset placement still worked.

---

## Non-negotiable guardrails
1. **Do not change generation math and placement constraints in the same step as UX/runtime work.**
2. Keep generator changes isolated and verifiable before adding queue/survival integrations.
3. Prefer reusing existing utility styles (`ChatMessageUtil`, `TooltipUtil`, existing GUI patterns) over ad-hoc formatting.
4. Keep methods generic/reusable when possible; avoid duplicating world checks, score formatting, and queue error text.

---

## Branch feature inventory (what this branch currently contains)

### A) Stronghold runtime (wave survival)
- `StrongholdSurvivalManager` added for:
  - run lifecycle, wave timers/countdowns, objective tracking,
  - score/rank computation,
  - bossbar + world border control,
  - door key item creation/consumption,
  - event handling for mob death/damage/run cleanup,
  - score/objective feedback and reward integration.
- Files:
  - `src/main/java/me/nakilex/levelplugin/debug/StrongholdSurvivalManager.java`

### B) Queue + command + GUI
- Queue modes and matchmaking:
  - `StrongholdQueueMode`
  - `StrongholdQueueManager`
- Player interaction:
  - `StrongholdQueueGUI`
  - `/stronghold` command via `StrongholdCommand`
- Command registration:
  - `CommandRegistry`
  - `plugin.yml`
- Files:
  - `src/main/java/me/nakilex/levelplugin/stronghold/StrongholdQueueMode.java`
  - `src/main/java/me/nakilex/levelplugin/stronghold/StrongholdQueueManager.java`
  - `src/main/java/me/nakilex/levelplugin/stronghold/gui/StrongholdQueueGUI.java`
  - `src/main/java/me/nakilex/levelplugin/stronghold/commands/StrongholdCommand.java`
  - `src/main/java/me/nakilex/levelplugin/utils/registeries/CommandRegistry.java`
  - `src/main/resources/plugin.yml`

### C) Generator + template data extraction
- `StrongholdTemplateData` introduced to hold template/asset coordinate data.
- `StrongholdDebugGenerator` updated to consume extracted data and run detached asset / border forest passes.
- Door hologram/interaction logic integrated with survival key consumption.
- Files:
  - `src/main/java/me/nakilex/levelplugin/debug/StrongholdTemplateData.java`
  - `src/main/java/me/nakilex/levelplugin/debug/StrongholdDebugGenerator.java`

### D) Integrations across systems
- Main/bootstrap manager wiring:
  - `Main`
  - `PluginBootstrap`
- Scoreboard stronghold stage display:
  - `PlayerScoreboardManager`
- Loot chest stronghold behavior:
  - `LootChestManager`
  - `LootChestListener`
  - `LootChestCloseListener`
- Stronghold world helper:
  - `StrongholdWorldUtil`
- Files:
  - `src/main/java/me/nakilex/levelplugin/Main.java`
  - `src/main/java/me/nakilex/levelplugin/core/PluginBootstrap.java`
  - `src/main/java/me/nakilex/levelplugin/scoreboard/PlayerScoreboardManager.java`
  - `src/main/java/me/nakilex/levelplugin/lootchests/managers/LootChestManager.java`
  - `src/main/java/me/nakilex/levelplugin/lootchests/listeners/LootChestListener.java`
  - `src/main/java/me/nakilex/levelplugin/lootchests/listeners/LootChestCloseListener.java`
  - `src/main/java/me/nakilex/levelplugin/utils/StrongholdWorldUtil.java`

### E) Pet and config changes
- Added stronghold-related pet effects to `PetEffectType`.
- Added stronghold pet entries in `pets.yml`.
- Files:
  - `src/main/java/me/nakilex/levelplugin/pet/PetEffectType.java`
  - `src/main/resources/pets.yml`

---

## Known risk area (generation regression)
The fragile area is the **organic border forest + detached asset placement** pipeline in:
- `StrongholdDebugGenerator.schedulePostTeleportEnhancements(...)`
- `StrongholdDebugGenerator.scheduleOrganicBorderForest(...)`

When restarting, lock this workflow first and prove it places border assets before layering runtime/UI changes.

---

## Recommended restart order (strict)
1. **Baseline generator only**
   - Start from known-good generation commit.
   - Add `StrongholdTemplateData` extraction only.
   - Verify generation parity.
2. **Detached assets + border forest pass**
   - Add only asset scheduling logic.
   - Verify border trees/rocks placement visually and via debug counters.
3. **Door interaction + key behavior**
4. **Survival runtime (`StrongholdSurvivalManager`)**
5. **Queue + GUI + command**
6. **Integrations (scoreboard/chest/pets/bootstrap)**
7. **Message/UI polish last**

---

## Acceptance checklist for Codex (copy/paste)
- [ ] Generator builds and creates stronghold layout without border asset regression.
- [ ] Border forest pass places non-zero assets on a typical generation run.
- [ ] Door interactions work (closed→open template swap, key consumed exactly once).
- [ ] Wave lifecycle stable (no duplicate complete messages, no stale countdown loops).
- [ ] Scoreboard shows wave/mobs/time/objective correctly during active run.
- [ ] Queue GUI supports solo/duo/squad and has clear info/error UX.
- [ ] Loot chests in stronghold worlds obey stronghold-specific claim behavior.
- [ ] Stronghold world checks are centralized via reusable helper(s).
- [ ] Pet effects and config entries load without enum/config mismatch.

---

## Notes for Codex execution
- Keep commits small and topic-scoped (generator, runtime, queue, integrations, polish).
- If border assets fail, stop and debug generator immediately; do not continue stacking features.
- Prefer existing utility patterns for messaging and lore formatting.
