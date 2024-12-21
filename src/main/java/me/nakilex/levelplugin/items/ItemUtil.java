package me.nakilex.levelplugin.items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ItemUtil {

    public static final NamespacedKey UPGRADE_LEVEL_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ItemUtil.class), "upgrade_level");
    public static final NamespacedKey GLOBAL_ID_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ItemUtil.class), "global_item_id");

    public static final NamespacedKey ITEM_ID_KEY = new NamespacedKey(
        JavaPlugin.getProvidingPlugin(ItemUtil.class),
        "custom_item_id"
    );

    public static ItemStack createItemStackFromCustomItem(CustomItem cItem, int amount) {
        Material mat = cItem.getMaterial();
        ItemStack stack = new ItemStack(mat, amount);

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        ChatColor rarityColor = cItem.getRarity().getColor();
        String stars = "★".repeat(cItem.getUpgradeLevel());
        meta.setDisplayName(rarityColor + cItem.getBaseName() + " " + stars);

        List<String> lore = new ArrayList<>();
        lore.add(""); // Blank line for spacing
        lore.add(ChatColor.GRAY + "Level Requirement: " + ChatColor.WHITE + cItem.getLevelRequirement());

        if (cItem.getClassRequirement() != null && !cItem.getClassRequirement().equalsIgnoreCase("ANY")) {
            lore.add(ChatColor.GRAY + "Class Requirement: " + ChatColor.WHITE + cItem.getClassRequirement().toUpperCase());
        }
        lore.add(""); // Another blank line for spacing

        if (cItem.getHp() != 0)   lore.add(ChatColor.RED + "❤ " + ChatColor.GRAY + "Health: " + ChatColor.RED + "+" + cItem.getHp());
        if (cItem.getDef() != 0)  lore.add(ChatColor.GRAY + "⛊ " + ChatColor.GRAY + "Defence: " + ChatColor.WHITE + "+" + cItem.getDef());
        if (cItem.getStr() != 0)  lore.add(ChatColor.BLUE + "☠ " + ChatColor.GRAY + "Strength: " + ChatColor.WHITE + "+" + cItem.getStr());
        if (cItem.getAgi() != 0)  lore.add(ChatColor.GREEN + "≈ " + ChatColor.GRAY + "Agility: " + ChatColor.WHITE + "+" + cItem.getAgi());
        if (cItem.getIntel() != 0) lore.add(ChatColor.AQUA + "♦ " + ChatColor.GRAY + "Intelligence: " + ChatColor.WHITE + "+" + cItem.getIntel());
        if (cItem.getDex() != 0)  lore.add(ChatColor.YELLOW + "➹ " + ChatColor.GRAY + "Dexterity: " + ChatColor.WHITE + "+" + cItem.getDex());
        lore.add(""); // Blank line before rarity

        //lore.add(ChatColor.GRAY + "Upgrade Level: " + ChatColor.GREEN + cItem.getUpgradeLevel() + "/5");
        lore.add(rarityColor + "" + ChatColor.BOLD + cItem.getRarity().name()); // Rarity with color and bold

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); // Hide item attributes
        meta.setUnbreakable(true); // Make the item unbreakable
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);; // Hide item attributes

        // Store unique data in the PersistentDataContainer
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ITEM_ID_KEY, PersistentDataType.INTEGER, cItem.getId()); // Store the item ID
        pdc.set(UPGRADE_LEVEL_KEY, PersistentDataType.INTEGER, cItem.getUpgradeLevel()); // Store the upgrade level

        stack.setItemMeta(meta);
        return stack;
    }


    // Retrieve upgrade level from PDC
    public static int getUpgradeLevel(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return 0;
        ItemMeta meta = stack.getItemMeta();
        return meta.getPersistentDataContainer().getOrDefault(UPGRADE_LEVEL_KEY, PersistentDataType.INTEGER, 0);
    }

    // Update an existing item's upgrade level
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
