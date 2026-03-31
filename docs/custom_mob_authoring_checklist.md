# Custom Mob Authoring Checklist (Codex Handoff)

Use this checklist every time you add or edit files under `src/main/resources/custom_mobs/`.

## 1) Reuse existing systems first

- Reuse the YAML spell runtime (`script-key` + files in `src/main/resources/custom_mob_spells/`) before adding Java-only spell logic.
- Reuse existing script action types from `CustomMobSpellController` instead of introducing duplicate variants.
- Reuse utility classes for UX/formatting (`ChatMessageUtil`, `TooltipUtil`, etc.) rather than ad-hoc styles.

## 2) Required spell pacing fields

For each spell entry in a custom mob YAML:

- Set `interval-ticks` (per-spell cooldown).
- Set `gcd-ticks` (mob global cooldown gate).
- Keep in mind controller-enforced minimum global spacing: currently **40 ticks (2.0s)** in `CustomMobSpellController`.

If the mob still feels spammy, increase `gcd-ticks` and/or `interval-ticks` in the mob YAML first.

## 3) Scripted spell mapping sanity

For each scripted spell:

- `id` in `custom_mobs/*.yml` should match either:
  - script file `id:` in `custom_mob_spells/*.yml`, or
  - explicit `script-key` in spell definition.
- Validate animation names used by `play-animation` exist in the model.
- Prefer exact animation names (e.g., `attack_2`) over broad fallback keywords.

## 4) Combat behavior sanity

- Confirm range fields are intentional: `min-range`, `max-range`, `require-line-of-sight`.
- Validate selection controls: `selection-group`, `selection-weight`.
- For ranged mobs, test they do not collapse into melee unless intended.

## 5) Validation steps before merge

Run:

1. `mvn -q -DskipTests compile`
2. In-game smoke test:
   - spawn mob
   - run `/mobs cast <spell_id>`
   - observe spell effect + animation sync across repeated casts
3. Live combat test for at least 30-60 seconds to verify cadence is not spammy.

## 6) Files to update together

When introducing a new mob spell set, update all relevant pieces in one change:

- `src/main/resources/custom_mobs/<mob>.yml`
- `src/main/resources/custom_mob_spells/<script>.yml` (if scripted)
- optional docs summary if introducing new action patterns.
