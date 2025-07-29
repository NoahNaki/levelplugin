package me.nakilex.levelplugin.items.utils;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.tools.ToolTier;
import me.nakilex.levelplugin.salvage.managers.SalvageManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.player.mining.managers.MiningManager;
import me.nakilex.levelplugin.items.data.ArmorType;
import me.nakilex.levelplugin.items.data.WeaponType;
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
    /**
     * Stores the original material of a custom item before we swap it to a
     * neutral material such as DIAMOND. Used so armor/weapon detection still
     * works after we change the visible material.
     */
    public static final NamespacedKey TEMPLATE_MATERIAL_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ItemUtil.class), "template_material");
    /**
     * Marks that this stack uses a Nexo model so we should not override its
     * material when refreshing the tooltip.
     */
    public static final NamespacedKey NEXO_MODEL_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ItemUtil.class), "nexo_model");
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

    // ─── Default Models ─────────────────────────────────────────────────────

    /** Maximum level that uses the early-game model set. */
    private static final int DEFAULT_MODEL_MAX_LEVEL = 10;

    /** Model set names for default equipment visuals by level range. */
    private static final String MODEL_SET_1_10 = "default1_10";
    private static final String MODEL_SET_11_20 = "dwarven11_20";
    private static final String MODEL_SET_21_30 = "conqueror21_30";

    /**
     * Determine which model set should be used for the given level.
     *
     * @param level The item level requirement.
     * @return The model set name or {@code null} if none applies.
     */
    private static String getModelSetForLevel(int level) {
        if (level <= 10) return MODEL_SET_1_10;
        if (level <= 20) return MODEL_SET_11_20;
        if (level <= 30) return MODEL_SET_21_30;
        return null;
    }

    /** Simple container for a model material and CustomModelData value. */
    private record Model(Material material, int data) {}

    /**
     * Class specific default weapon models. The key is the class name in
     * uppercase. Only four classes are supported for now.
     */
    private static final java.util.Map<String, Model> CLASS_DEFAULT_WEAPONS = java.util.Map.of(
            "MAGE",    new Model(Material.STICK,       1011),
            "ROGUE",   new Model(Material.DIAMOND,     1012),
            "ARCHER",  new Model(Material.BOW,         1002),
            "WARRIOR", new Model(Material.DIAMOND_AXE, 1005)
    );

    /** Default armor models for the early levels. */
    private static final java.util.Map<ArmorType, Model> DEFAULT_ARMOR_MODELS = java.util.Map.of(
            ArmorType.HELMET,     new Model(Material.KELP,             1000),
            ArmorType.CHESTPLATE, new Model(Material.LEATHER_CHESTPLATE, 1002),
            ArmorType.LEGGINGS,   new Model(Material.LEATHER_LEGGINGS,   1002),
            ArmorType.BOOTS,      new Model(Material.LEATHER_BOOTS,      1002)
    );


    /**
     * Creates an ItemStack from a CustomItem while including dynamic tooltip information.
     *
     * @param cItem  The custom item data.
     * @param amount The number of items to create.
     * @param player The player viewing the item (can be null if no context is available).
     * @return The created ItemStack.
     */
    public static ItemStack createItemStackFromCustomItem(CustomItem cItem, int amount, Player player) {
        return createItemStackFromCustomItem(cItem, amount, player, null);
    }

    /**
     * Variant that optionally applies a Nexo model by ID.
     */
    public static ItemStack createItemStackFromCustomItem(CustomItem cItem, int amount, Player player, String nexoId) {
        // Keep track of the template material before any potential neutral swap
        // so we can still determine weapon/armor type later. Default to the
        // custom item's material but replace it with the Nexo model's material
        // if one is provided.
        Material templateMat = cItem.getMaterial();
        Material mat = templateMat;

        // Apply the appropriate model set if this item has no model specified.
        String setName = getModelSetForLevel(cItem.getLevelRequirement());
        if ((nexoId == null || nexoId.isBlank()) && setName != null) {
            String id = me.nakilex.levelplugin.Main.getInstance()
                    .getModelSetManager()
                    .getModelId(setName, templateMat);
            if (id != null && !id.isEmpty()) {
                nexoId = id;
            }
        }
        me.nakilex.levelplugin.items.data.WeaponType wType =
                me.nakilex.levelplugin.items.data.WeaponType.matchType(new ItemStack(templateMat));
        me.nakilex.levelplugin.items.data.ArmorType aType =
                me.nakilex.levelplugin.items.data.ArmorType.matchType(new ItemStack(templateMat));
        // Use a neutral DIAMOND item for all weapons and armor so vanilla
        // attribute tooltips never show up regardless of type.
        boolean needsNeutral = aType != null || wType != null;

        boolean hasNexoModel = nexoId != null && !nexoId.isEmpty();
        boolean willApplyDefaultModel = !hasNexoModel
                && getModelSetForLevel(cItem.getLevelRequirement()) != null;

        Model defaultModel = null;
        if (willApplyDefaultModel && cItem.getLevelRequirement() <= DEFAULT_MODEL_MAX_LEVEL) {
            String cls = cItem.getClassRequirement();
            if (wType != null && cls != null) {
                defaultModel = CLASS_DEFAULT_WEAPONS.get(cls.toUpperCase());
            }
            if (defaultModel == null && aType != null) {
                defaultModel = DEFAULT_ARMOR_MODELS.get(aType);
            }
            if (defaultModel != null) {
                mat = defaultModel.material();
            }
        } else if (!hasNexoModel && aType == null) {
            // fallback material for weapons without models outside the early range
            String cls = cItem.getClassRequirement();
            if (cls != null) {
                switch (cls.toUpperCase()) {
                    case "WARRIOR" -> mat = Material.DIAMOND_SHOVEL;
                    case "ROGUE" -> mat = Material.DIAMOND_SWORD;
                    case "MAGE" -> mat = Material.STICK;
                    case "ARCHER" -> mat = Material.BOW;
                }
            }
        }
        ItemStack stack;
        if (nexoId != null && !nexoId.isEmpty()) {
            com.nexomc.nexo.items.ItemBuilder b = com.nexomc.nexo.api.NexoItems.itemFromId(nexoId);
            // When a model is provided by Nexo keep its original material so the
            // custom resource pack applies correctly. We only adjust the amount
            // here. Also update the template material so we know the true base
            // type of this item.
            stack = b != null ? b.build() : new ItemStack(mat);
            stack.setAmount(amount);
            templateMat = stack.getType();
        } else {
            stack = new ItemStack(mat, amount);
        }
        if (needsNeutral && !hasNexoModel && defaultModel == null) {
            // Armor in the conqueror range has no dedicated model, keep the
            // original material instead of forcing DIAMOND so vanilla visuals
            // remain intact.
            if (!(MODEL_SET_21_30.equals(setName) && aType != null)) {
                stack.setType(Material.DIAMOND);
            }
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        if (hasNexoModel) {
            meta.getPersistentDataContainer().set(NEXO_MODEL_KEY, PersistentDataType.STRING, nexoId);
        } else if (willApplyDefaultModel && !meta.hasCustomModelData() && defaultModel != null) {
            meta.setCustomModelData(defaultModel.data());
        }

        // Set display name with rarity color and upgrade stars.
        ChatColor rarityColor = cItem.getRarity().getColor();
        String stars = "<glyph:star>".repeat(cItem.getUpgradeLevel());
        meta.setDisplayName(rarityColor + cItem.getBaseName() + " " + stars);

        List<String> lore = new ArrayList<>();
        // Glyph line under the name to show rarity and item type
        String rarityGlyph = "<glyph:" + cItem.getRarity().name().toLowerCase() + ">";
        String typeGlyph = "<glyph:tool>";
        // Determine the original material from the template so the correct glyph shows
        Material origMat = templateMat;
        if (me.nakilex.levelplugin.items.data.ArmorType.matchType(new ItemStack(origMat)) != null) {
            typeGlyph = "<glyph:armor>";
        } else if (me.nakilex.levelplugin.items.data.WeaponType.matchType(new ItemStack(origMat)) != null) {
            typeGlyph = "<glyph:weapon>";
        }
        lore.add(rarityGlyph + typeGlyph);
        lore.add(""); // Blank line for spacing

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (!pdc.has(EGO_ID_KEY, PersistentDataType.STRING)) {
            // --- Class Requirement ---
            String clsReqRaw = cItem.getClassRequirement();
            me.nakilex.levelplugin.player.classes.data.PlayerClass reqClass = null;
            try {
                if (clsReqRaw != null && !clsReqRaw.isBlank()) {
                    reqClass = me.nakilex.levelplugin.player.classes.data.PlayerClass.valueOf(clsReqRaw.toUpperCase());
                }
            } catch (IllegalArgumentException ignored) {}

            if (reqClass != null && reqClass != me.nakilex.levelplugin.player.classes.data.PlayerClass.VILLAGER) {
                me.nakilex.levelplugin.player.classes.data.PlayerClass playerClass = null;
                if (player != null) {
                    playerClass = me.nakilex.levelplugin.player.attributes.managers.StatsManager
                            .getInstance().getPlayerStats(player.getUniqueId()).playerClass;
                }
                boolean meets = player == null ||
                        me.nakilex.levelplugin.player.classes.data.ClassUtil.meetsRequirement(playerClass, reqClass);
                String reqName = reqClass.name().substring(0,1) + reqClass.name().substring(1).toLowerCase();
                String line = (meets ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✘ ") +
                        ChatColor.GRAY + "Class Requirement: " + ChatColor.WHITE + reqName;
                lore.add(line);
            }

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
            lore.add(""); // Divider before Gear Score

            int gearScore = SalvageManager.getInstance().getTotalStats(cItem);
            lore.add("<glyph:sword_icon> " + ChatColor.GRAY + "Gear Score: "
                    + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + gearScore);
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
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DYE);

        // Store unique data in the PersistentDataContainer.
        // Reuse the container retrieved earlier instead of redeclaring it
        // to avoid duplicate variable errors during compilation.
        pdc.set(ITEM_ID_KEY, PersistentDataType.INTEGER, cItem.getId());
        pdc.set(UPGRADE_LEVEL_KEY, PersistentDataType.INTEGER, cItem.getUpgradeLevel());
        pdc.set(ITEM_UUID_KEY, PersistentDataType.STRING, cItem.getUuid().toString());
        pdc.set(DURABILITY_KEY, PersistentDataType.INTEGER, cItem.getCurrentDurability());
        pdc.set(TEMPLATE_MATERIAL_KEY, PersistentDataType.STRING, templateMat.name());
        if (nexoId != null && !nexoId.isEmpty()) {
            pdc.set(NEXO_MODEL_KEY, PersistentDataType.STRING, nexoId);
        }

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
        // Determine the original material so we can display the correct glyph
        Material origMat = stack.getType();
        PersistentDataContainer pdcStack = meta.getPersistentDataContainer();
        if (pdcStack.has(TEMPLATE_MATERIAL_KEY, PersistentDataType.STRING)) {
            String stored = pdcStack.get(TEMPLATE_MATERIAL_KEY, PersistentDataType.STRING);
            if (stored != null) {
                try {
                    origMat = Material.valueOf(stored);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        WeaponType wType = WeaponType.matchType(new ItemStack(origMat));
        ArmorType aType = ArmorType.matchType(new ItemStack(origMat));
        boolean hasNexoModel = pdcStack.has(NEXO_MODEL_KEY, PersistentDataType.STRING);
        boolean hasModel = meta.hasCustomModelData();
        String setName = getModelSetForLevel(cItem.getLevelRequirement());
        // Only switch to a DIAMOND type when absolutely necessary to hide vanilla
        // attributes. Conqueror range armor keeps its original material since
        // there is no matching model for it.
        if (!hasNexoModel && !hasModel && (aType != null || wType != null)) {
            if (!(MODEL_SET_21_30.equals(setName) && aType != null)) {
                stack.setType(Material.DIAMOND);
            } else {
                stack.setType(origMat);
            }
        } else {
            stack.setType(origMat);
        }
        if (me.nakilex.levelplugin.items.data.ArmorType.matchType(new ItemStack(origMat)) != null) {
            typeGlyph = "<glyph:armor>";
        } else if (me.nakilex.levelplugin.items.data.WeaponType.matchType(new ItemStack(origMat)) != null) {
            typeGlyph = "<glyph:weapon>";
        }
        lore.add(rarityGlyph + typeGlyph);
        lore.add(""); // Blank line for spacing

        // --- Class Requirement ---
        String clsReqRaw = cItem.getClassRequirement();
        me.nakilex.levelplugin.player.classes.data.PlayerClass reqClass = null;
        try {
            if (clsReqRaw != null && !clsReqRaw.isBlank()) {
                reqClass = me.nakilex.levelplugin.player.classes.data.PlayerClass.valueOf(clsReqRaw.toUpperCase());
            }
        } catch (IllegalArgumentException ignored) {}

        if (reqClass != null && reqClass != me.nakilex.levelplugin.player.classes.data.PlayerClass.VILLAGER) {
            me.nakilex.levelplugin.player.classes.data.PlayerClass playerClass = null;
            if (player != null) {
                playerClass = me.nakilex.levelplugin.player.attributes.managers.StatsManager
                        .getInstance().getPlayerStats(player.getUniqueId()).playerClass;
            }
            boolean meets = player == null ||
                    me.nakilex.levelplugin.player.classes.data.ClassUtil.meetsRequirement(playerClass, reqClass);
            String reqName = reqClass.name().substring(0,1) + reqClass.name().substring(1).toLowerCase();
            String line = (meets ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✘ ") +
                    ChatColor.GRAY + "Class Requirement: " + ChatColor.WHITE + reqName;
            lore.add(line);
        }

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

        lore.add(""); // Blank line before Gear Score

        int gearScore = SalvageManager.getInstance().getTotalStats(cItem);
        lore.add("<glyph:sword_icon> " + ChatColor.GRAY + "Gear Score: "
                + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + gearScore);

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
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DYE);
        meta.setUnbreakable(true);
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
        if (stack.hasItemMeta()) {
            ItemMeta meta = stack.getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (pdc.has(TEMPLATE_MATERIAL_KEY, PersistentDataType.STRING)) {
                // no action needed; stored material helps determine glyphs elsewhere
            }
        }
        if (stack.hasItemMeta() && stack.getItemMeta().getPersistentDataContainer().has(ITEM_UUID_KEY, PersistentDataType.STRING)) {
            updateCustomItemTooltip(stack, player);
        } else if (ToolTier.fromMaterial(stack.getType()) != null) {
            updateCustomToolTooltip(stack, player);
        }
    }

    /**
     * Refresh tooltips for all custom items and tools a player carries.
     */
    public static void refreshTooltips(Player player) {
        player.getInventory().forEach(stack -> {
            if (stack != null && stack.hasItemMeta()) {
                boolean custom = stack.getItemMeta().getPersistentDataContainer()
                        .has(ITEM_UUID_KEY, PersistentDataType.STRING);
                boolean tool = ToolTier.fromMaterial(stack.getType()) != null;
                if (custom || tool) {
                    updateTooltip(stack, player);
                }
            }
        });
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.hasItemMeta()) {
                boolean custom = armor.getItemMeta().getPersistentDataContainer()
                        .has(ITEM_UUID_KEY, PersistentDataType.STRING);
                boolean tool = ToolTier.fromMaterial(armor.getType()) != null;
                if (custom || tool) {
                    updateTooltip(armor, player);
                }
            }
        }
        player.updateInventory();
    }

    public static void applyUpdatedStack(ItemStack target, ItemStack source) {
        if (target == null || source == null) return;
        target.setType(source.getType());
        target.setItemMeta(source.getItemMeta());
    }

    /**
     * Calculate the player's total gear score by summing the stats of all
     * equipped custom items (armor and weapons).
     */
    public static int calculateTotalGearScore(Player player) {
        int total = 0;
        ItemStack[] equip = player.getInventory().getArmorContents();
        for (ItemStack stack : equip) {
            if (stack != null && stack.hasItemMeta()) {
                CustomItem ci = ItemManager.getInstance().getCustomItemFromItemStack(stack);
                if (ci != null) total += SalvageManager.getInstance().getTotalStats(ci);
            }
        }
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main != null && main.hasItemMeta()) {
            CustomItem ci = ItemManager.getInstance().getCustomItemFromItemStack(main);
            if (ci != null) total += SalvageManager.getInstance().getTotalStats(ci);
        }
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off != null && off.hasItemMeta()) {
            CustomItem ci = ItemManager.getInstance().getCustomItemFromItemStack(off);
            if (ci != null) total += SalvageManager.getInstance().getTotalStats(ci);
        }
        return total;
    }
}