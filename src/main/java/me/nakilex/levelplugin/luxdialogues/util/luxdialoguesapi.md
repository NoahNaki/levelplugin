# LuxDialogues Integration & Architecture Guide

## LevelPlugin Dialogue Framework

---

# Purpose

This document explains the current LuxDialogues integration architecture used inside LevelPlugin.

This is the canonical reference for:

* NPC dialogue
* Quest dialogue
* Dialogue rendering
* Dialogue pagination
* Automatic text wrapping
* LuxDialogues API usage
* Reusable NPC dialogue registration
* Future ChatGPT sessions

This document should be provided in future sessions when continuing dialogue-related development.

---

# Current System Overview

LevelPlugin now uses LuxDialogues as the primary dialogue renderer.

The old dialogue system still exists as a fallback, but LuxDialogues is now the intended architecture.

Current flow:

```text
Player right-clicks NPC
        ↓
NPCClickListener
        ↓
NPCDialogManager
        ↓
LuxNpcDialogueService
        ↓
LuxDialogues API
        ↓
Dialogue rendered in-game
```

---

# Important Design Goal

The system is intentionally generic.

NPCs should NOT manually create dialogue inline anymore.

Instead:

* NPCs register dialogue definitions
* The dialogue service handles rendering
* Wrapping/pagination are automatic
* Interaction handling is centralized

---

# Current Package Structure

Recommended structure:

```text
me.nakilex.levelplugin.luxdialogues
```

Current/Recommended files:

```text
LuxNpcDialogueService.java
LuxNpcDialogueChoice.java
LuxDialogueUtils.java
LuxDialoguePagination.java
LuxDialogueTextWrapper.java
LuxDialogueRegistry.java
```

NPC-related:

```text
npc/
    NPCClickListener.java
    NPCDialogManager.java
```

Quest-related:

```text
quest/
    QuestManager.java
    QuestDialogueHandler.java
```

---

# Required plugin.yml Setup

Inside plugin.yml:

```yml
softdepend:
  - LuxDialogues
```

Recommended over `depend`.

This prevents startup failure if LuxDialogues is missing.

---

# API Package

Use:

```java
org.aselstudios.luxdialoguesapi.*
```

DO NOT rely heavily on:

```java
org.aselstudios.luxdialogues.*
```

Those are internal classes and may change.

---

# Current Dialogue Rendering Rules

## Current Limits

```java
MAX_CHARS_PER_LINE = 32;
MAX_LINES_PER_PAGE = 4;
```

These values were chosen based on:

* current dialogue PNG width
* Minecraft font width
* readability
* avoiding overflow

---

# IMPORTANT: Minecraft Fonts Are Variable Width

Character count is only an approximation.

Example:

* `iiiiiiiiiiii`
* `WWWWWWWWWW`

Both have different pixel widths.

32 chars is considered safe for:

* lowercase dialogue
* regular sentence structure
* minimal formatting

If using:

* lots of uppercase
* custom glyphs
* icons
* formatting codes

Reduce line length manually.

---

# Automatic Text Wrapping

The system automatically wraps dialogue text.

Example:

Input:

```text
Nothing is truly worthless. Even rusted blades still have a purpose.
```

Output:

```text
Nothing is truly worthless.
Even rusted blades still
have a purpose.
```

The wrapper:

* preserves words
* avoids splitting words
* respects sentence punctuation
* starts new sentences on cleaner lines

---

# Sentence-Aware Wrapping

The wrapper specifically checks for:

```text
.
!
?
```

After punctuation:

* the next sentence prefers starting on a new line
* avoids ugly formatting like:

BAD:

```text
worthless. Even rusted
```

GOOD:

```text
worthless.
Even rusted blades still
```

---

# Automatic Pagination

If dialogue exceeds:

```java
MAX_LINES_PER_PAGE = 4;
```

The service automatically:

* creates a new LuxDialogue page
* inserts Continue options
* links pages together

This prevents dialogue overflow outside the dialogue box.

---

# Example Pagination

Input:

```text
The kingdom has fallen. Our walls are broken. The creatures now roam freely through the old roads.
```

Result:

## Page 1

```text
The kingdom has fallen.
Our walls are broken.
The creatures now roam
freely through the old
```

Choice:

```text
Continue
```

## Page 2

```text
roads.
```

---

# IMPORTANT: Interaction Forwarding

This was a major bug previously.

While inside a LuxDialogue:

DO NOT reopen the dialogue.

Instead:

```java
LuxDialoguesAPI.getProvider().triggerInteraction(player);
```

Without this:

* dialogue restarts endlessly
* choices cannot be selected
* pages never progress

Current behavior:

* right-click advances dialogue
* right-click selects choices
* NPC click does NOT restart active dialogue

---

# Checking Active Dialogue

Use:

```java
LuxDialoguesAPI.getProvider().isInDialogue(player)
```

Useful for:

* preventing duplicate dialogues
* movement locking
* combat restrictions
* inventory locking

---

# Closing Dialogue

Use:

```java
LuxDialoguesAPI.getProvider().clearDialogue(player);
```

---

# Required Builder Fields

LuxDialogues crashes if these are missing.

Required:

```java
.setDialogueBackgroundImage(...)
.setDialogueBackgroundImageColor(...)
.setDialogueBackgroundImageOffset(...)

.setNameStartImage(...)
.setNameMidImage(...)
.setNameEndImage(...)
.setNameImageColor(...)

.setTypingSound(...)
.setTypingSoundVolume(...)
.setTypingSoundPitch(...)

.setSelectionSound(...)

.setAllowedRange(...)
```

If missing:

* builder errors occur
* range listener crashes
* dialogue fails entirely

---

# Current Base Dialogue Builder

Recommended reusable builder:

```java
public static Dialogue.Builder createBaseDialogue(
        String id,
        String character
) {

    return new Dialogue.Builder()
            .setDialogueID(id)

            .setCharacterNameText(character, "#ffffff", 0)

            .setDialogueBackgroundImage("dialogue")
            .setDialogueBackgroundImageColor("#ffffff")
            .setDialogueBackgroundImageOffset(0)

            .setNameStartImage("name_start")
            .setNameMidImage("name_middle")
            .setNameEndImage("name_end")
            .setNameImageColor("#ffffff")

            .setTypingSound("minecraft:block.note_block.hat")
            .setTypingSoundVolume(1.0f)
            .setTypingSoundPitch(1.0f)

            .setSelectionSound("minecraft:block.note_block.pling")

            .setAllowedRange(10.0);
}
```

---

# Generic NPC Dialogue Architecture

The system now supports reusable dialogue registration.

Example concept:

```java
dialogueManager.register(
    "Storage Manager",

    LuxNpcDialogueDefinition.builder()
        .characterName("Storage Manager")

        .lines(List.of(
            "Looking to keep your belongings safe?",
            "I can register a personal storage for you for 100 coins."
        ))

        .choice("Yes", player -> {
            storageManager.register(player);
        })

        .choice("No", player -> {})

        .build()
);
```

This avoids:

* duplicated listeners
* duplicated wrapping logic
* duplicated page handling

---

# Recommended Dialogue Flow

## NPC Flow

```text
Player clicks NPC
        ↓
NPCClickListener
        ↓
NPCDialogManager
        ↓
Quest/State checks
        ↓
Dialogue generated
        ↓
LuxNpcDialogueService
        ↓
LuxDialogues API
```

---

# Quest Dialogue Structure

Recommended:

```text
quest_id_intro
quest_id_accept
quest_id_progress
quest_id_complete
quest_id_failure
```

Example:

```text
blacksmith_intro
blacksmith_accept
blacksmith_progress
blacksmith_complete
```

---

# Dialogue Factory Pattern

DO NOT inline large dialogue blocks inside listeners.

BAD:

```java
@EventHandler
public void onClick(...) {

    Dialogue dialogue = ...
}
```

GOOD:

```java
DialogueFactory.createBlacksmithIntro()
```

Recommended:

```text
DialogueFactory
QuestDialogueFactory
NpcDialogueFactory
```

---

# Triggering Dialogue From NPCs

Example:

```java
@EventHandler
public void onNpcClick(NPCRightClickEvent event) {

    Player player = event.getClicker();
    NPC npc = event.getNPC();

    if (!npc.getName().equalsIgnoreCase("Guard")) {
        return;
    }

    Dialogue dialogue =
            DialogueFactory.createGuardDialogue();

    LuxDialoguesAPI.getProvider()
            .sendDialogue(player, dialogue, "start");
}
```

---

# Triggering Dialogue From Quests

Example:

```java
if (!playerData.hasQuest("guard_quest")) {

    LuxDialoguesAPI.getProvider()
            .sendDialogue(
                    player,
                    DialogueFactory.createGuardIntro(),
                    "start"
            );

    return;
}
```

---

# Triggering Dialogue From Regions

Example:

```java
if (enteredDungeonRegion) {

    LuxDialoguesAPI.getProvider()
            .sendDialogue(
                    player,
                    DialogueFactory.createDungeonWarning(),
                    "start"
            );
}
```

---

# Dialogue Choices

Example:

```java
.addAnswer(
    new Answer.Builder()
        .setAnswerID("yes")
        .setAnswerText("Accept quest")
        .setGoTo(List.of("accept"))
        .build()
)
```

---

# Multiple Pages

Example:

```java
.addPage(
    new Page.Builder()
        .setID("intro")
        .addLine("Hello.")
        .addAnswer(...)
        .build()
)

.addPage(
    new Page.Builder()
        .setID("accept")
        .addLine("Quest accepted.")
        .build()
)
```

---

# Current Known LuxDialogues Issues

Observed:

* pixel-width inaccuracies
* undocumented builder requirements
* internal API instability
* null crashes if range missing
* rendering quirks
* spacing inconsistencies
* occasional glyph overflow

Because of this:

* keep dialogue concise
* wrap manually when needed
* avoid huge paragraphs
* avoid extremely long words

---

# Recommended Future Improvements

## Strongly Recommended

Add:

* YAML dialogue definitions
* localization
* portrait swapping
* emotion states
* cinematic camera integration
* PlaceholderAPI support
* async loading
* dialogue conditions
* dialogue actions
* quest branching
* persistent dialogue states

---

# Suggested YAML System

Future idea:

```yml
id: guard_intro

character: Guard

pages:
  - id: start

    lines:
      - "Halt traveler."
      - "State your business."

    answers:
      - id: accept
        text: "I need help."
        goto: help
```

Then dynamically:

* parse YAML
* build LuxDialogue objects
* register automatically

This is highly recommended long-term.

---

# Recommended Long-Term Architecture

Recommended future structure:

```text
DialogueRegistry
DialogueFactory
DialogueUtils
QuestDialogueManager
NpcDialogueManager
DialogueConditions
DialogueActions
DialoguePersistence
DialogueEvents
```

---

# Current Philosophy

LuxDialogues should be treated as:

* a renderer
* a dialogue UI framework

NOT:

* business logic
* quest state manager
* NPC manager

Your plugin should own:

* quest logic
* rewards
* progression
* persistence
* conditions

LuxDialogues should only display and interact.

---

# Recommended Workflow For Future ChatGPT Sessions

When continuing development:

Provide:

1. This MD file
2. Current plugin zip
3. Relevant assets if changed

Then explain:

* which NPC
* which quest
* desired flow
* rewards
* conditions
* current issues

Then request:

* implementation
* refactors
* factories
* YAML systems
* new dialogue types
* quest integration
* cinematic support

This gives future sessions immediate architectural context.

---

# Final Recommendation

DO NOT recreate the entirety of LuxDialogues.

Use the API.

Advantages:

* less maintenance
* easier upgrades
* better UI consistency
* faster development
* cleaner architecture
* reusable systems

Only replace LuxDialogues if:

* API becomes unstable
* rendering limitations become severe
* customization becomes impossible
* performance becomes problematic
