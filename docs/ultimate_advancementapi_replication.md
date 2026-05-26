# Replicating the UltimateAdvancementAPI in Your Own Plugin

This document outlines a comprehensive strategy for replicating the functionality of UltimateAdvancementAPI within your own Minecraft plugin. The objective is to recreate the core features of the API (custom advancement tabs, flexible advancement trees, team-based progression, persistence, and command integration) while adhering to good design practices and maintainability.

## 1) Purpose and Scope

UltimateAdvancementAPI provides a robust framework for creating custom advancement trees on Spigot-style servers. It defines a model with advancement tabs, root advancements, child advancements, and multi-task advancements, all of which can be persisted and tracked per team.

Before implementation, decide which features you need. The full API offers:

- Namespaced advancement tabs with a unique root advancement.
- A base `Advancement` class with progression tracking and optional reward messages.
- `RootAdvancement` for the first node of a tab (stores a background texture and is always visible).
- `BaseAdvancement` for basic children with a parent.
- Multi-task support via `MultiTasksAdvancement` and `TaskAdvancement`, allowing a single advancement to consist of multiple hidden tasks.
- Graphical information encapsulated in `AdvancementDisplay` (icon, title, description, frame type, position).
- Team-based progression stored in `TeamProgression` objects, cached by a `DatabaseManager`.
- Event system for registration, progression, and disposal events.
- Optional NMS wrappers for cross-version packet handling and disabling vanilla advancements.

This plan focuses on the core advancement model, persistence, and basic commands. Advanced features like cross-version packet manipulation and reflection-based vanilla advancement removal can be implemented later.

## 2) Architecture Overview

To mirror the API, structure your plugin into modules:

- **Core Module**: Advancement model (tabs, advancements, tasks), progression logic, display settings.
- **Persistence Module**: Storage of advancement progression for teams. Start in-memory, then add SQLite/MySQL.
- **Event Module**: Registration/progression/disposal events emitted by the core module.
- **Command Module**: Admin commands to grant, revoke, reset, and inspect progress.
- **Optional NMS/Version Module**: Version-specific packet behavior and vanilla advancement controls.

This separation keeps API surfaces clean and improves long-term maintainability.

## 3) Designing the Advancement Model

### 3.1 AdvancementTab

Represents one tab in the advancement GUI. Each tab should have a unique namespace and one root advancement.

Responsibilities:

- Namespace uniqueness across registered tabs.
- Root/child registration with ownership validation.
- Lifecycle methods: `initialise()`, `dispose()`, `isActive()`.
- Player/team tab visibility and client updates.
- Tracking which teams/players are currently viewing the tab.

```java
public final class AdvancementTab {
    private final String namespace;
    private final Map<AdvancementKey, Advancement> advancements = new HashMap<>();
    private RootAdvancement root;
    private boolean initialised;

    public void registerAdvancements(RootAdvancement rootAdv, Set<BaseAdvancement> children) {
        if (initialised) throw new IllegalStateException("Tab already initialised");
        this.root = rootAdv;
        // populate advancements map, validate ownership
        this.initialised = true;
    }
}
```

### 3.2 Advancement Base Class

Abstract parent for all advancement types.

Core fields:

- `AdvancementKey key`
- `AdvancementTab tab`
- `AdvancementDisplay display`
- `int maxProgress`

Core methods:

- `int getProgression(UUID teamId)`
- `boolean isGranted(UUID teamId)`
- `void increment(UUID teamId, int amount)`
- `void setProgression(UUID teamId, int newProgress, boolean giveRewards)`
- `boolean isVisible(UUID teamId)` (default `true`, overridable)

### 3.3 RootAdvancement

First node of a tree. Extend `Advancement`, add `String backgroundTexture`, and always return visible.

### 3.4 BaseAdvancement and Child Hierarchy

`BaseAdvancement` extends `Advancement` and adds `parent`. Validate parent tab ownership during registration.

For complex progressions, use `MultiTasksAdvancement` with a collection of `TaskAdvancement`:

- Each task has its own `maxProgress`.
- Sum of task progress requirements must equal parent `maxProgress`.
- Parent updates whenever a task changes.

### 3.5 AdvancementDisplay

Encapsulate display metadata:

- `ItemStack` icon
- Title
- Description lines
- Frame type
- Toast/chat flags
- Coordinates

Use a builder for clean construction.

### 3.6 TeamProgression

Store:

- Team UUID
- Team member UUIDs
- Advancement progress map

Cache by team ID so shared teams reuse a single progression object.

## 4) Persistence Layer

### 4.1 Storage Strategy

Define an `IDatabase` interface and start with:

- `InMemoryDatabase`
- `SQLDatabase` (SQLite/MySQL + pooling, async operations)

### 4.2 Caching and Loading

On login, load/create team progression and cache by team ID. Decide cache retention policy for offline players based on performance goals.

### 4.3 Saving Progress

Define update methods like:

```java
int updateProgression(AdvancementKey key, TeamProgression team, int newProgress)
```

Return prior value and fire progression events on changes.

## 5) Event System

Provide events such as:

- `AdvancementRegistrationEvent`
- `AdvancementProgressionUpdateEvent`
- `AdvancementDisposeEvent`
- `AdvancementDisposedEvent`

Use Bukkit’s event system (preferred) or a lightweight internal bus.

## 6) Command Integration

Implement administrative commands:

- `/advancement list`
- `/advancement grant <player> <tab>:<advancement>`
- `/advancement reset <player> <tab>:<advancement>`
- `/advancement show <player> <tab>`

Add permissions and tab-completion for namespaced keys.

## 7) NMS and Version Support (Optional)

If multi-version behavior is needed, define a `VersionedAdapter`:

- `sendAdvancementPacket(Player, AdvancementTab)`
- Version-specific implementations per supported server version

Keep all NMS/reflection code isolated.

## 8) Implementation Checklist

1. Implement model classes (`AdvancementKey`, `AdvancementDisplay`, `Advancement`, `RootAdvancement`, `BaseAdvancement`, `MultiTasksAdvancement`, `TaskAdvancement`).
2. Implement `AdvancementTab` lifecycle and registration.
3. Add persistence (`IDatabase`, in-memory first, SQL second).
4. Add `TeamProgression` and team cache behavior.
5. Add events and listeners for rewards/announcements.
6. Add commands for management and inspection.
7. Add tests for progression caps, task sums, and visibility behavior.
8. Optimize concurrency, persistence, and version support.

## 9) Considerations and Pitfalls

- **Thread safety**: Use concurrency-safe collections/locks for shared caches.
- **Validation**: Fail fast on invalid parent-child ownership and task sum mismatch.
- **Performance**: Avoid unnecessary packet churn; batch updates where possible.
- **Upgrades**: Keep version-specific code decoupled.
- **Licensing**: UltimateAdvancementAPI is LGPL-licensed; avoid directly copying implementation code unless you comply.

## 10) Conclusion

You can replicate UltimateAdvancementAPI capabilities by implementing a clear advancement model, team-based progression, persistence, events, and command tooling in stages. Start small (core + in-memory persistence), then progressively add multi-task, SQL, and cross-version support.
