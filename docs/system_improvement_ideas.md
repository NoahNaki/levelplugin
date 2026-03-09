# System Improvement Ideas (Grounded in Existing Plugin Systems)

This document proposes features that **build on systems already in this repository** so we avoid duplicate mechanics and keep implementation reusable.

## Design principles for new features

- Prefer extending existing managers/GUI/util classes over adding parallel systems.
- Keep player-facing text/lore consistent by routing through `ChatUtil`, `ChatMessageUtil`, `TooltipUtil`, and `GuiUtil`.
- Where a new behavior is similar to an existing one, extract a generic service (interface + strategy/enum) instead of adding one-off logic.

## 1) Weekly Contracts Board (cross-system progression)

A rotating set of contracts that combine goals from combat, gathering, dungeon, arena, and pet systems.

### Why it fits
- Reuses existing progression/reward loops without inventing a brand-new economy.
- Encourages players to touch underused systems.

### Existing hooks to reuse
- Reward payout patterns from life skills / combat reward services.
- Existing GUI patterns (pagination + claim interactions) from `LifeSkillRewardsGUI`, battle pass GUI, and codex/list GUIs.
- Chat and tooltip styling via `ChatMessageUtil` and `TooltipUtil`.

### Reusable implementation approach
- Add a generic `ObjectiveTracker` abstraction with objective types (`KILL_MOB`, `GATHER`, `COMPLETE_DUNGEON`, `WIN_ARENA`, `SUMMON_PET`, etc.).
- Have each system publish progress events into this tracker rather than each contract directly reading subsystem internals.

## 2) Affix Fusion at Blacksmith (item sink + item identity)

Allow players to consume duplicate/unused gear to transfer a selected stat range bonus (or reroll token) onto a target item.

### Why it fits
- Extends blacksmith reroll/upgrade identity without replacing current crafting flows.
- Uses existing item generation metadata and rarity/level requirements.

### Existing hooks to reuse
- `ItemUpgradeManager`, `ItemRerollManager`, and blacksmith GUI mode switching patterns.
- Existing item tooltip rebuild path in item utilities.
- Existing durability/upgrade persistence fields.

### Reusable implementation approach
- Generalize current blacksmith mode handling into a `BlacksmithOperation` strategy interface (`upgrade`, `repair`, `reroll`, `fuse`).
- Reuse one confirmation and result formatting pipeline for all operations.

## 3) Dungeon Mutators (light roguelike variance)

Before dungeon start, apply 1–3 mutators (e.g., `Fragile`, `Elite Swarms`, `Arcane Surge`) that alter enemy stats, drops, and/or room constraints.

### Why it fits
- Makes existing verified dungeons replayable with minimal new map content.
- Can directly interact with rating/reward systems.

### Existing hooks to reuse
- Verified dungeon definition and dungeon manager lifecycle.
- Mob stat/model configuration path used by custom mobs.
- Existing reward calculators and loot chest integration.

### Reusable implementation approach
- Introduce a generic `RunModifier` model consumed by dungeon and arena later.
- `RunModifier` can expose hooks such as `modifyMobStats`, `modifyRewards`, and `appendRunSummaryLines`.

## 4) Town Project Donations (community unlock loop)

A server-wide project board where players donate resources/currency to unlock buffs, vendors, cosmetics, or map utilities.

### Why it fits
- Reuses storage/economy/reward communication patterns.
- Gives long-term goals for surplus resources.

### Existing hooks to reuse
- Existing config-driven stage data (`buildingstages.yml`, etc.).
- GUI interaction patterns from storage/merchant/battle pass.
- Reward broadcast/message utility helpers.

### Reusable implementation approach
- Build a generic `MilestoneProjectService` that can back town upgrades now and other collective events later.
- Each project defines contribution item predicates + milestone rewards in YAML.

## 5) Pet Expedition Assist (pet + mercenary synergy)

Allow assigned pets to provide expedition bonuses (speed, success floor, bonus drop table entries, or friendship multipliers).

### Why it fits
- Connects two established systems with minimal UX overhead.
- Increases value of pet progression without pure combat power creep.

### Existing hooks to reuse
- `ActiveExpedition` and expedition GUI/reward flow.
- Pet effect definitions and pet profile persistence.
- Existing summary/chat feedback utilities in pet package.

### Reusable implementation approach
- Add a generic `CompanionBonusProvider` contract used by both mercenary and pet systems.
- Expedition simulator consumes normalized bonus values instead of pet-specific logic.

## 6) Arena Streak Perks (session excitement)

Short-session perks earned by consecutive arena wins/loss recovery (temporary cosmetic trails, tiny rating shield, bonus currency chance).

### Why it fits
- Builds on current arena queue/match/rating systems.
- Provides immediate goals even when leaderboard climb is slow.

### Existing hooks to reuse
- `ArenaQueueManager`, match managers, combat summary broadcaster.
- Existing toggle/feedback util classes for player notifications.

### Reusable implementation approach
- Add generic `SessionProgressTrack` utility for streaks that can later be reused by dungeons/contracts.
- Keep perk rules declarative in config (threshold -> perk IDs).

## 7) Dynamic Resource Events (world activity spikes)

Timed region events that temporarily boost specific gathering nodes/mobs and inject event-specific drops.

### Why it fits
- Extends current woodcutting/farming/mining reward systems.
- Promotes movement through fast travel and region content.

### Existing hooks to reuse
- Existing reward config files for gathering and mob rewards.
- Fast travel and region systems for surfacing active events.
- Existing hologram/chat messaging utilities for announcements.

### Reusable implementation approach
- Build a `TimedWorldEventService` with pluggable event effects (`rewardMultiplier`, `extraDropTable`, `spawnOverride`).
- Reuse same event framework for holiday events later.

## 8) Codex Mastery Bonuses (collection payoff)

Grant passive account bonuses when codex categories hit completion thresholds.

### Why it fits
- Gives codex tangible progression value.
- Encourages exploration and combat diversity.

### Existing hooks to reuse
- Codex GUI and tracking model.
- Existing attribute/reward messaging utilities.

### Reusable implementation approach
- Implement a generic `ThresholdRewardEngine` (used by codex now, contracts later).
- Store threshold + reward descriptors in config for easier balancing.

## Reusable refactors worth doing first (high leverage)

1. **Unified operation interface for action GUIs**
   - Blacksmith, enchanting, salvage, and future systems all run a `preview -> validate -> confirm -> apply -> message` loop.
   - Extract to a reusable controller to reduce duplicated click handlers.

2. **Shared objective/event bus**
   - Many systems need "progress happened" signals (battle pass, contracts, codex, achievements).
   - Introduce a small internal event layer so systems can subscribe without hard dependency chains.

3. **Consistent tooltip/chat builders**
   - Add small wrappers for recurring lore structures (`section header`, `requirements block`, `reward list`, `status badge`) on top of `TooltipUtil`.
   - Keep all system UIs visually consistent by default.

## Recommended first implementation order

1. Start with **Contracts Board** using the shared objective tracker.
2. Add **Codex Mastery** on top of the same threshold reward engine.
3. Implement **Blacksmith Affix Fusion** once generic operation flow exists.
4. Expand with **Dungeon Mutators** reusing run modifier abstractions.

This sequence gives reusable foundation pieces early and avoids one-off feature code.
