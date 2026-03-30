# Custom Mob Spell System Summary

This document summarizes the recent custom-mob spell overhaul and follow-up fixes/tuning.

## 1) Core Architecture Changes

### YAML-driven spell runtime
- Added `CustomMobSpellScriptEngine` to load spell actions from:
  - legacy `custom_mob_spells.yml` (if present), and
  - per-file scripts in `custom_mob_spells/*.yml`.
- Script actions execute sequentially with support for `delay` steps.
- Spell scripts can be addressed by file path stem and/or explicit `id` in script YAML.

### Extended custom spell definition model
`CustomMobDefinition.CustomMobSpell` now supports:
- `selectionGroup`
- `selectionWeight`
- `minRange`
- `maxRange`
- `requireLineOfSight`
- `gcdTicks`
- `scriptKey`

This enables more data-driven behavior tuning directly in mob YAML.

## 2) Spell Selection & Casting Flow

`CustomMobSpellController` now:
- collects eligible spells by group,
- filters by cooldown, range, and optional LOS,
- picks weighted random spells inside each group,
- applies both per-spell cooldown and mob-level global cooldown,
- runs script actions first and only keeps minimal fallback handling for basic legacy spell IDs.

## 3) Generic Script Action Runtime

The controller exposes reusable generic handlers used by scripts, including:
- animation/sound actions,
- projectile launchers (arrow/model/mage fireball),
- area pulses and ring/ray effects,
- cone damage + knockback,
- dash/rush movement,
- target-relative teleport/smoke,
- delayed explosion-at-target,
- nearby allied custom-mob healing.

The goal is that new skills are authored in YAML without adding new spell-specific Java methods.

## 4) Removed Hardcoded Spell-Specific Paths

Follow-up cleanup removed hardcoded per-mob cast branches and dead helpers from the controller (e.g. old knight-specific direct methods), in favor of script-driven behavior + shared primitives.

## 5) Resource Loading Hardening

Startup was fixed so plugin enable no longer crashes when optional embedded spell resources are missing:
- resource extraction now checks `plugin.getResource(path)` before `saveResource(path, false)`.
- this guard is used for both legacy and folder default resources.

## 6) Bundled Script Files Added

Added/maintained scripts under `src/main/resources/custom_mob_spells/` for:
- cursed archer (`shoot_1/2/3`)
- cursed mage (`spell_1/2/3`)
- cursed knight (`attack_1/2/3`)
- goblin archer (`shoot`, `throw_bomb`)
- goblin assassin (`shadowstep`, `stab`, `slash`)
- goblin warrior (`sword_slam`, `shield_rush`)
- goblin shaman (`fireball`, `heal`)

## 7) Ranged AI/Distance Tuning

To reduce caster mobs collapsing into melee range:
- added generic preferred-distance handling for ranged profiles inferred from spell metadata,
- tuned standoff logic to avoid affecting primarily melee kits,
- updated several ranged mob YAML configs with explicit `min-range`/`max-range`/LOS/GCD where missing.

Notable tuning updates were applied to:
- `cursed_archer`
- `cursed_mage`
- `goblin_archer`
- `goblin_shaman`
- `ember_witch`
- `cursed_arrow`

## 8) Intent/Design Direction

The current system is intentionally generic and reusable:
- behavior should be script-authored in YAML,
- controller code should provide primitives and orchestration,
- mob-specific spell logic in Java should be minimized/avoided.

