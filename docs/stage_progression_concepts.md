# Stage-Based Progression Concepts

This document outlines lightweight gameplay loops that reuse existing systems in the plugin to deliver stage-driven or low-asset progression.

## 1. Frontier Rift Ladder

**Core loop**
1. Guild activates a "Frontier Rift" from their environment hub once the siege is won.
2. Instance-specific mob packs are generated per stage using `DungeonManager`'s layout assembly and threat tables, scaling via the existing `ThreatUtil` curve.
3. Parties clear a timed wave (or fail). Completion increments the guild's ladder rank and upgrades the next wave's modifiers.
4. Reward crates are delivered through `EnvironmentManager.grantDailyPayout`-style deposits to guild banks and feed `BattlePassManager.addProgress` for participating players.
5. Daily reset uses `CalendarManager.advanceDay` to tick ladder decay and rotate mutators.

**Why it is low asset**
* Reuses dungeon blueprints and mob definitions; only scaling tables and mutator configs are new.
* Relies on the existing ladder-style threat computation, so no manual balancing per wave.

**System reuse**
* Environment gating lets only the owning guild queue rifts, leveraging `EnvironmentManager`'s ownership map.
* `GuildQuestManager.updateObjective` hooks advance "Siege Defense" quest objectives based on cleared stages.
* Failure can trigger mercenary auto-runs (`MercenaryManager`) that attempt lower tiers while players are offline.

## 2. Mercenary War Table (Automated Contracts 2.0)

**Core loop**
1. Players unlock "deployment slots" by upgrading specific buildings; these stages already persist under `EnvironmentManager` building states.
2. A shared contract deck rolls each day using `CalendarManager`, with difficulty bands keyed to `Guild` level and siege tier.
3. Players assign existing mercenary specializations (`PathNpc` profiles) to a contract. Each contract resolves after fixed ticks, referencing guild upgrades for success modifiers.
4. Outcomes deposit guild coins/exp directly (existing guild economy) and optionally drop life-skill materials into the environment storage (`PlayerConfig` fields used for town inventory).
5. Completing streaks (e.g., 5 successful deployments) grants `BattlePass` progress and rerolls new contract cards, ensuring an idle-friendly yet interconnected loop.

**Why it is low asset**
* No new maps or mobs are required—results are simulated via rolls weighted by already-defined mercenary stats.
* Uses the existing GUI toolkit (`TooltipUtil`, `ChatFormatter`) for the war table board.

**System reuse**
* Contract success chances scale off siege tier bonuses (`GuildSiegeManager`) so controlling tougher environments matters.
* Consumable requests tap into life-skill item categories already tracked by the gathering system, creating demand for those loops.
* Daily missions feed into guild quests by posting progress via `GuildQuestManager.updateObjective`.

## 3. Life-Skill Supply Chains

**Core loop**
1. Environment buildings unlock production chains (e.g., Timberyard → Lumber → Siege Ram upgrades) tracked through building stages.
2. Guild officers configure "supply runs" using existing `LootChestManager` timers as production cooldowns.
3. Members contribute gathered materials (life skills) which accelerate production using the current deposit interfaces.
4. Each completed supply stage provides guild XP discounts (`GuildLevel` modifiers) and rotates a new recipe requiring different skill specializations.
5. Weekly resets (calendar-driven) crown the guilds with highest throughput and grant prestige cosmetics via `ChatFormatter` broadcast ceremonies.

**Why it is low asset**
* Reuses deposit GUIs and item registries; recipes can be expressed as data tables referencing existing materials.
* Production timers reuse cooldown infrastructure—no bespoke scheduler work.

**System reuse**
* Upgrading environments already grants guild XP; supply chain completions simply add multipliers.
* Life skill XP flows naturally because players must gather resources already tracked by the skill manager.
* Guild quests get new objective types without extra storage by piggybacking on existing COLLECT/CONTRIBUTE counters.

## 4. Arcane Trials Prestige Track

**Core loop**
1. Individual players activate a "trial" from their settlement shrine (environment building stage requirement).
2. The trial spawns scaled solo scenarios using `DungeonManager` arenas with stage-based stat multipliers computed via `ThreatUtil`.
3. Players earn "Arcane Marks" per cleared stage; spending marks unlocks personal boons persisted in `PlayerConfig` alongside settlement data.
4. Completing a full tier lets the player prestige—resetting marks for a multiplier that buffs guild siege damage or mercenary power.
5. Seasonal leaderboards reset with the calendar, paying out cosmetics and battle pass progress.

**Why it is low asset**
* Reuses instanced arenas; difficulty scaling is formula-driven rather than content heavy.
* Prestige rewards are numeric modifiers stored with existing player config fields.

**System reuse**
* Marks deposited into guild banks count toward guild quest contributions.
* Prestige tiers unlock cosmetic holograms leveraging `MultiLineHologram` already used for environment feedback.

## 5. Expedition Relics (Shared Progress Bar)

**Core loop**
1. Guilds invest surplus siege currency into an "expedition" bar accessible from the guild hall GUI.
2. Each investment triggers a background tick (scheduled through `CalendarManager`) that progresses a shared meter.
3. When the meter fills, the plugin spawns a one-off dungeon event; success unlocks a relic that grants passive modifiers stored on the guild profile.
4. Relics decay over time unless refreshed via life-skill deliveries, tying gathering into the loop.
5. Expended expeditions feed the battle pass and unlock new siege cosmetics to close the loop.

**Why it is low asset**
* Uses a shared progress value rather than bespoke content; only relic modifiers and broadcast strings are needed.
* Dungeon event can reuse existing boss templates with stat multipliers.

**System reuse**
* Guild data model already tracks coins, xp, and quests; relic states can be additional fields without new persistence layers.
* Life skill deposits already persist in `PlayerConfig`, so decay refresh checks can piggyback on that storage.
* Chat UX for milestones uses `ChatFormatter` to remain consistent.
