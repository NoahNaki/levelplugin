# Environment Area Building Setup Guide

This document captures the current environment-area instancing workflow so a fresh conversation can still add new buildings correctly.

## Scope
- Main class: `EnvironmentAreaInstanceManager`
- Source world: `flatland`
- System flow:
  1. Copy full **empty world** area into player instance (`AREA`).
  2. For each building upgrade, capture a **finished world** selection and project it into empty-world coordinate space.
  3. Paste projected building blocks with layered animation.
  4. Hologram positions are also supplied in **finished-world coordinates** and projected with the same anchor offset.

## Critical Coordinates
Defined in `EnvironmentAreaInstanceManager`:
- `AREA` (full empty-world selection)
- `FINISHED_WORLD_AREA` (full finished-world selection)
- `FINISHED_WORLD_ANCHOR`
- `EMPTY_WORLD_ANCHOR`

The two anchors must represent the same relative corner/location between worlds.

## Projection Rules
Use anchor offset (finished -> empty):
- `dx = EMPTY_WORLD_ANCHOR.x - FINISHED_WORLD_ANCHOR.x`
- `dy = EMPTY_WORLD_ANCHOR.y - FINISHED_WORLD_ANCHOR.y`
- `dz = EMPTY_WORLD_ANCHOR.z - FINISHED_WORLD_ANCHOR.z`

Helpers already exist and should be reused:
- `projectFinishedToEmpty(Cuboid)`
- `projectFinishedToEmpty(WorldPoint)`

Do **not** add one-off mapping logic per building.

## Adding a New Building
When given new coordinates from game selections:

1. Collect from user/admin:
   - Building id/name
   - Finished-world build selection (`pos1`, `pos2`)
   - Finished-world hologram location

2. Add a new `BuildingTemplate` in `BUILDINGS`:
   - `source` = finished-world selection cuboid
   - `placement` = `projectFinishedToEmpty(source cuboid)`
   - `hologramPoint` = `projectFinishedToEmpty(finished hologram point)`

3. Choose icon material for hologram fallback marker (`marker` field).

4. Keep `slot` unique and update in order.

### Example Pattern
```java
new BuildingTemplate(
    5,
    "new_building",
    "New Building",
    Material.STONE,
    new Cuboid(x1, y1, z1, x2, y2, z2),
    projectFinishedToEmpty(new Cuboid(x1, y1, z1, x2, y2, z2)),
    projectFinishedToEmpty(new WorldPoint(hx, hy, hz))
)
```

## Validation Checklist
After changes:
1. Compile: `mvn -q -DskipTests compile`
2. In game:
   - Run `/debug area initialize <player>`
   - Confirm hologram appears at expected location.
   - Purchase build and verify replacement appears in correct empty-world counterpart region.
3. Check logs for debug line:
   - `[EnvironmentArea] Building '<id>' ... sourceDims=... destMin=... destMax=... blockCount=...`

## Common Failure Modes
- Build says complete but no visible change:
  - Wrong selection bounds or wrong anchor pairing.
  - Hologram point provided from empty world while code expects finished-world point before projection.
- Build appears shifted:
  - Anchor mismatch (not same relative corner between worlds).
- Partial paste:
  - Incorrect selection volume (wrong `pos1/pos2` Y or swapped area from another structure).

## UX/Behavior Notes
- Keep existing coin deduction, coin visuals, and layered animation behavior unchanged.
- Keep chat style using existing utilities (`ChatMessageUtil`, `TooltipUtil`) if any new user-facing text is introduced.
