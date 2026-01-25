# Phase 0 Inventory / Ground Truth

## 1) GUI inventory (current implementations)

| GUI | Class/Package | Purpose | Common Features (pagination / filter / sort / search / confirm / back) |
| --- | --- | --- | --- |
| Storage | `me.nakilex.levelplugin.storage.gui.StorageGUI` | Player storage with unlockable pages. | Pagination (prev/next); filter (filterMode); sort (sortMode); confirm (page unlock confirm). |
| Essence Weaver | `me.nakilex.levelplugin.player.classes.essence.gui.ClassEssenceUpgradeGUI` | Essence invest/star upgrade/reseal flow. | Confirm (check button per mode); back/navigation (left/right arrows to swap modes). |
| Enchant | `me.nakilex.levelplugin.enchanting.gui.EnchantGUI` | Apply enchant/prefix to custom items/tools. | Confirm (Enchant button). |
| Blacksmith | `me.nakilex.levelplugin.blacksmith.gui.BlacksmithGUI` | Upgrade/repair/reroll custom items. | Confirm (upgrade/repair/reroll buttons); back/navigation (left/right arrows between modes). |
| Merchant | `me.nakilex.levelplugin.merchants.gui.MerchantGUI` | Merchant inventory for buying items/tools/essences. | None of the listed features (static inventory). |
| Horse | `me.nakilex.levelplugin.horse.gui.HorseGUI` | Horse stats + reroll. | Confirm (reroll button). |
| Salvager | `me.nakilex.levelplugin.salvage.gui.SalvageGUI` | Salvage items into currencies. | Confirm (check); back/cancel (cross). |
| Life Skill Reward | `me.nakilex.levelplugin.player.attributes.gui.LifeSkillRewardsGUI` | Track + claim life skill rewards. | Pagination (prev/next); back (back button). |
| Arena | `me.nakilex.levelplugin.arena.gui.ArenaQueueGUI` | Join/leave arena queue. | None of the listed features (single-screen). |
| Auction House | `me.nakilex.levelplugin.auctionhouse.AuctionHouseGUI` | Browse, search, filter, and buy listings. | Pagination (prev/next); filter (level + rarity); sort; search; confirm (purchase confirmation); back (listings -> main). |
| Codex | `me.nakilex.levelplugin.codex.CodexMainGUI` + `MobCodexGUI`/`NpcCodexGUI`/`LocationCodexGUI` | Category hub + codex browsing. | Pagination (sub-GUIs); back (main + sub menus). |
| Guild | `me.nakilex.levelplugin.guild.GuildMemberGUI` (+ `GuildGUI` for list) | Guild roster management. | Pagination; search; sort; confirm (role change confirmation). |
| Battle Pass | `me.nakilex.levelplugin.player.battlepass.gui.BattlePassGUI` | Reward track pages. | Pagination (prev/next). |
| Fast Travel | `me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI` | Browse gates and teleport. | Pagination (prev/next); sort; filter. |

## 2) Item system (IDs, tooltips, v1 schema)

### Item IDs on ItemStacks (PDC keys)
Custom items are tagged with PDC keys under `ItemUtil`, including:
- `custom_item_id` (template/definition ID)
- `custom_item_uuid` (instance UUID)
- `upgrade_level`
- `custom_item_durability`
- `template_material`
- `nexo_model`
- `soulbound`
- `dungeon_item`

### Tooltip/lore generation
Custom item tooltips are rebuilt in `ItemUtil.updateCustomItemTooltip`, which:
- Reads custom item IDs and instances from PDC.
- Applies rarity styling, class/level requirement checks, gear score, stats, and durability lines.
- Uses `GuiUtil` for glyph/stat formatting, and `TooltipUtil` for shared bullet/progress/click UX patterns in other GUIs.

### Items YAML schema v1 (items.yml)
`ItemManager` reads from `items.yml` using the schema:
```
items:
  <numeric_id>:
    name: <string>
    rarity: <COMMON/UNCOMMON/RARE/...>
    level_requirement: <int>
    class_requirement: <string>
    material: <Bukkit Material>
    hp: "min-max"
    def: "min-max"
    str: "min-max"
    agi: "min-max"
    intel: "min-max"
    dex: "min-max"
    wil: "min-max"
    tec: "min-max"
```
Stat ranges are parsed via `StatRange.fromString` and normalized per armor/rarity rules.

### Item instance persistence (custom_items.yml)
`ItemConfig` persists per-instance data (UUID-based keys) including ID, name, rarity, level requirement, class requirement, material, rolled stat values, upgrade level, enchant count, and current durability.

## 3) Loot/mob drop systems

### Default XP/coin formula
`CombatRewardCalculator` defines:
- XP = 10% of combat power (rounded by tens).
- Coins = 5% of combat power (rounded).

### Loot selection + application
`MobRewardService`:
- Computes XP/coins via `CombatRewardCalculator`.
- Reads mob reward nodes from `mob_rewards.yml` and delegates to `ItemDropper`.
- Optionally rolls a loot chest item via `LootChestManager`.

`ItemDropper`:
- Reads `items` entries under a mob node and applies `drop_rate` + `quantity` ranges.
- Generates a custom item via `ItemManager.generateItemForGearScore` and applies a model from `ModelSetManager` (modelSet -> Nexo model id).
- Handles optional essence drops and rare reroll scrolls.

### Custom mob definition persistence
`CustomMobDefinition.fromConfig` parses `custom_mobs/*.yml` with fields such as:
- `id`, `type`, `display`, `level` or `level-range`
- `health`, `boss`
- `models` (list) or `model` (single)
- `stats` (vitality/strength/agility/intelligence/dexterity/will/technique)
- `options` (movement-speed, follow-range, knockback-resistance, attack-damage, attack-speed, ai, silent, despawn)

Example `custom_mobs/cursed_archer.yml` demonstrates these keys, including `models`, `stats`, and `options`.
