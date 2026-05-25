# Recreating Player‑Like NPC Functionality from the Citizens Plugin

This guide explains how the Citizens plugin implements player‑like non‑player characters (NPCs) with custom skins, skin‑layer controls, and look‑close behaviour. It is intended as an implementation blueprint for this plugin’s NPC stack.

## 1) High‑level architecture

Citizens models each NPC as an `NPC` with modular `Trait` components. The same pattern maps well to `me.nakilex.levelplugin.npc.system.NPC` plus `NpcTrait` classes already present in this repository.

Core components to mirror:

- **NPCRegistry**: create/store NPCs and resolve by id/entity.
- **SkinTrait**: persisted skin name + signature + texture data.
- **Skin cache/service**: async profile lookup and apply pipeline.
- **SkinLayers trait**: cosmetic-layer toggles (hat, jacket, sleeves, pants, cape).
- **LookClose trait**: nearby-target tracking + rotation controls.
- **Rotation trait/service**: head/body yaw/pitch updates each tick.

## 2) Creating player-type NPCs

### Command flow (`/npc create` style)

1. Parse required name and optional flags (`--type`, `-b`, `-s`, `--trait`, etc.).
2. Validate sender permissions for target entity type.
3. Resolve target registry.
4. Create NPC (`createNPC(type, name)` or item-backed create path).
5. Apply flags/traits/metadata.
6. Spawn immediately unless explicitly unspawned.
7. Emit plugin events around creation and allow cancellation hooks.

### Simplified pseudocode

```java
String name = args[0];
boolean baby = hasFlag("b");
boolean silent = hasFlag("s");
EntityType type = hasFlag("type") ? parseType(flagValue("type")) : EntityType.PLAYER;
NPCRegistry registry = resolveRegistry(args);

NPC npc = registry.createNPC(type, name);
if (baby) {
    npc.getOrAddTrait(Age.class).setAge(-24000);
}
if (silent) {
    npc.data().set("silent", true);
}
if (!hasFlag("u")) {
    npc.spawn(player.getLocation());
}
```

## 3) Skin system

### Trait state to persist

- `skinName`
- `signature`
- `textureRaw`
- `updateSkins` toggle
- `fetchDefaultSkin` toggle
- `modelType` (SLIM/WIDE)

### Runtime behaviour

- Maintain a shared lowercase-keyed skin cache.
- Fetch skin profile data asynchronously.
- Retry with backoff when rate-limited.
- Apply texture/signature to the NPC profile.
- If needed for client refresh, respawn NPC after texture apply.

### Useful command semantics (`/npc skin`)

- `-c`: clear texture.
- `-t`: set explicit `(skinName, signature, texture)`.
- `--url` / `--file`: generate skin payload from image source.
- `-l`: toggle “keep skin updated”.
- `-e`: export current texture to file.

## 4) Skin layers

Add a dedicated trait that persists booleans per layer:

- `cape`
- `hat`
- `jacket`
- `leftSleeve` / `rightSleeve`
- `leftPants` / `rightPants`

When values change (or on spawn), rebuild visible flags and apply to skinnable entity state.

## 5) Look-close behaviour

Persistable fields:

- `enabled`, `range`
- `perPlayer`
- `enableRandomLook`, `randomLookDelay`
- `randomPitchMin/max`, `randomYawMin/max`
- `randomSwitchTargets`
- `realisticLooking`
- `headOnly`, `linkedBody`
- `disableWhileNavigating`
- `targetNPCs`
- `filter`

Tick loop outline:

1. Early exit if disabled or blocked by navigation mode.
2. If no target and random look enabled, occasionally rotate to random yaw/pitch.
3. In per-player mode, maintain/update packet sessions per nearby player.
4. In global mode, pick nearest valid target, optionally random-switch.
5. Apply rotation via shared rotation trait/service.

## 6) Integration plan for this repository

Given current classes under `me.nakilex.levelplugin.npc.system` and `...npc.system.trait`, implement in phases:

1. Expand current `SkinTrait` and `LookCloseTrait` data models.
2. Introduce reusable `SkinLayersTrait` and layer enum.
3. Add tick-driven `RotationTrait` abstraction (or service used by look-close).
4. Wire command handlers (`/npc create`, `/npc skin`, `/npc skinlayers`, `/npc lookclose`) to trait APIs.
5. Keep network-dependent skin fetch work async, apply results sync on main thread.
6. Add persistence mapping for new trait fields in the NPC datastore format.

## 7) Testing checklist

- Create NPCs of multiple types with different flags.
- Validate skin apply/clear/update and restart persistence.
- Validate each skin-layer toggle independently.
- Validate look-close target selection, LOS filtering, and per-player behaviour.
- Validate behaviour while NPC is navigating.
- Test across target MC versions (especially packet-dependent paths).

---

This document is implementation-focused and intentionally mirrors Citizens-style behaviour while fitting the plugin’s existing NPC architecture and trait system.
