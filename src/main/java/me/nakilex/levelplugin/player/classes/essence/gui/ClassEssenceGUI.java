package me.nakilex.levelplugin.player.classes.essence.gui;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ClassEssenceGUI {

    public static final String TITLE = ChatColor.BLACK + "Class Essences";
    private static final int[] ESSENCE_SLOTS = {11, 13, 15};

    private ClassEssenceGUI() {}

    public static void open(Player player) {
        Inventory inv = GuiBuilder.create(27, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        for (int i = 0; i < ESSENCE_SLOTS.length; i++) {
            ItemStack essence = ps.essenceSlots[i];
            if (essence != null) {
                if (ps.equippedEssences[i]) {
                    ClassEssence.setEquipped(essence, true);
                }
                inv.setItem(ESSENCE_SLOTS[i], essence);
            } else {
                inv.setItem(ESSENCE_SLOTS[i], null);
            }
        }
        player.openInventory(inv);
    }

    public static int indexFromSlot(int rawSlot) {
        for (int i = 0; i < ESSENCE_SLOTS.length; i++) {
            if (ESSENCE_SLOTS[i] == rawSlot) return i;
        }
        return -1;
    }

    public static int slotFromIndex(int index) {
        return ESSENCE_SLOTS[index];
    }
}
