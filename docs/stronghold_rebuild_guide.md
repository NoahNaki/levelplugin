# Stronghold System Rebuild Guide (Start-Over Plan)

This guide describes a clean rebuild of the stronghold generator from scratch.

## 1) Goal

Build a **deterministic, connector-driven, graph-first** system where:

1. Graph topology is generated first (no world placement yet).
2. Templates are selected by connector compatibility.
3. Placement uses one canonical transform solver.
4. Alignment is driven by **redstone alignment markers** captured from templates.
5. Connector templates are inserted between rooms with correct axis/direction.

---

## 2) Source Template Coordinates (Current Catalog)

Use these cuboids as canonical source captures.

## Corner templates
- `corner_1`: `(473, -38, -5346)` -> `(543, -61, -5276)`
- `corner_2`: `(544, -38, -5631)` -> `(614, -61, -5701)`
- `corner_3`: `(614, -61, -5630)` -> `(544, -38, -5560)`

Tags: `CORNER`

## Straight templates
- `straight_1`: `(402, -38, -5276)` -> `(472, -61, -5346)`
- `straight_2`: `(472, -61, -5347)` -> `(402, -38, -5417)`
- `straight_3`: `(402, -38, -5418)` -> `(472, -61, -5488)`
- `straight_4`: `(472, -61, -5489)` -> `(402, -38, -5559)`
- `straight_5`: `(402, -38, -5560)` -> `(472, -61, -5630)`
- `straight_6`: `(472, -61, -5631)` -> `(402, -38, -5701)`
- `straight_7`: `(473, -38, -5701)` -> `(543, -61, -5631)`
- `straight_8`: `(543, -61, -5630)` -> `(473, -38, -5560)`
- `straight_9`: `(473, -38, -5417)` -> `(543, -61, -5347)`

Tags: `STRAIGHT`

## Dead-end templates
- `deadend_1`: `(543, -38, -5418)` -> `(473, -61, -5488)`
- `deadend_2`: `(473, -61, -5489)` -> `(543, -38, -5559)`

Tags: `DEADEND`

## Connector templates
- `connector_1`: `(412, -61, -5711)` -> `(402, -38, -5701)`
- `connector_2`: `(402, -38, -5721)` -> `(412, -61, -5711)`

Tags: `CONNECTOR`

## Tower templates
- `tower_1`: `(615, -61, -5488)` -> `(685, -7, -5418)`
- `tower_2`: `(615, -61, -5276)` -> `(685, -7, -5206)`

Tags: `TOWER`, `LARGE`, `LANDMARK`

## Gate templates
- `gate_1`: `(686, -61, -5346)` -> `(614, -10, -5418)`
- `gate_2`: `(686, -61, -5276)` -> `(614, -10, -5346)`

Tags: `GATE`, `LARGE`, `LANDMARK`

---

## 3) Connector Rules (Critical)

- Redstone blocks are **alignment markers** for IO faces.
- Parse redstone marker groups from captured templates.
- Infer connector facing from marker placement (same behavior as current `RoomTemplate.capture` connector extraction).
- Connector compatibility:
  - `A.facing == opposite(B.facing)`
  - `A.type == B.type`

---

## 4) Block Filtering Rules While Pasting

When materializing templates, ignore these materials:
- `AIR`
- `WHITE_CONCRETE`
- `LIGHT_BLUE_CONCRETE`
- `REDSTONE_BLOCK`

Reason: these are marker/guide blocks and should not appear in generated output.

---

## 5) Rebuild Architecture (Minimal and Reliable)

## Step A: Capture phase (once per run)
1. Capture all templates from cuboids above.
2. Build runtime template metadata from captures:
   - bounds
   - connectors (from redstone markers)
   - tags

## Step B: Graph phase
1. Generate graph (`SNAKE`, `BRANCHING`, `TEST`).
2. Each node has required degree.

## Step C: Placement phase
1. Place root at origin.
2. For each graph edge, place child by trying:
   - candidate templates matching degree/tag
   - all rotations
   - compatible connector pairs
3. Use one canonical transform solver for every pair.
4. If overlap, push forward along connector axis (bounded retries) or reject.

## Step D: Connector insertion phase
1. For each placed room-room edge, choose connector template by best directional fit.
2. Score candidates by endpoint distance error.
3. Accept best non-colliding candidate.

## Step E: Materialization phase
1. Paste transformed blocks into world.
2. Apply ignore-material filter above.

---

## 6) Determinism Requirements

- Seed controls graph, selection order, and tie-breaks.
- Same seed + same catalog data => same layout.
- Log seed and key selection decisions.

---

## 7) Debugging Checklist

For every attempted placement log:
- template id
- rotation
- connector pair chosen (`A.face -> B.face`)
- solved transform
- overlap rejection reason (with bounds)

For connector insertion log:
- connector template candidates considered
- endpoint distance score
- chosen template + rotation

For failures log:
- first failing node id
- required degree
- candidate count

---

## 8) Command Surface

Required command behavior:
- `/debug stronghold generate [mode] [seed] [rooms]`
- `/debug stronghold templates`

Recommended while rebuilding:
- Keep `TEST` mode as a strict two-room + one-connector smoke test.
- Add a temporary `validate` mode that only runs capture + connector parsing and prints found connectors.

---

## 9) Implementation Notes

- Reuse `RoomTemplate.capture` for cuboid parsing and connector extraction.
- Reuse existing rotation/block-data utilities where possible.
- Keep placement math in one solver method to avoid divergence.
