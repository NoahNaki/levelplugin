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
- **Fantasy:** Aerial barrage combo: dash into the target, rebound upward with slow-falling control, and repeat.
- **Gameplay:** 4-hit assassin juggle that repeatedly re-engages the same target while the rogue stays airborne.
- **Implementation notes:**
  - Apply `SLOW_FALLING` to the caster during the barrage window for controllable aerial rebounds.
  - Reuse one timed loop for dash → hit → self-knockback sequencing.
  - Keep invalid cast messaging aligned with existing style via `ChatMessageUtil`.

### Offensive (2): **Nightfall Lunge**
- **Fantasy:** Conjures a compact rotating cyclone around a marked target, then lands a finisher cut.
- **Gameplay:** Shorter 4-hit single-target sequence with a clear final burst.
- **Implementation notes:**
  - Reuse one timed orbit loop (angle + radius) to keep hit pacing/data tuning centralized.
  - Reuse `ArcSlashCombatUtil.strike(...)` + direct damage for mixed AoE/single-target payoff.
  - Use `ChatMessageUtil` for invalid-target feedback to match existing spell UX.

### Rogue progression tiers
- **Shadow Flurry** → *Tempest Dive* → *Execution Drop* (more barrage hits, longer aerial window, stronger slam).
- **Smoke Bomb** → *Obscure Field* → *Dread Cloud* (longer cloud uptime, larger radius, stronger stun cadence).
- **Razor Dash** → *Rift Cut* → *Shade Surge* (higher dash speed and lane pressure).
- **Nightfall Lunge** → *Cyclone* → *Judgement* (more orbit hits and stronger finisher damage).

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

## Warrior skill concepts

These are starter concepts matching the same class structure you asked for: **2 offensive, 1 defensive, 1 mobility**.
They are designed to mirror existing class themes (clear identity, readable telegraphs, and upgrade-ready tiers).

### Offensive (1): **Sunder Chain**
- **Fantasy:** A heavy opening cleave that marks enemies, then a follow-up slam detonates the mark.
- **Gameplay:** Two-phase pressure tool that rewards sticking to the same target pack.
- **Implementation notes:**
  - Reuse one generic melee cone helper for initial cleave hit detection (angle/range/damage params).
  - Mark targets with metadata for a short window, then consume marks with the second strike for bonus damage.
  - Reuse `ChatMessageUtil` deny messaging for out-of-range or invalid cast states.

### Offensive (2): **Warbanner Crash**
- **Fantasy:** Leap a short distance and drive a spectral banner into the ground, causing radial shockwaves.
- **Gameplay:** Mid-range engage + AoE burst with one immediate hit and delayed pulse hits.
- **Implementation notes:**
  - Reuse one radial pulse scheduler (ticks/radius growth/damage falloff), which can later power other AoE spells.
  - Telegraph pulse timing with layered particles/sound to match existing readable spell cadence.
  - Keep pulse damage in a shared helper so progression tiers only tune data, not control flow.

### Defensive: **Iron Bastion**
- **Fantasy:** Brace behind a forward guard that reduces incoming frontal damage and punishes melee attackers.
- **Gameplay:** Short-duration mitigation stance with directional skill expression.
- **Implementation notes:**
  - Implement as a timed status state (duration, frontal arc, reduction percent, retaliate damage).
  - Reuse a generic incoming-damage modifier hook so future defensive spells can share it.
  - Use `ChatMessageUtil` for active/expired feedback in the same UX tone as existing class spells.

### Mobility: **Bulwark Charge**
- **Fantasy:** Shoulder-charge forward, knocking aside enemies and ending with a brief unstoppable step-through.
- **Gameplay:** Lane-control reposition tool that can initiate or disengage through enemy lines.
- **Implementation notes:**
  - Reuse the existing dash/charge movement pattern approach used by mobility spells (distance/speed/i-frame window).
  - Apply controlled knockback only to enemies in a narrow lane to preserve warrior frontliner identity.
  - Keep collision + cleanup logic in one reusable movement utility for future melee mobility variants.

### Warrior progression tiers
- **Sunder Chain** → *Bonebreaker Chain* → *Kingsplitter* (higher mark detonation damage + wider cleave).
- **Warbanner Crash** → *Siegebanner Crash* → *Cataclysm Standard* (more pulses and stronger final wave).
- **Bulwark Charge** → *Rampart Rush* → *Warlord Rush* (longer charge, stronger lane displacement).
- **Iron Bastion** → *Fortress Stance* → *Unbroken Bastion* (higher frontal mitigation + stronger punish proc).

