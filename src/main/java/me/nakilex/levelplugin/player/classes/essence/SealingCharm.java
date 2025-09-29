package me.nakilex.levelplugin.player.classes.essence;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Factory helpers for sealing charm items used when resealing essences.
 */
public final class SealingCharm {

    private static final NamespacedKey KEY = new NamespacedKey(
            JavaPlugin.getProvidingPlugin(SealingCharm.class),
            "sealing_charm"
    );

    private SealingCharm() {}

    /**
     * Build a new sealing charm stack with the desired amount.
     */
    public static ItemStack create(int amount) {
        ItemStack stack = new ItemStack(Material.PAPER, Math.max(1, amount));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Sealing Charm");
            meta.setLore(List.of(
                    ChatColor.GRAY + "Used to reseal soulbound essences.",
                    ChatColor.GRAY + "Place in the reseal altar with an essence."
            ));
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(KEY, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /**
     * @return true when the provided stack is identified as a sealing charm.
     */
    public static boolean isCharm(ItemStack stack) {
        if (stack == null || stack.getType() != Material.PAPER) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Byte flag = pdc.get(KEY, PersistentDataType.BYTE);
        return flag != null && flag == (byte) 1;
    }
}

