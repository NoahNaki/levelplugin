package me.nakilex.levelplugin.effectdemo;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.nakilex.levelplugin.effectdemo.DemoEffects;

/**
 * Simple GUI showcasing a few EffectLib particle effects.
 */
public class EffectDemoGUI {
    /**
     * Inventory size must be a multiple of 9. Round up to the next valid size
     * while capping at a single chest (54 slots).
     */
    private static final int SIZE = Math.min(((DemoEffects.values().length + 8) / 9) * 9, 54);

    /**
     * Open the demo GUI for the player.
     */
    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, ChatColor.BLUE + "FX Demo");

        int slot = 0;
        for (DemoEffects effect : DemoEffects.values()) {
            addItem(inv, slot++, effect.getIcon(), effect.getLabel());
        }

        player.openInventory(inv);
    }

    private static void addItem(Inventory inv, int slot, Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + name);
            it.setItemMeta(meta);
        }
        inv.setItem(slot, it);
    }
}
