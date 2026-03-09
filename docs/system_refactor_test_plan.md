# In-Game Test Plan: Refactors + Foundations

## 1) Unified GUI action operation flow (Blacksmith)

1. Start server and join as operator.
2. Open blacksmith upgrade, repair, and reroll modes.
3. Verify action button updates correctly when:
   - no item is inserted,
   - non-custom item is inserted,
   - valid custom item is inserted,
   - reroll placeholder is missing / invalid / valid.
4. Execute each mode and confirm:
   - coins are deducted only on valid action,
   - success/failure chat messages are shown,
   - quest hooks still trigger (upgrade/repair/reroll objectives),
   - resulting item tooltip updates correctly.

## 2) Codex mastery thresholds

1. Discover codex entries incrementally.
2. At 10/25/50 discoveries, verify codex mastery reward message appears and coins are awarded.
3. Re-log and continue discovering; verify rewards do not re-trigger for already reached levels.

## 3) Tooltip/chat builder consistency

1. Inspect any new GUI items/lore lines using the new `TooltipUtil` block helpers.
2. Verify styling consistency:
   - section headers are gold/bold,
   - requirements use bullet formatting,
   - rewards use arrow formatting,
   - status badge colors reflect active/inactive states.

## 4) Dungeon mutator abstraction (foundation)

1. Confirm plugin boots successfully with new modifier abstraction classes present.
2. (Developer check) Instantiate `RunModifierSet` in a debug command or test harness and verify
   chained reward/drop modifiers produce expected values.
