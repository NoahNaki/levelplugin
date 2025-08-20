package me.nakilex.levelplugin.player.classes.essence.gui;

import me.nakilex.levelplugin.utils.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** GUI for feeding duplicate essences as experience. */
public final class ClassEssenceProgressGUI {

    public static final String TITLE = ChatColor.DARK_PURPLE + "Essence Progress";
    private static final int SACRIFICE_SLOT = 11;
    private static final int DISPLAY_SLOT = 15;

    private static final Map<UUID, ItemStack> TARGET = new HashMap<>();

    private ClassEssenceProgressGUI() {}

    public static void open(Player player, ItemStack essence) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);
        inv.setItem(SACRIFICE_SLOT, null);
        inv.setItem(DISPLAY_SLOT, essence.clone());
        TARGET.put(player.getUniqueId(), essence);
        player.openInventory(inv);
    }

    public static ItemStack getTarget(Player p) {
        return TARGET.get(p.getUniqueId());
    }

    public static void clear(Player p) {
        TARGET.remove(p.getUniqueId());
    }

    public static int getSacrificeSlot() {
        return SACRIFICE_SLOT;
    }

    public static int getDisplaySlot() {
        return DISPLAY_SLOT;
    }
}

