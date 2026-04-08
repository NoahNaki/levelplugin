# Stronghold System Vision (Design Intent)

This document captures the intended direction for the stronghold debug/generation system and the template connection model.

## Core Vision

1. **Graph-first generation, template-second realization**
   - Generate a dungeon graph (`SNAKE`, `BRANCHING`, `TEST`) as logical topology first.
   - Materialize each node by selecting a template that satisfies that node's required connector directions.

2. **Deterministic connector alignment**
   - Every connector pairing should resolve through one canonical transform path:
     - template-local connector marker -> world anchor
     - world anchor -> solved room/connector center
   - The same math should power normal generation and test flows.

3. **Place rooms, then bridge edges**
   - Place node templates first.
   - For each graph edge, resolve and paste an explicit connector template that exactly aligns both endpoints.
   - Reject candidate placements early when overlap is above the allowed threshold.

4. **Controlled variety with guardrails**
   - Use category pools (straight/corner/deadend/connector/tower/gate).
   - Keep variety without chaos via usage/recent weighting.
   - Gate large structures (tower/gate) with spacing and neighborhood constraints.

5. **Debuggability as a first-class goal**
   - Toggle templates through GUI.
   - Tune overlap tolerance via command.
   - Provide focused test-mode behavior for connector validation.

---

## Intended Template Connection Model

For any two connected nodes `A -> B`:

1. Find connector on `A` facing `B`.
2. Find compatible connector on `B` facing `A`.
3. Compute a world anchor for one side.
4. Solve center of the other template from that anchor.
5. Validate that resolved opposite endpoint exactly matches expected anchor.
6. Only accept if all placed-neighbor constraints still hold.

This model should stay generic and reusable so every placement flow uses the same primitives.

---

## Template Catalog (Captured Regions)

Template IDs are generated in load order as `<category>_<index>`.

### Corner templates
- `corner_1`: `(473, -38, -5346) -> (543, -61, -5276)`
- `corner_2`: `(544, -38, -5631) -> (614, -61, -5701)`
- `corner_3`: `(614, -61, -5630) -> (544, -38, -5560)`

**Intended use:** 2-way turns (L-junctions) between orthogonal branches.

### Straight templates
- `straight_1`: `(402, -38, -5276) -> (472, -61, -5346)`
- `straight_2`: `(472, -61, -5347) -> (402, -38, -5417)`
- `straight_3`: `(402, -38, -5418) -> (472, -61, -5488)`
- `straight_4`: `(472, -61, -5489) -> (402, -38, -5559)`
- `straight_5`: `(402, -38, -5560) -> (472, -61, -5630)`
- `straight_6`: `(472, -61, -5631) -> (402, -38, -5701)`
- `straight_7`: `(473, -38, -5701) -> (543, -61, -5631)`
- `straight_8`: `(543, -61, -5630) -> (473, -38, -5560)`
- `straight_9`: `(473, -38, -5417) -> (543, -61, -5347)`

**Intended use:** primary corridor/wall chain segments for opposite-direction links.

### Dead-end templates
- `deadend_1`: `(543, -38, -5418) -> (473, -61, -5488)`
- `deadend_2`: `(473, -61, -5489) -> (543, -38, -5559)`

**Intended use:** branch termination nodes (degree 1).

### Connector templates
- `connector_1`: `(412, -61, -5711) -> (402, -38, -5701)`
- `connector_2`: `(402, -38, -5721) -> (412, -61, -5711)`

**Intended use:** explicit bridge pieces inserted between already-placed node templates.

### Tower templates
- `tower_1`: `(615, -61, -5488) -> (685, -7, -5418)`
- `tower_2`: `(615, -61, -5276) -> (685, -7, -5206)`

**Intended use:** larger landmark nodes with constrained frequency/spacing.

### Gate templates
- `gate_1`: `(686, -61, -5346) -> (614, -10, -5418)`
- `gate_2`: `(686, -61, -5276) -> (614, -10, -5346)`

**Intended use:** special large straight-chain set pieces, placed with additional adjacency constraints.
