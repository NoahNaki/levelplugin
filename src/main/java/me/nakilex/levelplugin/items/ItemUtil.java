package me.nakilex.levelplugin.items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ItemUtil {

    private static final NamespacedKey ITEM_ID_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ItemUtil.class), "custom_item_id");

    public static ItemStack createItemStackFromCustomItem(CustomItem cItem, int amount) {
        Material mat = cItem.getMaterial();
        ItemStack stack = new ItemStack(mat, amount);

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        ChatColor rarityColor = cItem.getRarity().getColor();
        meta.setDisplayName(rarityColor + cItem.getName());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Level Req: " + cItem.getLevelRequirement());

        // Only add class requirement line if it's not ANY
        if (cItem.getClassRequirement() != null && !cItem.getClassRequirement().equalsIgnoreCase("ANY")) {
            lore.add(ChatColor.GRAY + "Class Req: " + cItem.getClassRequirement().toUpperCase());
        }

        lore.add(rarityColor + "" + ChatColor.BOLD + cItem.getRarity().name());
        lore.add("");

        if (cItem.getHp() != 0)   lore.add(ChatColor.RED + "HP: +" + cItem.getHp());
        if (cItem.getDef() != 0)  lore.add(ChatColor.GRAY + "DEF: +" + cItem.getDef());
        if (cItem.getStr() != 0)  lore.add(ChatColor.RED + "STR: +" + cItem.getStr());
        if (cItem.getAgi() != 0)  lore.add(ChatColor.GREEN + "AGI: +" + cItem.getAgi());
        if (cItem.getIntel() != 0) lore.add(ChatColor.LIGHT_PURPLE + "INT: +" + cItem.getIntel());
        if (cItem.getDex() != 0)  lore.add(ChatColor.AQUA + "DEX: +" + cItem.getDex());

        meta.setLore(lore);

        // Store numeric ID in the PDC
        meta.getPersistentDataContainer().set(ITEM_ID_KEY, PersistentDataType.INTEGER, cItem.getId());
        stack.setItemMeta(meta);
        return stack;
    }

    public static int getCustomItemId(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return -1;
        ItemMeta meta = stack.getItemMeta();
        Integer value = meta.getPersistentDataContainer().get(ITEM_ID_KEY, PersistentDataType.INTEGER);
        return (value != null) ? value : -1;
    }
}
