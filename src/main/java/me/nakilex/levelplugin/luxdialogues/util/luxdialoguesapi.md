# LuxDialogues API Integration Guide For LevelPlugin

## Purpose

This document explains how to integrate and use the LuxDialogues API inside LevelPlugin.

The goal is:

* Trigger LuxDialogues from our own plugin
* Create dialogues dynamically in Java
* Trigger dialogues from NPCs, quests, regions, commands, etc.
* Reuse this document in future ChatGPT sessions with no prior context

This guide assumes:

* Paper/Spigot server
* Java plugin development
* LuxDialogues plugin installed on the server
* LevelPlugin as the primary plugin

---

# 1. What We Learned

LuxDialogues exposes a usable API.

The important package is:

```java
org.aselstudios.luxdialoguesapi
```

DO NOT use internal LuxDialogues classes unless absolutely necessary.

Avoid:

```java
org.aselstudios.luxdialogues.Dialogues.*
```

Use:

```java
org.aselstudios.luxdialoguesapi.*
```

The API already supports:

* Sending dialogues
* Dialogue builders
* Pages
* Answers
* Triggering interactions
* Clearing dialogue sessions
* Dialogue state checks

---

# 2. Required plugin.yml Setup

Inside LevelPlugin's `plugin.yml`:

```yml
softdepend:
  - LuxDialogues
```

or if mandatory:

```yml
depend:
  - LuxDialogues
```

Recommended:

```yml
softdepend:
  - LuxDialogues
```

This prevents startup failure if LuxDialogues is missing.

---

# 3. Checking If LuxDialogues Exists

Always validate before using the API.

Example:

```java
Plugin plugin = Bukkit.getPluginManager().getPlugin("LuxDialogues");

if (plugin == null || !plugin.isEnabled()) {
    player.sendMessage("LuxDialogues is not installed.");
    return;
}
```

---

# 4. Recommended Project Structure

Recommended package:

```text
me.nakilex.levelplugin.luxdialogues
```

Recommended files:

```text
LuxDialoguesBridge.java
DialogueFactory.java
DialogueUtils.java
QuestDialogueHandler.java
NpcDialogueListener.java
```

---

# 5. Basic API Usage

## Sending a dialogue

Example:

```java
LuxDialoguesAPI.getProvider().sendDialogue(player, dialogue, "start");
```

Arguments:

* player
* dialogue object
* starting page id

---

# 6. Basic Dialogue Example

## Minimal Working Dialogue

```java
Dialogue dialogue = new Dialogue.Builder()
        .setDialogueID("test_dialogue")

        .setCharacterNameText("Guard", "#ffffff", 0)

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

        .setAllowedRange(10.0)

        .addPage(
                new Page.Builder()
                        .setID("start")
                        .addLine("Welcome to the village.")
                        .addLine("The LuxDialogues API is working.")

                        .addAnswer(
                                new Answer.Builder()
                                        .setAnswerID("continue")
                                        .setAnswerText("Continue")
                                        .setGoTo(List.of("next"))
                                        .build()
                        )

                        .build()
        )

        .addPage(
                new Page.Builder()
                        .setID("next")
                        .addLine("Good luck on your journey.")
                        .build()
        )

        .build();
```

---

# 7. IMPORTANT: Required Builder Fields

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

* Builder errors occur
* RangeListener crashes
* Dialogue fails to open

---

# 8. Text Wrapping Rules

LuxDialogues does NOT properly constrain long lines automatically.

Recommended safe line length:

```text
28-30 characters
```

DO NOT rely on automatic wrapping.

BAD:

```java
.addLine("This is an extremely long sentence that may overflow outside the dialogue box.")
```

GOOD:

```java
.addLine("This is a safer line.")
.addLine("It stays inside the box.")
```

Best practice:

* 28-30 visible chars
* Split manually
* Keep sentences short
* Prefer multiple lines

---

# 9. Manual Wrapping Utility

Recommended helper:

```java
public static List<String> wrapDialogue(String text, int maxChars)
```

Suggested default:

```java
maxChars = 30;
```

Goals:

* Word-aware wrapping
* No broken words
* Consistent dialogue width

---

# 10. Dialogue Utility Class

Recommended reusable utility:

```java
public class DialogueUtils {

    public static Dialogue.Builder createBaseDialogue(String id, String character) {

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
}
```

This avoids repeating boilerplate.

---

# 11. Triggering Dialogue From NPCs

## Example Flow

1. Player right-clicks NPC
2. Plugin detects NPC
3. Dialogue gets created
4. Dialogue sent to player

---

## Citizens Example

```java
@EventHandler
public void onNpcClick(NPCRightClickEvent event) {

    Player player = event.getClicker();
    NPC npc = event.getNPC();

    if (!npc.getName().equalsIgnoreCase("Guard")) {
        return;
    }

    Dialogue dialogue = DialogueFactory.createGuardDialogue();

    LuxDialoguesAPI.getProvider()
            .sendDialogue(player, dialogue, "start");
}
```

---

# 12. Quest Dialogue Structure

Recommended structure:

```text
Quest ID
 └── Intro Dialogue
 └── Accept Dialogue
 └── Progress Dialogue
 └── Completion Dialogue
 └── Failure Dialogue
```

Example:

```text
blacksmith_intro
blacksmith_accept
blacksmith_progress
blacksmith_complete
```

---

# 13. Recommended Dialogue Factory Pattern

Instead of creating dialogues inline:

BAD:

```java
event code + dialogue code together
```

GOOD:

```java
DialogueFactory.createBlacksmithIntro()
```

Example:

```java
public class DialogueFactory {

    public static Dialogue createGuardDialogue() {

        return DialogueUtils.createBaseDialogue(
                "guard_intro",
                "Guard"
        )

        .addPage(...)
        .build();
    }
}
```

This keeps dialogue organized.

---

# 14. Triggering Dialogue From Quests

Example:

```java
if (!playerData.hasQuest("guard_quest")) {

    LuxDialoguesAPI.getProvider()
            .sendDialogue(player,
                    DialogueFactory.createGuardIntro(),
                    "start");

    return;
}
```

---

# 15. Triggering Dialogue From Regions

Example:

```java
if (enteredDungeonRegion) {

    LuxDialoguesAPI.getProvider()
            .sendDialogue(player,
                    DialogueFactory.createDungeonWarning(),
                    "start");
}
```

---

# 16. Dialogue Choices

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

# 17. Multiple Pages

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

# 18. Closing Dialogues

```java
LuxDialoguesAPI.getProvider().clearDialogue(player);
```

---

# 19. Checking If Player Is In Dialogue

```java
LuxDialoguesAPI.getProvider().isInDialogue(player)
```

Useful for:

* preventing movement
* disabling combat
* preventing inventory access

---

# 20. Trigger Interaction

```java
LuxDialoguesAPI.getProvider().triggerInteraction(player);
```

Can simulate:

* advancing pages
* continuing dialogue
* interaction events

---

# 21. Recommended Long-Term Architecture

Recommended final architecture:

```text
DialogueRegistry
DialogueFactory
DialogueUtils
QuestDialogueManager
NpcDialogueManager
DialogueConditions
DialogueActions
```

---

# 22. Recommended Future Improvements

## Add:

* automatic text wrapping
* localization
* placeholder support
* animation control
* quest conditions
* async loading
* JSON/YAML dialogue definitions
* cinematic dialogue support
* portrait swapping
* emotion states

---

# 23. Suggested Dialogue YAML System

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

Then convert YAML -> LuxDialogue object dynamically.

This is strongly recommended long-term.

---

# 24. Known LuxDialogues Issues

Observed:

* Text overflow
* Required builder fields undocumented
* Null range crashes
* Internal APIs unstable
* Some visuals hardcoded
* Pixel width handling imperfect

Because of this:

* Wrap text manually
* Use helper builders
* Keep dialogue short

---

# 25. Recommended Workflow For Future ChatGPT Sessions

When starting a new ChatGPT session:

1. Provide this MD file
2. Provide current LevelPlugin zip
3. State:

    * what NPC
    * what quest
    * desired dialogue flow
    * conditions/rewards
    * if Citizens/MythicMobs involved

Then request:

* dialogue implementation
* factory creation
* event hooks
* commands
* YAML support
* wrapping utility

This gives future sessions enough context immediately.

---

# 26. Example Final Quest Flow

```text
Player clicks Guard NPC
    ↓
Quest check
    ↓
No quest?
    ↓
Open intro dialogue
    ↓
Player accepts
    ↓
Quest starts
    ↓
Progress dialogue changes
    ↓
Quest completed
    ↓
Completion dialogue plays
```

---

# 27. Final Recommendation

DO NOT recreate the entirety of LuxDialogues.

Use the API instead.

Advantages:

* less maintenance
* easier updates
* less broken UI logic
* faster development
* cleaner architecture

Only recreate parts if:

* API limitations become severe
* performance issues appear
* rendering customization becomes impossible
* you want total UI control
