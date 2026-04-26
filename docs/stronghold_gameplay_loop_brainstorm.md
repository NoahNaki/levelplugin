# Stronghold Gameplay Loop Brainstorm (Implementation-Oriented)

This document turns current Stronghold ideas into a concrete loop and a reusable implementation plan that fits the existing plugin architecture.

## 1) North-star run loop

### Core objective
- Survive and clear escalating combat **waves** while exploring a generated Stronghold.
- Build power during the run through **boons** and **elite drops**.
- Complete optional side events (Shrines) for burst rewards.
- Finish with a score/rank that drives end-of-run rewards.

### Proposed run sequence
1. **Run Start**
   - Spawn in Stronghold lobby/start room.
   - Show run objective via `ChatMessageUtil` + optional title/action bar.
2. **Combat Progression**
   - Waves spawn from configured pools.
   - Every 5 waves can include enhanced pressure rules.
   - **Wave 15:** miniboss.
   - **Wave 30:** boss (or run-complete boss in shorter mode).
3. **Mid-run Choices**
   - Every N waves (e.g. 3), open a boon choice GUI (3 options).
   - Occasional elite mobs spawn and drop higher quality loot/exp.
4. **Optional Shrine Event**
   - Interact with shrine asset to start defense timer.
   - Survive dense spawn event in shrine radius.
   - On success: reward bomb + score bonus.
5. **Run End**
   - Compile score metrics (time, damage taken, chests/doors/shrines).
   - Convert to rank `S/A/B/C/D/E/F`.
   - Grant base rewards + rank bonus (extra chest/reward bomb for S/A).

## 2) Encounter system proposal (wave + elites + bosses)

## Wave pacing model
- Use a reusable wave config object with:
  - `waveNumber`
  - `budget`
  - `mobPool`
  - `affixes` (optional modifiers)
  - `specialType` (`NORMAL`, `MINIBOSS`, `BOSS`, `SHRINE_DEFENSE`)
- Drive progression by "budget" instead of hardcoded counts so scaling is generic.

## Special wave rules
- **Wave 15 (Miniboss):**
  - One miniboss from miniboss pool + supporting trash.
  - Guaranteed boon selection after clear.
- **Wave 30 (Boss):**
  - Full boss encounter + low add pressure.
  - Post-boss reward chest and rank calculation.

## Elite mob injection
- Spawn chance starts low and scales with wave.
- Elites use modifier tags (e.g. `frenzied`, `armored`, `arcane`).
- Rewards:
  - Better loot roll table.
  - Bonus exp.
  - Small score bonus for elite kills.

## 3) Shrine defense event proposal

### Trigger flow
1. Player interacts with shrine asset marker.
2. Event locks shrine area and starts countdown.
3. Spawn pulses of mobs at ring points around shrine.
4. Event fails if shrine health reaches zero or timer expires (configurable).
5. Success drops reward bomb and marks shrine complete.

### Design constraints
- One active shrine event at a time per run.
- Prevent duplicate activation with run-scoped shrine state.
- Add anti-cheese leash checks (mobs must path/teleport back into arena if too far).

### Rewards
- Guaranteed reward bomb.
- Chance for boon shard / temporary buff.
- Score injection: `+shrineCompleteBonus`.

## 4) Boon/power-up system via GUI

### Choice cadence
- Present every fixed interval (e.g. every 3 waves or after miniboss).
- Offer 3 random boons from weighted pools.

### Boon categories
- Offense: crit chance, ability damage, execute.
- Defense: damage reduction, shields, sustain.
- Utility: movement, cooldown reduction, loot/exp multipliers.

### UX + implementation fit
- Build with existing widget stack (`GuiBuilder`, `GuiLayout`, `ActionWidget`, `GuiContext`).
- Use `TooltipUtil.clickInstructions(...)` for consistent interaction hints.
- Use `ChatMessageUtil` message types for standardized feedback.

## 5) Scoring + rank system

## Suggested tracked metrics
- `timeTakenSeconds` (lower is better)
- `damageTaken` (lower is better)
- `chestsOpened`
- `doorsOpened`
- `shrinesCompleted`
- optional: `deaths`, `elitesKilled`, `bossesKilled`

## Scoring model (composable)
- Compute normalized sub-scores (0-100) per metric.
- Final score = weighted sum (0-100):
  - Time: 30%
  - Damage taken: 30%
  - Chests opened: 15%
  - Doors opened: 10%
  - Shrines completed: 15%

> Keep weights in config so balancing does not require code edits.

## Rank thresholds
- `S >= 90`
- `A >= 80`
- `B >= 70`
- `C >= 60`
- `D >= 50`
- `E >= 40`
- `F < 40`

## Reward policy
- Base reward for all clears.
- Additional rank rewards:
  - `S`: high bonus reward bomb + guaranteed premium roll.
  - `A`: medium bonus reward bomb.
- Optional weekly tracker for S/A runs.

## 6) Reusable architecture to avoid duplicate systems

To align with the project preference for generic/reusable code:

- **RunState / RunStats (new):**
  - Single source of truth for counters and timestamps.
  - Used by waves, shrine events, scoring, and reward payout.
- **EncounterOrchestrator (new):**
  - Generic encounter runner that can execute `WAVE` or `DEFENSE` phases.
- **SpawnBudgetEngine (new):**
  - Shared spawn budgeting logic (normal waves + shrine pulses).
- **RewardDistributor (extend existing reward bomb flow):**
  - One API for base reward, elite drops, shrine rewards, and rank bonus.
- **ScoringService (new):**
  - Pure calculation class for deterministic rank outputs.

## 7) Existing code touchpoints to leverage first

- Stronghold queue and entry flow already exists:
  - `StrongholdQueueManager`
  - `StrongholdQueueGUI`
  - `StrongholdCommand`
- Existing utility systems to keep UX/style consistent:
  - `ChatMessageUtil` / `ChatUtil`
  - `TooltipUtil`
  - `GuiUtil` and widget APIs under `utils/gui`
- Existing custom mob + reward configs are good foundations:
  - `src/main/resources/custom_mobs/*`
  - `src/main/resources/dungeon_mobs.yml`
  - `src/main/resources/mob_rewards.yml`

## 8) Suggested phased implementation

### Phase 1 (minimal playable loop)
- Add wave manager with 30-wave cap and milestone bosses (15/30).
- Track run timer + damage taken.
- End-of-run rank calculation + S/A bonus reward.

### Phase 2 (depth)
- Add elites and boon choice GUI.
- Add shrine defense event and shrine metric.
- Add chest/door metrics into score model.

### Phase 3 (polish + replayability)
- Add mutators/affixes and difficulty modifiers.
- Add post-run summary GUI with detailed metric breakdown.
- Add season stats/leaderboard integration.

## 9) Quick balancing defaults (starting point)
- Wave budget growth: `base + (wave * scalar)` with soft jumps at 10/20/30.
- Elite chance: `3% + 0.5% * wave` (cap around 20%).
- Boon cadence: every 3 waves + guaranteed after wave 15.
- Shrine timer: 45-75 seconds depending on team size.

---

This plan keeps systems modular so future modes (e.g., endless, time attack, duo challenge) can reuse the same orchestrator, scoring, and reward layers.
