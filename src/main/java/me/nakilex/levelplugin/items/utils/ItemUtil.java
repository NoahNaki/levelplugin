package me.nakilex.levelplugin.items.utils;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.tools.ToolTier;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.player.mining.managers.MiningManager;
import me.nakilex.levelplugin.ego.EgoRarity;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemUtil {

    public static final NamespacedKey UPGRADE_LEVEL_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ItemUtil.class), "upgrade_level");
    public static final NamespacedKey ITEM_ID_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ItemUtil.class), "custom_item_id");
    public static final NamespacedKey ITEM_UUID_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ItemUtil.class), "custom_item_uuid");
    public static final NamespacedKey DURABILITY_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ItemUtil.class), "custom_item_durability");
    public static final NamespacedKey EGO_ID_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ItemUtil.class), "ego_weapon_id");
    public static final NamespacedKey EGO_RANK_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ItemUtil.class), "ego_weapon_rank");
    public static final NamespacedKey EGO_EXP_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ItemUtil.class), "ego_weapon_exp");
    public static final NamespacedKey EGO_RARITY_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ItemUtil.class), "ego_weapon_rarity");

    private static final int PREFIX_BONUS = 20;
    private static final java.util.Map<String, StatsManager.StatType> PREFIX_MAP = new java.util.HashMap<>();
    private static final java.util.List<String> PREFIX_LIST = new java.util.ArrayList<>();

    static {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(
                new java.io.File(JavaPlugin.getProvidingPlugin(ItemUtil.class).getDataFolder(), "prefixes.yml"));
        for (String key : cfg.getKeys(false)) {
            String prefix = cfg.getString(key);
            if (prefix == null) continue;
            StatsManager.StatType st = switch (key.toLowerCase()) {
                case "strength" -> StatsManager.StatType.STR;
                case "agility" -> StatsManager.StatType.AGI;
                case "dexterity" -> StatsManager.StatType.DEX;
                case "intelligence" -> StatsManager.StatType.INT;
                case "defense" -> StatsManager.StatType.DEF;
                case "hp" -> StatsManager.StatType.HP;
                default -> StatsManager.StatType.DEF;
            };
            PREFIX_MAP.put(prefix, st);
            PREFIX_LIST.add(prefix);
        }
    }

    private static String parsePrefix(String name) {
        for (String p : PREFIX_LIST) {
            if (name.startsWith(p + " ")) return p;
            if (name.equals(p)) return p;
        }
        return null;
    }


    /**
     * Creates an ItemStack from a CustomItem while including dynamic tooltip information.
     *
     * @param cItem  The custom item data.
     * @param amount The number of items to create.
     * @param player The player viewing the item (can be null if no context is available).
     * @return The created ItemStack.
     */
    public static ItemStack createItemStackFromCustomItem(CustomItem cItem, int amount, Player player) {
        Material mat = cItem.getMaterial();
        ItemStack stack = new ItemStack(mat, amount);

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        // Set display name with rarity color and upgrade stars.
        ChatColor rarityColor = cItem.getRarity().getColor();
        String stars = "<glyph:star>".repeat(cItem.getUpgradeLevel());
        meta.setDisplayName(rarityColor + cItem.getBaseName() + " " + stars);

        List<String> lore = new ArrayList<>();
        // Glyph line under the name to show rarity and item type
        String rarityGlyph = "<glyph:" + cItem.getRarity().name().toLowerCase() + ">";
        String typeGlyph = "<glyph:tool>";
        if (me.nakilex.levelplugin.items.data.ArmorType.matchType(stack) != null) {
            typeGlyph = "<glyph:armor>";
        } else if (me.nakilex.levelplugin.items.data.WeaponType.matchType(stack) != null) {
            typeGlyph = "<glyph:weapon>";
        }
        lore.add(rarityGlyph + typeGlyph);
        lore.add(""); // Blank line for spacing

        // If this item is an Ego Weapon, show rank progress before stats
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(EGO_RANK_KEY, PersistentDataType.INTEGER)) {
            int rank = pdc.getOrDefault(EGO_RANK_KEY, PersistentDataType.INTEGER, 1);
            int exp  = pdc.getOrDefault(EGO_EXP_KEY,  PersistentDataType.INTEGER, 0);
            me.nakilex.levelplugin.ego.EgoRarity rarity = me.nakilex.levelplugin.ego.EgoRarity.COMMON;
            if (pdc.has(EGO_RARITY_KEY, PersistentDataType.STRING)) {
                try {
                    rarity = me.nakilex.levelplugin.ego.EgoRarity.valueOf(pdc.get(EGO_RARITY_KEY, PersistentDataType.STRING));
                } catch (Exception ignored) {}
            }
            int expToNext = 100 * rank * rarity.getXpMultiplier();
            double pct = expToNext > 0 ? (exp * 100.0 / expToNext) : 0.0;
            if (pct > 100.0) pct = 100.0;
            pct = Math.round(pct * 10.0) / 10.0;
            lore.add(ChatColor.GRAY + "Rank: " + ChatColor.YELLOW + rank + ChatColor.GRAY + "/10");
            lore.add(ChatColor.GRAY + "Progress to Level " + ChatColor.YELLOW + (rank + 1) + ChatColor.GRAY + ": " + ChatColor.YELLOW + pct + "%");
            int barLen = 15;
            int filled = (int) Math.round(pct / 100.0 * barLen);
            String bar = ChatColor.GREEN + "" + "-".repeat(Math.max(filled,0)) + ChatColor.WHITE + "" + "-".repeat(Math.max(barLen - filled,0));
            lore.add(bar + " " + ChatColor.YELLOW + exp + ChatColor.GOLD + "/" + ChatColor.YELLOW + expToNext);
            lore.add("");
        }

        if (!pdc.has(EGO_ID_KEY, PersistentDataType.STRING)) {
            // --- Level Requirement ---
            int playerLevel = (player != null) ? LevelManager.getInstance().getLevel(player) : 0;
            String levelRequirementLine;
            if (player == null) {
                levelRequirementLine = ChatColor.GRAY + "Level Requirement: " + cItem.getLevelRequirement();
            } else if (playerLevel < cItem.getLevelRequirement()) {
                levelRequirementLine = ChatColor.RED + "✘ " + ChatColor.GRAY + "Level Requirement: " + ChatColor.WHITE + cItem.getLevelRequirement();
            } else {
                levelRequirementLine = ChatColor.GREEN + "✔ " + ChatColor.GRAY + "Level Requirement: " + ChatColor.WHITE + cItem.getLevelRequirement();
            }
            lore.add(levelRequirementLine);
            lore.add(""); // Another blank line for spacing
        }

        // --- Stats Information ---
        String prefix = parsePrefix(cItem.getBaseName());
        StatsManager.StatType prefixStat = prefix != null ? PREFIX_MAP.get(prefix) : null;
        if (cItem.getHp() != 0) {
            String line = ChatColor.RED + "❤ " + ChatColor.GRAY + "Health: " + ChatColor.RED + "+" + cItem.getHp();
            if (prefixStat == StatsManager.StatType.HP) line += ChatColor.LIGHT_PURPLE + " (" + "+" + PREFIX_BONUS + ")";
            lore.add(line);
        }
        if (cItem.getDef() != 0) {
            String line = ChatColor.GRAY + "⛂ " + ChatColor.GRAY + "Defence: " + ChatColor.WHITE + "+" + cItem.getDef();
            if (prefixStat == StatsManager.StatType.DEF) line += ChatColor.LIGHT_PURPLE + " (" + "+" + PREFIX_BONUS + ")";
            lore.add(line);
        }
        if (cItem.getStr() != 0) {
            String line = ChatColor.BLUE + "☠ " + ChatColor.GRAY + "Strength: " + ChatColor.WHITE + "+" + cItem.getStr();
            if (prefixStat == StatsManager.StatType.STR) line += ChatColor.LIGHT_PURPLE + " (" + "+" + PREFIX_BONUS + ")";
            lore.add(line);
        }
        if (cItem.getAgi() != 0) {
            String line = ChatColor.GREEN + "≈ " + ChatColor.GRAY + "Agility: " + ChatColor.WHITE + "+" + cItem.getAgi();
            if (prefixStat == StatsManager.StatType.AGI) line += ChatColor.LIGHT_PURPLE + " (" + "+" + PREFIX_BONUS + ")";
            lore.add(line);
        }
        if (cItem.getIntel() != 0) {
            String line = ChatColor.AQUA + "♦ " + ChatColor.GRAY + "Intelligence: " + ChatColor.WHITE + "+" + cItem.getIntel();
            if (prefixStat == StatsManager.StatType.INT) line += ChatColor.LIGHT_PURPLE + " (" + "+" + PREFIX_BONUS + ")";
            lore.add(line);
        }
        if (cItem.getDex() != 0) {
            String line = ChatColor.YELLOW + "➹ " + ChatColor.GRAY + "Dexterity: " + ChatColor.WHITE + "+" + cItem.getDex();
            if (prefixStat == StatsManager.StatType.DEX) line += ChatColor.LIGHT_PURPLE + " (" + "+" + PREFIX_BONUS + ")";
            lore.add(line);
        }

        lore.add("");
        if (cItem.isBroken()) {
            lore.add(ChatColor.GRAY + "Durability: " + ChatColor.RED + ChatColor.BOLD + "BROKEN");
        } else {
            lore.add(ChatColor.GRAY + "Durability: "
                + ChatColor.WHITE + cItem.getCurrentDurability()
                + "/" + cItem.getMaxDurability());
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); // Hide item attributes
        meta.setUnbreakable(true); // Make the item unbreakable
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        // Store unique data in the PersistentDataContainer.
        // Reuse the container retrieved earlier instead of redeclaring it
        // to avoid duplicate variable errors during compilation.
        pdc.set(ITEM_ID_KEY, PersistentDataType.INTEGER, cItem.getId());
        pdc.set(UPGRADE_LEVEL_KEY, PersistentDataType.INTEGER, cItem.getUpgradeLevel());
        pdc.set(ITEM_UUID_KEY, PersistentDataType.STRING, cItem.getUuid().toString());
        pdc.set(DURABILITY_KEY, PersistentDataType.INTEGER, cItem.getCurrentDurability());

        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * Updates the tooltip (lore) of a custom item based on the player's current stats.
     *
     * @param stack  The ItemStack to update.
     * @param player The player for whom the tooltip should be updated.
     */
    public static void updateCustomItemTooltip(ItemStack stack, Player player) {
        if (stack == null || !stack.hasItemMeta()) {
            Bukkit.getLogger().info("[CustomItem] updateTooltip: Item stack is null or has no item meta.");
            return;
        }

        // Log that we're updating this particular item for the given player.
        ItemMeta meta = stack.getItemMeta();
        String displayName = meta != null ? meta.getDisplayName() : "Unknown";
        String name = (player != null) ? player.getName() : "null";
        Bukkit.getLogger().info("[CustomItem] Updating tooltip for item: " + displayName + " for player: " + name);

        // Retrieve the custom item ID from the PersistentDataContainer.
        Integer itemId = meta.getPersistentDataContainer().get(ITEM_ID_KEY, PersistentDataType.INTEGER);
        if (itemId == null) {
            Bukkit.getLogger().info("[CustomItem] updateTooltip: No custom item ID found.");
            return;
        }

        UUID uuid = getItemUUID(stack);
        CustomItem cItem = ItemManager.getInstance().getCustomItemFromItemStack(stack);
        if (cItem == null) {
            Bukkit.getLogger().warning("[CustomItem] No instance found for UUID " + uuid);
            return;
        }

        // Build the updated lore.
        List<String> lore = new ArrayList<>();

        // Glyph line under the name
        String rarityGlyph = "<glyph:" + cItem.getRarity().name().toLowerCase() + ">";
        String typeGlyph = "<glyph:tool>";
        if (me.nakilex.levelplugin.items.data.ArmorType.matchType(stack) != null) {
            typeGlyph = "<glyph:armor>";
        } else if (me.nakilex.levelplugin.items.data.WeaponType.matchType(stack) != null) {
            typeGlyph = "<glyph:weapon>";
        }
        lore.add(rarityGlyph + typeGlyph);
        lore.add(""); // Blank line for spacing


        // --- Level Requirement ---
        int playerLevel = (player != null) ? LevelManager.getInstance().getLevel(player) : 0;
        String levelRequirementLine;
        if (player == null) {
            levelRequirementLine = ChatColor.GRAY + "Level Requirement: " + cItem.getLevelRequirement();
        } else if (playerLevel < cItem.getLevelRequirement()) {
            levelRequirementLine = ChatColor.RED + "✘ " + ChatColor.GRAY + "Level Requirement: " + ChatColor.WHITE + cItem.getLevelRequirement();
        } else {
            levelRequirementLine = ChatColor.GREEN + "✔ " + ChatColor.GRAY + "Level Requirement: " + ChatColor.WHITE + cItem.getLevelRequirement();
        }
        lore.add(levelRequirementLine);

        lore.add(""); // Blank line for spacing

        // --- Stats Information ---
        String prefix = parsePrefix(cItem.getBaseName());
        StatsManager.StatType prefixStat = prefix != null ? PREFIX_MAP.get(prefix) : null;
        if (cItem.getHp() != 0) {
            String line = ChatColor.RED + "❤ " + ChatColor.GRAY + "Health: " + ChatColor.RED + "+" + cItem.getHp();
            if (prefixStat == StatsManager.StatType.HP) line += ChatColor.LIGHT_PURPLE + " (" + "+" + PREFIX_BONUS + ")";
            lore.add(line);
        }
        if (cItem.getDef() != 0) {
            String line = ChatColor.GRAY + "⛂ " + ChatColor.GRAY + "Defence: " + ChatColor.WHITE + "+" + cItem.getDef();
            if (prefixStat == StatsManager.StatType.DEF) line += ChatColor.LIGHT_PURPLE + " (" + "+" + PREFIX_BONUS + ")";
            lore.add(line);
        }
        if (cItem.getStr() != 0) {
            String line = ChatColor.BLUE + "☠ " + ChatColor.GRAY + "Strength: " + ChatColor.WHITE + "+" + cItem.getStr();
            if (prefixStat == StatsManager.StatType.STR) line += ChatColor.LIGHT_PURPLE + " (" + "+" + PREFIX_BONUS + ")";
            lore.add(line);
        }
        if (cItem.getAgi() != 0) {
            String line = ChatColor.GREEN + "≈ " + ChatColor.GRAY + "Agility: " + ChatColor.WHITE + "+" + cItem.getAgi();
            if (prefixStat == StatsManager.StatType.AGI) line += ChatColor.LIGHT_PURPLE + " (" + "+" + PREFIX_BONUS + ")";
            lore.add(line);
        }
        if (cItem.getIntel() != 0) {
            String line = ChatColor.AQUA + "♦ " + ChatColor.GRAY + "Intelligence: " + ChatColor.WHITE + "+" + cItem.getIntel();
            if (prefixStat == StatsManager.StatType.INT) line += ChatColor.LIGHT_PURPLE + " (" + "+" + PREFIX_BONUS + ")";
            lore.add(line);
        }
        if (cItem.getDex() != 0) {
            String line = ChatColor.YELLOW + "➹ " + ChatColor.GRAY + "Dexterity: " + ChatColor.WHITE + "+" + cItem.getDex();
            if (prefixStat == StatsManager.StatType.DEX) line += ChatColor.LIGHT_PURPLE + " (" + "+" + PREFIX_BONUS + ")";
            lore.add(line);
        }

        lore.add(""); // Blank line before rarity

        if (cItem.isBroken()) {
            lore.add(ChatColor.GRAY + "Durability: " + ChatColor.RED + ChatColor.BOLD + "BROKEN");
        } else {
            lore.add(ChatColor.GRAY + "Durability: "
                + ChatColor.WHITE + cItem.getCurrentDurability()
                + "/" + cItem.getMaxDurability());
        }
        // Update the item meta with the new lore.
        meta.setLore(lore);
        stack.setItemMeta(meta);
    }


    public static UUID getItemUUID(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String uuidString = pdc.get(ITEM_UUID_KEY, PersistentDataType.STRING);
        return uuidString != null ? UUID.fromString(uuidString) : null;
    }

    public static int getUpgradeLevel(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return 0;
        ItemMeta meta = stack.getItemMeta();
        return meta.getPersistentDataContainer().getOrDefault(UPGRADE_LEVEL_KEY, PersistentDataType.INTEGER, 0);
    }

    public static void updateDurability(ItemStack stack, int durability) {
        if (stack == null || !stack.hasItemMeta()) return;
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(DURABILITY_KEY, PersistentDataType.INTEGER, durability);
        stack.setItemMeta(meta);
    }

    public static int getDurability(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return 100;
        ItemMeta meta = stack.getItemMeta();
        return meta.getPersistentDataContainer().getOrDefault(DURABILITY_KEY, PersistentDataType.INTEGER, 100);
    }



    public static void updateUpgradeLevel(ItemStack stack, int upgradeLevel) {
        if (stack == null || !stack.hasItemMeta()) return;
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(UPGRADE_LEVEL_KEY, PersistentDataType.INTEGER, upgradeLevel);
        stack.setItemMeta(meta);
    }

    public static int getCustomItemId(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return -1;
        ItemMeta meta = stack.getItemMeta();
        Integer value = meta.getPersistentDataContainer().get(ITEM_ID_KEY, PersistentDataType.INTEGER);
        return (value != null) ? value : -1;
    }

// ─── Ego Weapon Utilities ────────────────────────────────────────────────

    /**
     * Scale a base stat for an Ego weapon using its rarity and rank.
     * Higher rarity provides a larger base multiplier and increases the
     * bonus gained per rank.
     */
    public static int scaleEgoStat(int base, me.nakilex.levelplugin.ego.EgoRarity rarity, int rank) {
        if (base == 0) return 0;
        double rarityMul = rarity.getScale();
        double perRank = 0.10 * rarityMul;
        double multiplier = rarityMul + (rank - 1) * perRank;
        return (int) Math.round(base * multiplier);
    }

    // Previously certain stats unlocked only at higher rarities. To keep
    // gameplay simpler all rarities now support every stat. The function is
    // retained for future extensibility but always returns true.
    public static boolean rarityAllows(me.nakilex.levelplugin.ego.EgoRarity rarity, StatsManager.StatType stat) {
        return true;
    }

    public static ItemStack createEgoWeaponItem(me.nakilex.levelplugin.ego.EgoWeapon weapon, Material material) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(weapon.getRarity().getColor() + weapon.getName());
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(EGO_ID_KEY, PersistentDataType.STRING, weapon.getId());
            pdc.set(EGO_RANK_KEY, PersistentDataType.INTEGER, weapon.getRank());
            pdc.set(EGO_EXP_KEY, PersistentDataType.INTEGER, weapon.getExp());
            pdc.set(EGO_RARITY_KEY, PersistentDataType.STRING, weapon.getRarity().name());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            stack.setItemMeta(meta);
        }
        updateEgoWeaponTooltip(stack, null);
        return stack;
    }

    /**
     * Copy Ego weapon metadata from one stack to another and update the display
     * name to use the Ego rarity color. Optionally provide the CustomItem used
     * to rebuild the target so the upgrade stars stay in sync.
     */
    public static void copyEgoData(ItemStack source, ItemStack target, CustomItem item, Player viewer) {
        if (source == null || target == null) return;
        if (!source.hasItemMeta() || !target.hasItemMeta()) return;

        ItemMeta srcMeta = source.getItemMeta();
        ItemMeta tgtMeta = target.getItemMeta();
        PersistentDataContainer srcPdc = srcMeta.getPersistentDataContainer();
        PersistentDataContainer tgtPdc = tgtMeta.getPersistentDataContainer();
        if (!srcPdc.has(EGO_ID_KEY, PersistentDataType.STRING)) return;

        String id = srcPdc.get(EGO_ID_KEY, PersistentDataType.STRING);
        int rank = srcPdc.getOrDefault(EGO_RANK_KEY, PersistentDataType.INTEGER, 1);
        int exp  = srcPdc.getOrDefault(EGO_EXP_KEY,  PersistentDataType.INTEGER, 0);
        String rarStr = srcPdc.get(EGO_RARITY_KEY, PersistentDataType.STRING);
        if (rarStr == null && viewer != null) {
            me.nakilex.levelplugin.ego.EgoWeapon w = me.nakilex.levelplugin.ego.EgoWeaponManager.getInstance().getWeapon(viewer.getUniqueId());
            if (w != null && w.getId().equals(id)) {
                rarStr = w.getRarity().name();
            }
        }

        tgtPdc.set(EGO_ID_KEY, PersistentDataType.STRING, id);
        tgtPdc.set(EGO_RANK_KEY, PersistentDataType.INTEGER, rank);
        tgtPdc.set(EGO_EXP_KEY,  PersistentDataType.INTEGER, exp);
        if (rarStr != null) tgtPdc.set(EGO_RARITY_KEY, PersistentDataType.STRING, rarStr);

        EgoRarity rar = EgoRarity.COMMON;
        if (rarStr != null) {
            try { rar = EgoRarity.valueOf(rarStr); } catch (Exception ignored) {}
        }
        String stars = item != null ? "<glyph:star>".repeat(item.getUpgradeLevel()) : "";
        tgtMeta.setDisplayName(rar.getColor() + item.getBaseName() + " " + stars);
        target.setItemMeta(tgtMeta);
        updateEgoWeaponTooltip(target, viewer);
    }

    public static void updateEgoWeaponTooltip(ItemStack stack, Player viewer) {
        if (stack == null || !stack.hasItemMeta()) return;
        ItemMeta meta = stack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(EGO_ID_KEY, PersistentDataType.STRING)) return;

        int rank = pdc.getOrDefault(EGO_RANK_KEY, PersistentDataType.INTEGER, 1);
        int exp  = pdc.getOrDefault(EGO_EXP_KEY,  PersistentDataType.INTEGER, 0);
        me.nakilex.levelplugin.ego.EgoRarity rarity = me.nakilex.levelplugin.ego.EgoRarity.COMMON;
        if (pdc.has(EGO_RARITY_KEY, PersistentDataType.STRING)) {
            try {
                rarity = me.nakilex.levelplugin.ego.EgoRarity.valueOf(pdc.get(EGO_RARITY_KEY, PersistentDataType.STRING));
            } catch (Exception ignored) {}
        }
        int expToNext = 100 * rank * rarity.getXpMultiplier();
        double pct = expToNext > 0 ? (exp * 100.0 / expToNext) : 0.0;
        if (pct > 100.0) pct = 100.0;
        pct = Math.round(pct * 10.0) / 10.0;

        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        lore.clear();

        // rarity already loaded above
        lore.add(rarity.getSymbol() + "<glyph:weapon>");

        lore.add("");
        lore.add(ChatColor.GRAY + "Rank: " + ChatColor.YELLOW + rank + ChatColor.GRAY + "/10");
        lore.add(ChatColor.GRAY + "Progress to Level " + ChatColor.YELLOW + (rank + 1) + ChatColor.GRAY + ": " + ChatColor.YELLOW + pct + "%");
        int barLen = 15;
        int filled = (int) Math.round(pct / 100.0 * barLen);
        String bar = ChatColor.GREEN + "" + "-".repeat(Math.max(filled,0)) + ChatColor.WHITE + "" + "-".repeat(Math.max(barLen - filled,0));
        lore.add(bar + " " + ChatColor.YELLOW + exp + ChatColor.GOLD + "/" + ChatColor.YELLOW + expToNext);
        lore.add("");

        CustomItem cItem = ItemManager.getInstance().getCustomItemFromItemStack(stack);
        if (cItem != null) {
            String prefix = parsePrefix(cItem.getBaseName());
            StatsManager.StatType prefixStat = prefix != null ? PREFIX_MAP.get(prefix) : null;
            int scaledHp = scaleEgoStat(cItem.getHp(), rarity, rank);
            int scaledDef = scaleEgoStat(cItem.getDef(), rarity, rank);
            int scaledStr = scaleEgoStat(cItem.getStr(), rarity, rank);
            int scaledAgi = scaleEgoStat(cItem.getAgi(), rarity, rank);
            int scaledInt = scaleEgoStat(cItem.getIntel(), rarity, rank);
            int scaledDex = scaleEgoStat(cItem.getDex(), rarity, rank);

            if (scaledHp != 0 && rarityAllows(rarity, StatsManager.StatType.HP)) {
                String line = ChatColor.RED + "❤ " + ChatColor.GRAY + "Health: " + ChatColor.RED + "+" + scaledHp;
                if (prefixStat == StatsManager.StatType.HP) line += ChatColor.LIGHT_PURPLE + " (" + "+" + PREFIX_BONUS + ")";
                lore.add(line);
            }
            if (scaledDef != 0 && rarityAllows(rarity, StatsManager.StatType.DEF)) {
                String line = ChatColor.GRAY + "⛂ " + ChatColor.GRAY + "Defence: " + ChatColor.WHITE + "+" + scaledDef;
                if (prefixStat == StatsManager.StatType.DEF) line += ChatColor.LIGHT_PURPLE + " (" + "+" + PREFIX_BONUS + ")";
                lore.add(line);
            }
            if (scaledStr != 0 && rarityAllows(rarity, StatsManager.StatType.STR)) {
                String line = ChatColor.BLUE + "☠ " + ChatColor.GRAY + "Strength: " + ChatColor.WHITE + "+" + scaledStr;
                if (prefixStat == StatsManager.StatType.STR) line += ChatColor.LIGHT_PURPLE + " (" + "+" + PREFIX_BONUS + ")";
                lore.add(line);
            }
            if (scaledAgi != 0 && rarityAllows(rarity, StatsManager.StatType.AGI)) {
                String line = ChatColor.GREEN + "≈ " + ChatColor.GRAY + "Agility: " + ChatColor.WHITE + "+" + scaledAgi;
                if (prefixStat == StatsManager.StatType.AGI) line += ChatColor.LIGHT_PURPLE + " (" + "+" + PREFIX_BONUS + ")";
                lore.add(line);
            }
            if (scaledInt != 0 && rarityAllows(rarity, StatsManager.StatType.INT)) {
                String line = ChatColor.AQUA + "♦ " + ChatColor.GRAY + "Intelligence: " + ChatColor.WHITE + "+" + scaledInt;
                if (prefixStat == StatsManager.StatType.INT) line += ChatColor.LIGHT_PURPLE + " (" + "+" + PREFIX_BONUS + ")";
                lore.add(line);
            }
            if (scaledDex != 0 && rarityAllows(rarity, StatsManager.StatType.DEX)) {
                String line = ChatColor.YELLOW + "➹ " + ChatColor.GRAY + "Dexterity: " + ChatColor.WHITE + "+" + scaledDex;
                if (prefixStat == StatsManager.StatType.DEX) line += ChatColor.LIGHT_PURPLE + " (" + "+" + PREFIX_BONUS + ")";
                lore.add(line);
            }

            lore.add("");
            if (cItem.isBroken()) {
                lore.add(ChatColor.GRAY + "Durability: " + ChatColor.RED + ChatColor.BOLD + "BROKEN");
            } else {
                lore.add(ChatColor.GRAY + "Durability: " + ChatColor.WHITE + cItem.getCurrentDurability() + "/" + cItem.getMaxDurability());
            }
        }

        meta.setLore(lore);
        stack.setItemMeta(meta);
    }

    /**
     * Updates the tooltip of a tool item based on the viewer's mining level.
     */
    public static void updateCustomToolTooltip(ItemStack stack, Player viewer) {
        ToolTier tier = ToolTier.fromMaterial(stack.getType());
        if (tier == null) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;

        List<String> lore = new ArrayList<>();
        String rarityGlyph = "<glyph:" + tier.getRarity().name().toLowerCase() + ">";
        lore.add(rarityGlyph + "<glyph:tool>");
        lore.add("");
        int level = (viewer != null) ? MiningManager.getInstance().getLevel(viewer) : 0;
        String reqLine = level >= tier.getLevelRequirement()
                ? ChatColor.GREEN + "✔ " + ChatColor.GRAY + "Mining Lv. Requirement: " + ChatColor.WHITE + tier.getLevelRequirement()
                : ChatColor.RED + "✘ " + ChatColor.GRAY + "Mining Lv. Requirement: " + ChatColor.WHITE + tier.getLevelRequirement();
        lore.add(reqLine);
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Mining Speed: " + ChatColor.GREEN + "+" + tier.getMiningSpeed());
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        stack.setItemMeta(meta);
    }

    /**
     * Convenience method to update either a custom item or custom tool tooltip.
     */
    public static void updateTooltip(ItemStack stack, Player player) {
        if (stack == null) return;
        if (stack.hasItemMeta() && stack.getItemMeta().getPersistentDataContainer().has(EGO_ID_KEY, PersistentDataType.STRING)) {
            updateEgoWeaponTooltip(stack, player);
        } else if (stack.hasItemMeta() && stack.getItemMeta().getPersistentDataContainer().has(ITEM_UUID_KEY, PersistentDataType.STRING)) {
            updateCustomItemTooltip(stack, player);
        } else if (ToolTier.fromMaterial(stack.getType()) != null) {
            updateCustomToolTooltip(stack, player);
        }
   }
}