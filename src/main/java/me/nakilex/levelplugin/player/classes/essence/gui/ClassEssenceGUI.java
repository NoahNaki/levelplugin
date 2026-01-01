package me.nakilex.levelplugin.player.classes.essence.gui;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ClassEssenceGUI {

    public static final String TITLE = "Class Essences";
    private static final int[] ESSENCE_SLOTS = {11, 13, 15};

    private ClassEssenceGUI() {}

    public static void open(Player player) {
        Inventory inv = GuiBuilder.create(27, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();
        StatsManager statsManager = StatsManager.getInstance();
        StatsManager.PlayerStats ps = statsManager.getPlayerStats(player.getUniqueId());
        int unlockedSlots = statsManager.getUnlockedEssenceSlots(player);
        for (int i = 0; i < ESSENCE_SLOTS.length; i++) {
            if (i >= unlockedSlots) {
                inv.setItem(ESSENCE_SLOTS[i], createLockedSlotItem(i, statsManager));
                continue;
            }
            ItemStack essence = ps.essenceSlots[i];
            if (essence != null) {
                if (ps.equippedEssences[i]) {
                    ClassEssence.setEquipped(essence, true);
                }
                ClassEssence.addSlotTips(essence);
                inv.setItem(ESSENCE_SLOTS[i], essence);
            } else {
                inv.setItem(ESSENCE_SLOTS[i], null);
            }
        }
        player.openInventory(inv);
    }

    private static ItemStack createLockedSlotItem(int slotIndex, StatsManager statsManager) {
        ItemStack locked = GuiUtil.getNexoItem("lock", ChatColor.RED + "Locked");
        ItemMeta meta = locked.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            if (slotIndex == 1) {
                lore.addAll(TooltipUtil.bulletList("Complete the Essence Weaver's Lesson quest to unlock."));
            } else if (slotIndex == 2) {
                int level = statsManager.getEssenceSlotUnlockLevel(2);
                lore.addAll(TooltipUtil.bulletList("Reach level " + level + " to unlock."));
            }
            meta.setLore(lore);
            locked.setItemMeta(meta);
        }
        return locked;
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
