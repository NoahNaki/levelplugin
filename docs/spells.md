# Spells System Overview

This document provides context for adding or updating spells in **levelplugin**.
Use it as a reference whenever creating new spells or bindings.

## High-level flow

1. **Spell input** is captured by `SpellInputListener` and emitted as a `SpellInputEvent`.
2. **Spell dispatch** happens in `SpellCastListener`, which resolves a matching spell from the registry.
3. **Spell execution** is handled by a `SpellHandler` implementation (e.g. `MeteorSpell`).

## Registry + bindings

Core registry pieces live in `src/main/java/me/nakilex/levelplugin/spells`:

- `SpellRegistry`
  - Stores registered spells (`SpellDefinition` + `SpellHandler`).
  - Stores `SpellBinding` entries for class/input resolution.
  - Stores optional `SpellProgression` data for upgrades.
- `SpellBinding`
  - Supports **input sequences** (e.g. `RLL`, `Sneak+Left`) or a direct `SpellInputType` binding.
  - Use `ClassUtil` predicates for family-aware bindings (mage/rogue/etc).
- `SpellCatalog`
  - Central place to register default spells during bootstrap.
  - Call `SpellCatalog.registerDefaults(plugin)` during plugin init (already wired).

### Example binding

```java
SpellDefinition meteor = new SpellDefinition("meteor", "Meteor", 18, false);
registry.registerSpell(meteor, new MeteorSpell(plugin, particleService));
registry.registerProgression(new SpellProgression(meteor.id(), null));
registry.registerBinding(SpellBinding.forSequence(
    meteor.id(),
    ClassUtil::isMageFamily,
    SpellInputMode.MOUSE_COMBO,
    "RLL"
));
registry.registerBinding(SpellBinding.forSequence(
    meteor.id(),
    ClassUtil::isMageFamily,
    SpellInputMode.MOUSE_AND_KEYBOARD,
    "Sneak+Left"
));
```

## Spell implementation guidelines

### 1) Use ModelEngine for visuals

- Use `ModelEngineUtil.applyModels(entity, List.of("model_id"), plugin)`.
- For spells, an invisible `ArmorStand` is a common host entity.

### 2) Add physical behavior manually

ModelEngine models have no inherent physics:
- Use a scheduled task to move the host entity (e.g. `BukkitRunnable`).
- On impact, remove the host entity and trigger explosion + effects.

### 3) Damage + DoT helpers

`SpellEffectUtil` provides:
- `applyAreaDamage(Player source, Location center, double radius, double damage)`
- `startDamageOverTime(JavaPlugin plugin, Player source, Location center, ...)`

### 4) Targeting

Use `SpellTargetingUtil.resolveTargetGround(player, maxDistance)` to resolve the block the player is aiming at.

### 5) Particles

Use the existing particle system:
- `ParticleService.renderPreset(player, ElementalPresets.BURNING_SIGIL, impactLocation)`
- Add new presets to `ElementalPresets` if needed.

### 6) UX messaging

Use standardized chat helpers:
- `ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "...")`

## Meteor example (current)

- ModelEngine blueprint: `meteor_of_doom.bbmodel` (model id: `meteor_of_doom`)
- Spawned above caster, moved downward to ground target.
- On impact:
  - Explosion + lava particles
  - AoE damage (`SpellEffectUtil.applyAreaDamage`)
  - Burning sigil DoT (`ElementalPresets.BURNING_SIGIL` + `startDamageOverTime`)

## Extending for upgrades (progression)

`SpellProgression` is already in place so you can define upgrade tiers later.
For example:

- Base spell: `meteor`
- Upgrades: `meteor_double`, `meteor_big`

Wire them using `SpellRegistry.registerProgression(...)`, then adjust resolution logic
when progression data is available (e.g., player stats, class rank, or unlocks).

---

When adding a new spell:
1. Create a `SpellHandler` implementation under `spells.impl`.
2. Register it in `SpellCatalog`.
3. Bind it via `SpellBinding` to the correct class + input.
4. Add particles, model visuals, and DoT/impact logic as needed.

## Rogue arc-slash skill concepts

Use these as candidate implementations for the rogue family while reusing the existing arc slash particle debug preset (`/debug particlepreset arc`) for iteration.

### Offensive (1): **Shadow Flurry**
- **Fantasy:** Unleashes a three-wave fan of cross-slashes in front of the rogue.
- **Gameplay:** Forward pressure tool that carves center + side lanes instead of single-target locking.
- **Implementation notes:**
  - Reuse shared arc helpers (`ArcSlashCombatUtil.strike/applyConeDamage`) for each lane in the wave.
  - Keep wave timing in one reusable scheduled loop for easier balancing.
  - Keep invalid cast messaging aligned with existing style via `ChatMessageUtil`.

### Offensive (2): **Nightfall Lunge**
- **Fantasy:** Conjures a compact rotating cyclone around a marked target, then lands a finisher cut.
- **Gameplay:** Shorter 4-hit single-target sequence with a clear final burst.
- **Implementation notes:**
  - Reuse one timed orbit loop (angle + radius) to keep hit pacing/data tuning centralized.
  - Reuse `ArcSlashCombatUtil.strike(...)` + direct damage for mixed AoE/single-target payoff.
  - Use `ChatMessageUtil` for invalid-target feedback to match existing spell UX.

### Mobility: **Razor Dash**
- **Fantasy:** Fast forward dash that cuts enemies in a narrow lane.
- **Gameplay:** Gap-closer with i-frames for first 0.2s; deals light damage along path.
- **Implementation notes:**
  - Use one generic dash routine (distance/speed/i-frame window params) so later rogue skills can reuse it.
  - Spawn short-lived arc slices every few blocks along travel path.
  - Use the same cooldown/deny messaging style as existing spells with `ChatMessageUtil`.

### Defensive: **Smoke Bomb**
- **Fantasy:** Tosses a skull-like bomb canister that detonates into dense black smoke.
- **Gameplay:** Area denial utility that repeatedly stuns enemies inside the smoke cloud.
- **Implementation notes:**
  - Use a tracked dropped item (`WITHER_SKELETON_SKULL`) with explicit no-pickup handling.
  - Emit `SMOKE`/`SMOKE_LARGE` + black dust particles while the bomb is active.
  - Include cleanup hooks for owner disconnect and plugin shutdown so active bomb entities/tasks are removed reliably.


