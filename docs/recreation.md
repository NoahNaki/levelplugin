# Plugin Recreation Workflow

## Purpose

This workflow is used whenever recreating functionality from an existing plugin, repository, JAR, or gameplay system into LevelPlugin.

The objective is NOT to blindly copy code.

The objective is to:

1. Understand how the reference plugin works.
2. Compare it against the current LevelPlugin implementation.
3. Identify missing systems and architecture.
4. Implement features in small reviewable iterations.
5. Reach a testable state as quickly as possible.
6. Avoid massive Codex implementation attempts that are difficult to review.

---

# Inputs

I will provide one or more of the following:

## Reference Sources

* Reference plugin repository
* Decompiled source
* Plugin JAR
* Plugin ZIP
* Videos
* Documentation
* Screenshots
* Configuration files

## Current Project

One or more of:

* Entire LevelPlugin ZIP
* Relevant package ZIP
* Changed-files ZIP
* Git diff
* Review package

---

# Phase 1: Reference Plugin Analysis

Before writing any code:

Analyze the reference plugin and determine:

## Core Systems

What major systems exist?

Examples:

* Tree detection
* Dialogue engine
* Fishing minigames
* NPC rendering
* Cooking recipes
* Progression systems

## Architecture

Determine:

* Main managers
* Services
* Registries
* Data models
* Event flow
* State management

## Gameplay Flow

Determine:

Player Action
↓
Internal Processing
↓
Visual Feedback
↓
Rewards

Explain the complete flow.

## Hidden Features

Look for features users may not immediately notice:

* Edge cases
* Optimizations
* Anti-exploits
* Quality-of-life features
* Visual polish

---

# Phase 2: Gap Analysis

Compare the reference plugin against the current LevelPlugin implementation.

Create four categories:

## Already Implemented

Features that already exist.

## Partially Implemented

Features that exist but are missing functionality.

## Missing

Features that do not exist.

## Should Not Be Recreated

Features that:

* Conflict with LevelPlugin architecture
* Introduce unnecessary complexity
* Are plugin-specific
* Have better alternatives

---

# Phase 3: Implementation Roadmap

Split work into phases.

Example:

Phase 1

* Core data structures
* Registries
* Managers

Phase 2

* Gameplay logic

Phase 3

* Visual feedback

Phase 4

* Commands

Phase 5

* Persistence

Phase 6

* Polish

Each phase must be independently reviewable.

Do NOT attempt full implementation in one step.

---

# Phase 4: Codex Task Generation

Generate a Codex task for ONLY the next phase.

The task must include:

## Objective

What is being implemented?

## Files To Modify

List exact files.

## Files To Create

List exact files.

## Requirements

Concrete implementation requirements.

## Restrictions

Follow existing architecture.

Before modifying or creating anything go through the repository to see if there are similar things already created so that we avoid creating more functions/classes that effectively do the same thing.

If there is a scenario where a function needs to be created but there is already one that exists that does something similar, make the existing method more generic so that we can reuse it in several scenarios.

Adopt the concept of generic and reusable code.

Make sure to look at existing utility classes before introducing new utilities.

Do not create duplicate systems.

## Deliverables

What should exist after completion?

---

# Phase 5: Review Mode

When I provide:

* Changed-files ZIP
* Diff package
* Branch ZIP
* Review package

Do NOT immediately continue implementation.

First perform a review.

Determine:

## Correct

What was implemented correctly?

## Problems

What is incorrect?

## Architectural Concerns

What could become technical debt?

## Missing Work

What was not completed?

## Overengineering

What should be simplified?

## Regression Risk

What may break existing systems?

---

# Phase 6: Completion Assessment

Estimate completion:

* Architecture %
* Gameplay %
* Integration %
* Polish %

Example:

Architecture: 100%
Gameplay: 75%
Integration: 80%
Polish: 20%

Overall Completion: 68%

---

# Phase 7: Next Codex Task

After review:

Generate ONLY the next task required.

Do not generate instructions for future phases.

Keep iteration size small.

Target:

1-3 hour implementation chunks.

---

# Testable State Definition

A feature is considered testable when:

* Builds successfully
* Loads without errors
* Core gameplay loop functions
* No placeholder implementations remain
* Can be evaluated in-game

Only then should human testing begin.

---

# Final Rule

Prefer:

Small iteration
↓
Review
↓
Refine
↓
Review
↓
Test

Over:

Massive implementation
↓
Massive bug fixing
↓
Massive refactor
