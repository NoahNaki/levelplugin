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
