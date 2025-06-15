package me.nakilex.levelplugin.effectdemo;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Simple GUI showcasing a few EffectLib particle effects.
 */
public class EffectDemoGUI {
    private static final int SIZE = 27;

    /**
     * Open the demo GUI for the player.
     */
    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, ChatColor.BLUE + "FX Demo");

        // filler
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) {
            fm.setDisplayName(" ");
            filler.setItemMeta(fm);
        }
        for (int i = 0; i < SIZE; i++) inv.setItem(i, filler);

        // place demo items
        addItem(inv, 10, Material.BLAZE_ROD, "Helix Effect");
        addItem(inv, 11, Material.NETHER_STAR, "Sphere Effect");
        addItem(inv, 12, Material.FIRE_CHARGE, "Tornado Effect");
        addItem(inv, 13, Material.ENDER_EYE, "Atom Effect");
        addItem(inv, 14, Material.SNOWBALL, "Cone Effect");
        addItem(inv, 15, Material.IRON_BARS, "Cylinder Effect");
        addItem(inv, 16, Material.BONE, "DNA Effect");

        addItem(inv, 19, Material.CAKE, "Donut Effect");
        addItem(inv, 20, Material.WATER_BUCKET, "Fountain Effect");
        addItem(inv, 21, Material.APPLE, "Heart Effect");
        addItem(inv, 22, Material.TNT, "Big Bang Effect");
        addItem(inv, 23, Material.ENDER_PEARL, "Vortex Effect");
        addItem(inv, 24, Material.PRISMARINE_SHARD, "Wave Effect");
        addItem(inv, 25, Material.AMETHYST_SHARD, "Star Effect");

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
