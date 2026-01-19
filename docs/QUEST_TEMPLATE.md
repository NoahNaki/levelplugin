# Quest Request Template

Use this template to provide everything we need to build a quest in the current system. Please be as specific as possible so we can reproduce your quest accurately. If something is not applicable, write `N/A`.

---

## 1) Quest Overview
- **Quest ID (unique, snake/lowercase):**
- **Quest name (player-facing):**
- **Short description (1–2 sentences):**
- **Quest type:** Main / Side / Tutorial / Daily / Weekly
- **Repeatable?:** One-time / Daily / Weekly
- **Minimum level requirement:**
- **Class requirement (if any):**
- **Prerequisite quests (IDs):**
- **Should quest show the quest-giver location in UI?:** Yes / No
- **Should objectives be sequential?:** Yes / No

## 2) Quest Giver & NPCs
- **Primary quest-giver NPC:**
  - Name (exact in-game name):
  - NPC ID (if known):
  - Location / spawn coordinates:
  - Should be highlighted on the map / waypoint?: Yes / No
- **Additional NPCs involved:**
  - Name + role + NPC ID (if known) + location:
- **NPC visibility rules:**
  - Should any NPCs be hidden or despawned after completion?:
  - Should any NPCs only appear while the quest is active?:
  - Should any NPCs swap dialog based on progress?:

## 3) Story & Player Flow
- **Narrative summary (what is the player doing and why?):**
- **Key beats (ordered list):**
  1.
  2.
  3.
- **Player guidance / UX notes:** (e.g., “show beacon to NPC”, “give hint in chat”, “pop tutorial tooltip”)

## 4) Dialog Script
> Provide the **exact text** for dialog lines. If there are multiple stages, list them clearly.

- **Intro dialog (quest start):**
  - Line 1:
  - Line 2:
  - ...
- **Mid-quest dialog (optional):**
  - Trigger condition (e.g., after objective 1):
  - Lines:
- **Completion dialog:**
  - Lines:
- **Alternate dialog states:**
  - If player hasn’t met requirements:
  - If quest already completed:
  - If quest is active and player revisits NPC:

## 5) Objectives (List Each Objective)
> For each objective, specify type, target, amount, and any navigation or display info.

**Objective #1**
- **Type:** (KILL / COLLECT / INTERACT / BUY / UPGRADE / CAST / CRAFT / DUEL / ESCORT / TALK / EXPLORE / SELECT_CLASS / ESSENCE_SWAP / ENCHANT / DISCOVER / CONSUME_POTION / PLAY_TIME / AUCTION_BUY / AUCTION_LIST / AUCTION_SELL / AUCTION_BID / TOWN_UPGRADE / BLACKSMITH_SERVICE / BLACKSMITH_REPAIR / BLACKSMITH_REROLL / SALVAGE / WAYSTONE_UNLOCK / WAYSTONE_USE / CAST_COMBO / DUEL_PARTICIPATE / DUEL_LOSE / LOOTCHEST_OPEN / SIEGE_PARTICIPATE / DUEL_WIN / ARENA_MATCH / DUNGEON_CREATE / DUNGEON_COMPLETE / CAPTURE_FISH / GATHER_CROPS)
- **Target (exact mob/item/NPC/event key):**
- **Amount required:**
- **Allow overflow progress?:** Yes / No
- **Objective description override (if needed for UI):**
- **Navigation beacon target:**
  - None / NPC name / NPC ID / Location
  - If location: provide coordinates + world

**Objective #2** (repeat fields as needed)

## 6) Talk/Interact Targets
> If any objectives rely on “talk” or “interact” targets, list the IDs or keywords we should register.
- **Target key(s):**
- **Associated NPC name (exact):**

## 7) Rewards
- **XP:**
- **Coins:**
- **Gems:**
- **Item rewards (IDs or exact item names):**
- **Class unlocks (if any):**
- **Custom reward text lines (if any):**

## 8) World & Location Details
- **Quest areas / zones involved:**
- **Exact coordinates for travel points, triggers, or exploration zones:**
- **Any restricted areas or phasing notes:**

## 9) Custom Behavior & Scripts (If Needed)
> If the quest needs unique behavior beyond objectives and dialog, describe it clearly.
- **On quest start behavior:**
- **On quest completion behavior:**
- **On quest reset behavior:**
- **Special mechanics or minigames:**
- **Timers / cooldowns:**
- **Flags or branching logic:** (describe how we should store player state)

## 10) Post-Completion Behavior
- **Should NPCs change dialog after completion?:**
- **Should quest-related NPCs hide/despawn after completion?:**
- **Should quest items be removed?:**
- **Should any gates/doors unlock or remain open?:**
- **Any persistent world changes?:**

## 11) QA Checklist
- **Happy path success criteria:**
- **Known edge cases:** (e.g., player leaves area, disconnects mid-dialog, abandons quest)
- **Localization or tone notes:**
- **Additional references:** (existing quests that feel similar)

---

### Example (Optional)
> If helpful, include a filled-out example using this template.
