# Stronghold Generation Optimization Brainstorm

## Context from Current Code

From the current implementation, stronghold construction appears to be a **single synchronous generation pass** that:

- Creates a generated world, then immediately starts generation in that world (`createGeneratedWorld(...)`).
- Loads all source chunks for all templates up front (`loadSourceChunks(...)`).
- Grows branches with randomized placement loops (`growBranches(...)`) that can attempt many placement checks per side.

Relevant references:

- `StrongholdDebugGenerator#createGeneratedWorld`
- `StrongholdDebugGenerator#loadSourceChunks`
- `StrongholdDebugGenerator#growBranches`

There is also a proven staged pattern already in production via catacombs where rooms are attached incrementally and old rooms are cleaned up (`CatacombsManager#attachRoom`, `#completeStage`, `#scheduleRemoval`).

## High-Level Recommendation

Your analogy is exactly right: if timeouts are happening, the best win is usually to change *when* and *how much* we generate per tick, not only micro-optimize one giant generation call.

**Recommended direction:** move from one-shot generation to a **budgeted progressive pipeline**.

---

## Option A (Recommended): Progressive Generation Pipeline

### Core idea

1. Teleport player into a safe lobby/waiting pocket in the target world.
2. Generate the stronghold in **phases** with a strict per-tick block/time budget.
3. Open sections as they finish (spine first, then branches, then props/detached assets).

### Why this is likely best

- Converts worst-case lag spikes into predictable smaller work slices.
- Lets us pause/cancel safely if player leaves.
- Provides immediate feedback to player instead of long freeze.

### Suggested phases

1. **Phase 0: world + safe spawn shell**
2. **Phase 1: critical path/spine rooms only**
3. **Phase 2: branch segments**
4. **Phase 3: decorative/detail pass**
5. **Phase 4: detached assets + mobs + finalization**

### Implementation shape

Introduce a reusable generation job framework (generic, reusable for future systems too):

- `GenerationJob` (state + queue)
- `GenerationWorkUnit` (single small task)
- `GenerationScheduler` (runs N units per tick or until X ms consumed)

This keeps code generic so catacombs/dungeons/strongholds can share scheduling behavior.

---

## Option B: Two-World Staging (Prebuild then Teleport)

### Core idea

- Build the stronghold in a hidden staging world asynchronously over many ticks.
- Teleport player only after a ready threshold (e.g., spine + first combat area).

### Pros

- Player never sees partial/unready geometry.
- Easy to keep immersion polished.

### Cons

- More memory/world lifecycle complexity.
- Slightly higher management overhead.

---

## Option C: Keep One-Shot Flow, Add Heavy Optimizations

If we must keep current one-shot flow, still apply these:

1. **Placement attempt caps** per branch side and per total generation.
2. **Template compatibility pre-indexes** so we avoid repeated connector matching scans.
3. **Memoize collision checks** for candidate transforms/footprints.
4. **Chunk warmup locality** (load only required source bounds per current phase, not all upfront).
5. **Hard timeout + graceful fallback** (shorter stronghold variant if budget exceeded).

This reduces spikes but typically less effective than progressive generation.

---

## Concrete Changes to Try First (Low Risk, High Value)

1. **Add a generation budget loop**
   - Start with max `2-4 ms` of generation work per tick.
   - Measure average completion time and tune.

2. **Generate playable core first**
   - Ensure first combat-ready section is available quickly.
   - Delay non-critical branches/decor.

3. **Add watchdog + degradation policy**
   - If generation exceeds total budget (e.g., 10-15 seconds), switch to fallback template count and finish.

4. **Instrument diagnostics in prod (not just debug)**
   - Capture counts for attempts, blocked sides, placed pieces, generation millis per phase.
   - Use this telemetry to tune branch length, candidate pool size, and caps.

---

## Teleport-Then-Generate Answer

Yes — teleporting the player first and generating progressively is a valid and often superior approach **if** we guarantee:

- Spawn safety bubble is complete before teleport.
- Incomplete sections are physically blocked/hidden.
- Player messaging indicates “Stronghold stabilizing…” style progress.

This gives smoother server performance and a better perceived experience versus waiting on full one-shot generation.

---

## Reuse Opportunities Already in Repo

To avoid duplicate architecture:

- Reuse the staged room lifecycle concepts from `CatacombsManager` (attach next, remove old, stage transitions).
- Reuse existing world creation/void world flows in managers that already create instance worlds.
- Reuse existing chat/UX messaging utilities when surfacing generation states to players.

---

## Proposed Next Step

Create a small POC that only converts **spine generation** to a budgeted queue, leaving branch/decor logic unchanged initially. Measure:

- Tick impact (ms/tick while generating)
- Total completion time
- Timeout rate before/after

If successful, migrate branch/decor passes into the same scheduler.
