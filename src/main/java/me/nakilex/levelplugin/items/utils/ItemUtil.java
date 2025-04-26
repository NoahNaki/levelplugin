package me.nakilex.levelplugin.items.utils;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemUtil {

    public static final NamespacedKey UPGRADE_LEVEL_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ItemUtil.class), "upgrade_level");
    public static final NamespacedKey ITEM_ID_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ItemUtil.class), "custom_item_id");
    public static final NamespacedKey ITEM_UUID_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ItemUtil.class), "custom_item_uuid");

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
        String stars = "★".repeat(cItem.getUpgradeLevel());
        meta.setDisplayName(rarityColor + cItem.getBaseName() + " " + stars);

        List<String> lore = new ArrayList<>();
        lore.add(""); // Blank line for spacing

        // --- Level Requirement ---
        int playerLevel = (player != null) ? LevelManager.getInstance().getLevel(player) : 0;
        String levelRequirementLine;
        if (player == null) {
            // When no player is provided, simply show the requirement in gray.
            levelRequirementLine = ChatColor.GRAY + "Level Requirement: " + cItem.getLevelRequirement();
        } else if (playerLevel < cItem.getLevelRequirement()) {
            // Player does not meet the requirement: red cross and then light gray text.
            levelRequirementLine = ChatColor.RED + "✘ " + ChatColor.GRAY + "Level Requirement: " + ChatColor.WHITE + cItem.getLevelRequirement();
        } else {
            // Player meets the requirement: green check and then light gray text.
            levelRequirementLine = ChatColor.GREEN + "✔ " + ChatColor.GRAY + "Level Requirement: " + ChatColor.WHITE + cItem.getLevelRequirement();
        }
        lore.add(levelRequirementLine);

        // --- Class Requirement ---
        if (cItem.getClassRequirement() != null && !cItem.getClassRequirement().equalsIgnoreCase("ANY")) {
            // Format the class requirement: capitalize first letter.
            String rawClassReq = cItem.getClassRequirement();
            String classReq = rawClassReq.substring(0, 1).toUpperCase() + rawClassReq.substring(1).toLowerCase();
            String classRequirementLine;

            if (player == null) {
                classRequirementLine = ChatColor.GRAY + "Class Requirement: " + classReq;
            } else {
                // Retrieve the player's class from StatsManager.
                PlayerClass playerClass = StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass;
                if (playerClass.name().equalsIgnoreCase(classReq)) {
                    classRequirementLine = ChatColor.GREEN + "✔ " + ChatColor.GRAY + "Class Requirement: " + ChatColor.WHITE + classReq;
                } else {
                    classRequirementLine = ChatColor.RED + "✘ " + ChatColor.GRAY + "Class Requirement: " + ChatColor.WHITE + classReq;
                }
            }
            lore.add(classRequirementLine);
        }
        lore.add(""); // Another blank line for spacing

        // --- Stats Information ---
        if (cItem.getHp() != 0)
            lore.add(ChatColor.RED + "❤ " + ChatColor.GRAY + "Health: " + ChatColor.RED + "+" + cItem.getHp());
        if (cItem.getDef() != 0)
            lore.add(ChatColor.GRAY + "⛂ " + ChatColor.GRAY + "Defence: " + ChatColor.WHITE + "+" + cItem.getDef());
        if (cItem.getStr() != 0)
            lore.add(ChatColor.BLUE + "☠ " + ChatColor.GRAY + "Strength: " + ChatColor.WHITE + "+" + cItem.getStr());
        if (cItem.getAgi() != 0)
            lore.add(ChatColor.GREEN + "≈ " + ChatColor.GRAY + "Agility: " + ChatColor.WHITE + "+" + cItem.getAgi());
        if (cItem.getIntel() != 0)
            lore.add(ChatColor.AQUA + "♦ " + ChatColor.GRAY + "Intelligence: " + ChatColor.WHITE + "+" + cItem.getIntel());
        if (cItem.getDex() != 0)
            lore.add(ChatColor.YELLOW + "➹ " + ChatColor.GRAY + "Dexterity: " + ChatColor.WHITE + "+" + cItem.getDex());
        lore.add(""); // Blank line before rarity

        // --- Rarity ---
        lore.add(rarityColor + "" + ChatColor.BOLD + cItem.getRarity().name());

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); // Hide item attributes
        meta.setUnbreakable(true); // Make the item unbreakable
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        // Store unique data in the PersistentDataContainer.
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ITEM_ID_KEY, PersistentDataType.INTEGER, cItem.getId());
        pdc.set(UPGRADE_LEVEL_KEY, PersistentDataType.INTEGER, cItem.getUpgradeLevel());
        pdc.set(ITEM_UUID_KEY, PersistentDataType.STRING, cItem.getUuid().toString());

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
            Bukkit.getLogger().info("[CustomItem] updateCustomItemTooltip: Item stack is null or has no item meta.");
            return;
        }

        // Log that we're updating this particular item for the given player.
        ItemMeta meta = stack.getItemMeta();
        String displayName = meta != null ? meta.getDisplayName() : "Unknown";
        Bukkit.getLogger().info("[CustomItem] Updating tooltip for item: " + displayName + " for player: " + player.getName());

        // Retrieve the custom item ID from the PersistentDataContainer.
        Integer itemId = meta.getPersistentDataContainer().get(ITEM_ID_KEY, PersistentDataType.INTEGER);
        if (itemId == null) {
            Bukkit.getLogger().info("[CustomItem] updateCustomItemTooltip: No custom item ID found.");
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

        // --- Class Requirement ---
        if (cItem.getClassRequirement() != null && !cItem.getClassRequirement().equalsIgnoreCase("ANY")) {
            String rawClassReq = cItem.getClassRequirement();
            String classReq = rawClassReq.substring(0, 1).toUpperCase() + rawClassReq.substring(1).toLowerCase();
            String classRequirementLine;
            if (player == null) {
                classRequirementLine = ChatColor.GRAY + "Class Requirement: " + classReq;
            } else {
                PlayerClass playerClass = StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass;
                if (playerClass.name().equalsIgnoreCase(classReq)) {
                    classRequirementLine = ChatColor.GREEN + "✔ " + ChatColor.GRAY + "Class Requirement: " + ChatColor.WHITE + classReq;
                } else {
                    classRequirementLine = ChatColor.RED + "✘ " + ChatColor.GRAY + "Class Requirement: " + ChatColor.WHITE + classReq;
                }
            }
            lore.add(classRequirementLine);
        }
        lore.add(""); // Blank line for spacing

        // --- Stats Information ---
        if (cItem.getHp() != 0)
            lore.add(ChatColor.RED + "❤ " + ChatColor.GRAY + "Health: " + ChatColor.RED + "+" + cItem.getHp());
        if (cItem.getDef() != 0)
            lore.add(ChatColor.GRAY + "⛂ " + ChatColor.GRAY + "Defence: " + ChatColor.WHITE + "+" + cItem.getDef());
        if (cItem.getStr() != 0)
            lore.add(ChatColor.BLUE + "☠ " + ChatColor.GRAY + "Strength: " + ChatColor.WHITE + "+" + cItem.getStr());
        if (cItem.getAgi() != 0)
            lore.add(ChatColor.GREEN + "≈ " + ChatColor.GRAY + "Agility: " + ChatColor.WHITE + "+" + cItem.getAgi());
        if (cItem.getIntel() != 0)
            lore.add(ChatColor.AQUA + "♦ " + ChatColor.GRAY + "Intelligence: " + ChatColor.WHITE + "+" + cItem.getIntel());
        if (cItem.getDex() != 0)
            lore.add(ChatColor.YELLOW + "➹ " + ChatColor.GRAY + "Dexterity: " + ChatColor.WHITE + "+" + cItem.getDex());

        lore.add(""); // Blank line before rarity

        ChatColor rarityColor = cItem.getRarity().getColor();
        lore.add(rarityColor + "" + ChatColor.BOLD + cItem.getRarity().name());

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
}