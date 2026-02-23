package me.nakilex.levelplugin.items.utils;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import me.nakilex.levelplugin.items.tools.ToolManager;
import me.nakilex.levelplugin.items.tools.ToolTier;
import me.nakilex.levelplugin.salvage.managers.SalvageManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.player.mining.managers.MiningManager;
import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.items.data.ArmorType;
import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.potions.data.PotionInstance;
import me.nakilex.levelplugin.utils.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import me.nakilex.levelplugin.utils.GuiUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public static final NamespacedKey SOULBOUND_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ItemUtil.class), "soulbound");
    public static final NamespacedKey DUNGEON_ITEM_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ItemUtil.class), "dungeon_item");

    private static final Pattern LEVEL_REQUIREMENT_PATTERN = Pattern.compile("(?i)(?:level|lvl|lv)\\.?\\s*(?:requirement|req)?\\s*:?\\s*(\\d+)");
    private static final Pattern LAST_NUMBER_PATTERN = Pattern.compile("(\\d+)(?!.*\\d)");

    private static final int PREFIX_BONUS = 20;
    private static final java.util.Map<String, StatsManager.StatType> PREFIX_MAP = new java.util.HashMap<>();
    private static final java.util.List<String> PREFIX_LIST = new java.util.ArrayList<>();

    static {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(
                new java.io.File(JavaPlugin.getProvidingPlugin(ItemUtil.class).getDataFolder(), "prefixes.yml"));
        for (String key : cfg.getKeys(false)) {
            String prefix = cfg.getString(key);
            if (prefix == null) continue;
            StatsManager.StatType st = StatsManager.StatType.fromKey(key);
            if (st == null) st = StatsManager.StatType.VIT;
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
     * Extract the stored Nexo model id from an item stack, if present.
     */
    public static String getNexoModelId(org.bukkit.inventory.ItemStack stack) {
        if (stack == null) return null;
        org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(NEXO_MODEL_KEY, org.bukkit.persistence.PersistentDataType.STRING)) {
            return pdc.get(NEXO_MODEL_KEY, org.bukkit.persistence.PersistentDataType.STRING);
        }
        return null;
    }

    /**
     * Apply a Nexo model id to an existing item, adjusting its material and
     * custom model data while preserving other metadata.
     */
    public static void applyNexoModel(org.bukkit.inventory.ItemStack stack, String nexoId) {
        if (stack == null || nexoId == null || nexoId.isBlank()) return;
        org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        com.nexomc.nexo.items.ItemBuilder b = com.nexomc.nexo.api.NexoItems.itemFromId(nexoId);
        if (b != null) {
            org.bukkit.inventory.ItemStack base = b.build();
            org.bukkit.inventory.meta.ItemMeta baseMeta = base.getItemMeta();
            if (baseMeta != null && baseMeta.hasCustomModelData()) {
                meta.setCustomModelData(baseMeta.getCustomModelData());
            }
            stack.setType(base.getType());
        }
        meta.getPersistentDataContainer().set(NEXO_MODEL_KEY, org.bukkit.persistence.PersistentDataType.STRING, nexoId);
        stack.setItemMeta(meta);
    }

    /** Append stat lines to lore following the standard display order. */
    private static void addStatLines(List<String> lore, CustomItem cItem,
                                    StatsManager.StatType prefixStat) {
        for (StatsManager.StatType type : StatsManager.StatType.DISPLAY_ORDER) {
            int val = cItem.getStat(type);
            if (val != 0) {
                String line = GuiUtil.formatStatLine(type, val, false);
                if (prefixStat == type) {
                    line += ChatColor.LIGHT_PURPLE + " (" + "+" + PREFIX_BONUS + ")";
                }
                lore.add(line);
            }
        }
    }

    private static void addEnchantedLine(List<String> lore, CustomItem cItem) {
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Enchanted: &f" + cItem.getEnchantCount()));
    }

    // ─── Default Models ─────────────────────────────────────────────────────

    /** Maximum level that uses the early-game model set. */
    private static final int DEFAULT_MODEL_MAX_LEVEL = 10;

    /** Model set names for default equipment visuals by level range. */
    private static final String MODEL_SET_1_10 = "default1_10";
    private static final String MODEL_SET_11_20 = "dwarven11_20";
    private static final String MODEL_SET_21_30 = "conqueror21_30";
    private static final String MODEL_SET_31_45 = "assortment31_45";
    private static final String MODEL_SET_46_60 = "demonking46_60";

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
        if (level <= 45) return MODEL_SET_31_45;
        if (level <= 100) return MODEL_SET_46_60;
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
            // Use a proper sword material so rogue weapons remain equipable
            "ROGUE",   new Model(Material.DIAMOND_SWORD, 1012),
            "ARCHER",  new Model(Material.BOW,         1002),
            // Warriors wield shovels by default to distinguish from rogue swords
            "WARRIOR", new Model(Material.DIAMOND_SHOVEL, 1005)
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
        // Only weapon items need a neutral material to hide vanilla attributes.
        boolean needsNeutral = wType != null;

        boolean hasNexoModel = nexoId != null && !nexoId.isEmpty();
        boolean willApplyDefaultModel = !hasNexoModel
                && getModelSetForLevel(cItem.getLevelRequirement()) != null;

        Model defaultModel = null;
        if (willApplyDefaultModel && cItem.getLevelRequirement() <= DEFAULT_MODEL_MAX_LEVEL) {
            String cls = cItem.getClassRequirement();
            if (wType != null) {
                cls = switch (wType) {
                    case WAND -> "MAGE";
                    case BOW -> "ARCHER";
                    case SHOVEL, AXE -> "WARRIOR";
                    case SWORD -> "ROGUE";
                };
            }
            if (wType != null) {
                defaultModel = CLASS_DEFAULT_WEAPONS.get(cls.toUpperCase());
            }
            if (defaultModel != null) {
                mat = defaultModel.material();
            }
        } else if (!hasNexoModel && aType == null) {
            // fallback material for weapons without models outside the early range
            String cls = cItem.getClassRequirement();
            if (wType != null) {
                cls = switch (wType) {
                    case WAND -> "MAGE";
                    case BOW -> "ARCHER";
                    case SHOVEL, AXE -> "WARRIOR";
                    case SWORD -> "ROGUE";
                };
            }
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
            // Swap to a neutral diamond item for weapons without models so
            // vanilla attribute lines never appear.
            stack.setType(Material.DIAMOND);
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

        // --- Class Requirement ---
        String clsReqRaw = cItem.getClassRequirement();
        if (wType != null) {
            clsReqRaw = switch (wType) {
                case WAND -> "MAGE";
                case BOW -> "ARCHER";
                case SHOVEL, AXE -> "WARRIOR";
                case SWORD -> "ROGUE";
            };
        }
        me.nakilex.levelplugin.player.classes.data.PlayerClass reqClass = null;
        if (clsReqRaw != null && !clsReqRaw.isBlank()) {
            reqClass = me.nakilex.levelplugin.player.classes.data.PlayerClass.fromString(clsReqRaw);
        }

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
        lore.add(ChatColor.GRAY + "Gear Score: "
                + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + gearScore);
        lore.add(""); // divider after Gear Score

        // --- Stats Information ---
        String prefix = parsePrefix(cItem.getBaseName());
        StatsManager.StatType prefixStat = prefix != null ? PREFIX_MAP.get(prefix) : null;
        addStatLines(lore, cItem, prefixStat);

        lore.add("");
        addEnchantedLine(lore, cItem);
        if (cItem.isBroken()) {
            lore.add(ChatColor.GRAY + "Durability: " + ChatColor.RED + ChatColor.BOLD + "BROKEN");
        } else {
            lore.add(ChatColor.GRAY + "Durability: "
                + ChatColor.WHITE + cItem.getCurrentDurability()
                + "/" + cItem.getMaxDurability());
        }

        if (cItem.isSoulbound()) {
            lore.add(ChatColor.RED + "Soulbound");
            pdc.set(SOULBOUND_KEY, PersistentDataType.BYTE, (byte)1);
        }
        meta.setUnbreakable(true);
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
        applyRarityTooltipStyle(stack, cItem.getRarity());
        centerGearName(stack);
        return stack;
    }

    public static boolean isSoulbound(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;

        // Primary check: custom items that have the standard soulbound key
        if (stack.getItemMeta().getPersistentDataContainer().has(SOULBOUND_KEY, PersistentDataType.BYTE)) {
            return true;
        }

        // Handle class essences which use a different key
        if (ClassEssence.isSoulbound(stack)) {
            return true;
        }

        // Fallback for legacy items that only have a lore indicator
        if (stack.getItemMeta().hasLore()) {
            for (String line : stack.getItemMeta().getLore()) {
                if (ChatColor.stripColor(line).equalsIgnoreCase("Soulbound")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isDungeonItem(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;

        if (meta.getPersistentDataContainer().has(DUNGEON_ITEM_KEY, PersistentDataType.BYTE)) {
            return true;
        }

        if (meta.hasLore()) {
            for (String line : meta.getLore()) {
                if (ChatColor.stripColor(line).equalsIgnoreCase("Dungeon Item")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determine the rarity of an ItemStack. Supports custom items,
     * tools, custom potions, essences, and vanilla potion types.
     * Returns {@code null} when the stack is not recognized.
     */
    public static ItemRarity getItemRarity(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return null;

        CustomItem cItem = ItemManager.getInstance().getCustomItemFromItemStack(stack);
        if (cItem != null) return cItem.getRarity();

        PotionInstance pInst = Main.getInstance().getPotionManager().getInstanceFromItem(stack);
        if (pInst != null) return ItemRarity.fromTier(pInst.getTemplate().getTier());

        me.nakilex.levelplugin.items.tools.CustomTool tool = ToolManager.getInstance().getTool(stack);
        if (tool != null) {
            return tool.getTier().getRarity();
        }

        ItemRarity essenceRarity = ClassEssence.getRarity(stack);
        if (essenceRarity != null) {
            return essenceRarity;
        }

        Material type = stack.getType();
        if (type == Material.POTION || type == Material.SPLASH_POTION || type == Material.LINGERING_POTION) {
            return ItemRarity.COMMON;
        }
        return null;
    }

    /**
     * Insert a lore line into an ItemStack at the given index. Negative or
     * out-of-range indices append to the end of the lore. This method is
     * intentionally generic so it can be reused anywhere we need to tweak
     * existing item tooltips.
     *
     * @param stack the item whose lore should be modified
     * @param index the position to insert at; values outside the current range
     *              are clamped to the end
     * @param line  the text to insert
     */
    public static void insertLoreLine(ItemStack stack, int index, String line) {
        if (stack == null || stack.getType() == Material.AIR) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        if (index < 0 || index > lore.size()) {
            lore.add(line);
        } else {
            lore.add(index, line);
        }

        meta.setUnbreakable(true);
        meta.setLore(lore);
        stack.setItemMeta(meta);
    }

    /**
     * Generic helper to store a string value in an item's persistent data
     * container. Useful for lightweight component-style metadata like
     * tooltip borders.
     */
    public static void setStringData(ItemStack stack, NamespacedKey key, String value) {
        if (stack == null || stack.getType() == Material.AIR) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        stack.setItemMeta(meta);
    }

    /**
     * Generic helper to apply a keyed data component (such as tooltip style)
     * directly onto an {@link ItemStack}. The component type must accept a
     * {@link Key} value.
     *
     * @param stack the stack to modify
     * @param type  the data component type to set
     * @param value the key value for the component
     */
    public static void setKeyedComponent(ItemStack stack, DataComponentType.Valued<Key> type, Key value) {
        if (stack == null || stack.getType() == Material.AIR) return;
        if (type == null || value == null) return;
        stack.setData(type, value);
    }

    /**
     * Convenience method to add {@link ItemFlag}s to an {@link ItemStack} while
     * safely handling null checks.
     */
    public static void addItemFlags(ItemStack stack, ItemFlag... flags) {
        if (stack == null || stack.getType() == Material.AIR) return;
        if (flags == null || flags.length == 0) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        meta.addItemFlags(flags);
        stack.setItemMeta(meta);
    }

    /**
     * Apply or remove a lightweight enchantment glint for visual emphasis.
     * Keeps enchant lines hidden while preserving existing item data.
     */
    public static void setVisualEnchanted(ItemStack stack, boolean enchanted) {
        if (stack == null || stack.getType() == Material.AIR) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        if (enchanted) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else if (meta.hasEnchant(Enchantment.UNBREAKING)) {
            meta.removeEnchant(Enchantment.UNBREAKING);
        }
        stack.setItemMeta(meta);
    }

    /**
     * Center the display name of weapons and armor without altering lore lines.
     * This ensures only gear names are centered, leaving other items untouched.
     *
     * @param stack the item stack to adjust
     */
    public static void centerGearName(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return;
        if (WeaponType.matchType(stack) != null || ArmorType.matchType(stack) != null) {
            TextUtil.centerItemTooltip(stack, true, false);
        }
    }

    /**
     * Convenience wrapper to set the tooltip border style based on an
     * {@link ItemRarity}. This ensures items consistently display the correct
     * frame whenever they are created or refreshed.
     */
    public static void applyRarityTooltipStyle(ItemStack stack, ItemRarity rarity) {
        if (stack == null || rarity == null) return;
        String style = "minecraft:" + rarity.getTooltipStyle();
        setKeyedComponent(stack, DataComponentTypes.TOOLTIP_STYLE, Key.key(style));
    }

    /**
     * Resolve the original template material for a custom item, falling back
     * to the current material when no template is stored.
     */
    public static Material getTemplateMaterial(ItemStack stack) {
        if (stack == null) return null;
        Material material = stack.getType();
        if (!stack.hasItemMeta()) {
            return material;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return material;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(TEMPLATE_MATERIAL_KEY, PersistentDataType.STRING)) {
            String stored = pdc.get(TEMPLATE_MATERIAL_KEY, PersistentDataType.STRING);
            if (stored != null) {
                try {
                    material = Material.valueOf(stored);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return material;
    }

    /**
     * Check whether the stack represents a weapon or armor item.
     */
    public static boolean isWeaponOrArmor(ItemStack stack) {
        Material material = getTemplateMaterial(stack);
        if (material == null) return false;
        ItemStack probe = new ItemStack(material);
        return WeaponType.matchType(probe) != null || ArmorType.matchType(probe) != null;
    }

    /**
     * Return the rarity for a custom item stack, or null if not a custom item.
     */
    public static ItemRarity getCustomItemRarity(ItemStack stack) {
        if (stack == null) return null;
        CustomItem item = ItemManager.getInstance().getCustomItemFromItemStack(stack);
        return item != null ? item.getRarity() : null;
    }

    /**
     * Checks whether an ItemStack can be placed into the salvage GUI.
     * Accepts custom items, tools, essences, and potions (including vanilla).
     */
    public static boolean isSalvageable(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return false;

        if (ItemManager.getInstance().getCustomItemFromItemStack(stack) != null) return true;
        if (ToolManager.getInstance().getTool(stack) != null) return true;
        if (Main.getInstance().getPotionManager().getInstanceFromItem(stack) != null) return true;
        if (ClassEssence.isEssence(stack)) return true;

        Material type = stack.getType();
        return type == Material.POTION || type == Material.SPLASH_POTION || type == Material.LINGERING_POTION;
    }

    /**
     * Updates the tooltip (lore) of a custom item based on the player's current stats.
     *
     * @param stack  The ItemStack to update.
     * @param player The player for whom the tooltip should be updated.
     */
    public static void updateCustomItemTooltip(ItemStack stack, Player player) {
        if (stack == null || !stack.hasItemMeta()) {
            return;
        }

        ItemMeta meta = stack.getItemMeta();

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

        applyRarityTooltipStyle(stack, cItem.getRarity());
        centerGearName(stack);

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
        // Only switch to a DIAMOND type when absolutely necessary to hide vanilla
        // attributes on weapons without models.
        if (!hasNexoModel && !hasModel && wType != null) {
            stack.setType(Material.DIAMOND);
        } else {
            stack.setType(origMat);
        }
        if (aType != null) {
            typeGlyph = "<glyph:armor>";
        } else if (wType != null) {
            typeGlyph = "<glyph:weapon>";
        }
        lore.add(rarityGlyph + typeGlyph);
        lore.add(""); // Blank line for spacing

        // --- Class Requirement ---
        String clsReqRaw = cItem.getClassRequirement();
        me.nakilex.levelplugin.player.classes.data.PlayerClass reqClass = null;
        if (clsReqRaw != null && !clsReqRaw.isBlank()) {
            reqClass = me.nakilex.levelplugin.player.classes.data.PlayerClass.fromString(clsReqRaw);
        }

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
        lore.add(ChatColor.GRAY + "Gear Score: "
                + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + gearScore);
        lore.add(""); // divider after Gear Score

        // --- Stats Information ---
        String prefix = parsePrefix(cItem.getBaseName());
        StatsManager.StatType prefixStat = prefix != null ? PREFIX_MAP.get(prefix) : null;
        addStatLines(lore, cItem, prefixStat);


        lore.add(""); // Blank line before rarity

        addEnchantedLine(lore, cItem);

        if (cItem.isBroken()) {
            lore.add(ChatColor.GRAY + "Durability: " + ChatColor.RED + ChatColor.BOLD + "BROKEN");
        } else {
            lore.add(ChatColor.GRAY + "Durability: "
                + ChatColor.WHITE + cItem.getCurrentDurability()
                + "/" + cItem.getMaxDurability());
        }
        // Update the item meta with the new lore.
        meta.setUnbreakable(true);
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

    public static Integer getLevelRequirement(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        CustomItem custom = ItemManager.getInstance().getCustomItemFromItemStack(stack);
        if (custom != null) {
            return custom.getLevelRequirement();
        }
        int templateId = getCustomItemId(stack);
        if (templateId >= 0) {
            CustomItem template = ItemManager.getInstance().getTemplateById(templateId);
            if (template != null) {
                return template.getLevelRequirement();
            }
        }
        if (!stack.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return null;
        }
        for (String line : meta.getLore()) {
            String stripped = ChatColor.stripColor(line);
            if (stripped == null) {
                continue;
            }
            String lower = stripped.toLowerCase(Locale.ROOT);
            if (!lower.contains("level") && !lower.contains("lv") && !lower.contains("lvl")) {
                continue;
            }
            Matcher matcher = LEVEL_REQUIREMENT_PATTERN.matcher(stripped);
            if (matcher.find()) {
                try {
                    return Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException ignored) {
                    // keep searching
                }
            }
            if (lower.contains("requirement") || lower.contains("req")) {
                Matcher fallback = LAST_NUMBER_PATTERN.matcher(stripped);
                if (fallback.find()) {
                    try {
                        return Integer.parseInt(fallback.group(1));
                    } catch (NumberFormatException ignored) {
                        // keep searching
                    }
                }
            }
        }
        return null;
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
        me.nakilex.levelplugin.items.tools.CustomTool customTool = ToolManager.getInstance().getTool(stack);
        ToolTier tier = customTool != null ? customTool.getTier() : ToolTier.fromMaterial(stack.getType());
        if (tier == null) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;

        applyRarityTooltipStyle(stack, tier.getRarity());
        centerGearName(stack);

        List<String> lore = new ArrayList<>();
        String rarityGlyph = "<glyph:" + tier.getRarity().name().toLowerCase() + ">";
        lore.add(rarityGlyph + "<glyph:tool>");
        lore.add("");
        ToolDiscipline discipline = customTool != null ? customTool.getDiscipline() : ToolDiscipline.MINING;
        int level = 0;
        String requirementLabel;
        if (discipline == ToolDiscipline.FARMING) {
            level = (viewer != null) ? me.nakilex.levelplugin.player.farming.managers.FarmingManager.getInstance().getLevel(viewer) : 0;
            requirementLabel = "Farming";
        } else if (discipline == ToolDiscipline.FISHING) {
            level = (viewer != null) ? me.nakilex.levelplugin.player.fishing.managers.FishingManager.getInstance().getLevel(viewer) : 0;
            requirementLabel = "Fishing";
        } else if (discipline == ToolDiscipline.WOODCUTTING) {
            level = (viewer != null) ? me.nakilex.levelplugin.player.woodcutting.managers.WoodcuttingManager.getInstance().getLevel(viewer) : 0;
            requirementLabel = "Woodcutting";
        } else {
            level = (viewer != null) ? MiningManager.getInstance().getLevel(viewer) : 0;
            requirementLabel = "Mining";
        }
        String reqLine = level >= tier.getLevelRequirement()
                ? ChatColor.GREEN + "✔ " + ChatColor.GRAY + requirementLabel + " Lv. Requirement: " + ChatColor.WHITE + tier.getLevelRequirement()
                : ChatColor.RED + "✘ " + ChatColor.GRAY + requirementLabel + " Lv. Requirement: " + ChatColor.WHITE + tier.getLevelRequirement();
        lore.add(reqLine);
        lore.add(" ");
        if (discipline == ToolDiscipline.FARMING) {
            lore.add(ChatColor.GRAY + "Harvest Yield: " + ChatColor.GREEN + "+" + (int) (tier.getHarvestYield() * 100 - 100) + "%");
            me.nakilex.levelplugin.items.tools.FarmingToolEnchant enchant =
                    ToolManager.getInstance().getFarmingEnchant(stack);
            if (enchant != null) {
                lore.add(ChatColor.GRAY + "Enchant: " + ChatColor.LIGHT_PURPLE + enchant.getDisplayName());
                lore.addAll(me.nakilex.levelplugin.utils.TooltipUtil.bulletList(enchant.getDescription()));
            }
        } else if (discipline == ToolDiscipline.FISHING) {
            lore.add(ChatColor.GRAY + "Fishing Speed: " + ChatColor.GREEN + "+" + (int) (tier.getFishingSpeed() * 100 - 100) + "%");
            lore.add(ChatColor.GRAY + "Fish Rarity: " + ChatColor.GREEN + "+" + (int) (tier.getFishRarityBonus() * 100 - 100) + "%");
        } else if (discipline == ToolDiscipline.WOODCUTTING) {
            lore.add(ChatColor.GRAY + "Woodcutting Speed: " + ChatColor.GREEN + "+" + tier.getMiningSpeed());
            me.nakilex.levelplugin.items.tools.WoodcuttingToolEnchant enchant =
                    ToolManager.getInstance().getWoodcuttingEnchant(stack);
            if (enchant != null) {
                lore.add(ChatColor.GRAY + "Enchant: " + ChatColor.LIGHT_PURPLE + enchant.getDisplayName());
                lore.addAll(me.nakilex.levelplugin.utils.TooltipUtil.bulletList(enchant.getDescription()));
            }
            meta.setUnbreakable(true);
        } else if (discipline == ToolDiscipline.WOODCUTTING) {
            lore.add(ChatColor.GRAY + "Woodcutting Speed: " + ChatColor.GREEN + "+" + tier.getMiningSpeed());
        } else {
            lore.add(ChatColor.GRAY + "Mining Speed: " + ChatColor.GREEN + "+" + tier.getMiningSpeed());
        }
        meta.setUnbreakable(true);
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
        } else if (ToolManager.getInstance().isToolMaterial(stack.getType())) {
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
                boolean tool = ToolManager.getInstance().isToolMaterial(stack.getType());
                if (custom || tool) {
                    updateTooltip(stack, player);
                }
            }
        });
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.hasItemMeta()) {
                boolean custom = armor.getItemMeta().getPersistentDataContainer()
                        .has(ITEM_UUID_KEY, PersistentDataType.STRING);
                boolean tool = ToolManager.getInstance().isToolMaterial(armor.getType());
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
    /** Sum gear scores for any collection of custom items. */
    public static int calculateTotalGearScore(Collection<CustomItem> items) {
        int total = 0;
        if (items == null) return 0;
        for (CustomItem ci : items) {
            if (ci != null) {
                total += SalvageManager.getInstance().getTotalStats(ci);
            }
        }
        return total;
    }

    /** Sum gear score for all equipment a player currently has equipped. */
    public static int calculateTotalGearScore(Player player) {
        List<CustomItem> items = new ArrayList<>();
        ItemStack[] equip = player.getInventory().getArmorContents();
        for (ItemStack stack : equip) {
            if (stack != null && stack.hasItemMeta()) {
                CustomItem ci = ItemManager.getInstance().getCustomItemFromItemStack(stack);
                if (ci != null) items.add(ci);
            }
        }
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main != null && main.hasItemMeta()) {
            CustomItem ci = ItemManager.getInstance().getCustomItemFromItemStack(main);
            if (ci != null) items.add(ci);
        }
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off != null && off.hasItemMeta()) {
            CustomItem ci = ItemManager.getInstance().getCustomItemFromItemStack(off);
            if (ci != null) items.add(ci);
        }
        int total = calculateTotalGearScore(items);
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        if (ps != null) {
            for (int i = 0; i < ps.equippedEssences.length; i++) {
                if (ps.equippedEssences[i]) {
                    ItemStack ess = ps.essenceSlots[i];
                    if (ess != null) total += ClassEssence.getGearScore(ess);
                }
            }
        }
        return total;
    }
}
