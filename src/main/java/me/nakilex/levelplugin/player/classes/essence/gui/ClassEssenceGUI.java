package me.nakilex.levelplugin.player.classes.essence.gui;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClassEssenceGUI {

    public static final String TITLE = "Class Essences";
    private static final int[] ESSENCE_SLOTS = {11, 13, 15};
    private static final Map<UUID, List<GuiWidget>> WIDGETS = new HashMap<>();

    private ClassEssenceGUI() {}

    public static void open(Player player) {
        Inventory inv = GuiBuilder.create(27, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();
        StatsManager statsManager = StatsManager.getInstance();
        StatsManager.PlayerStats ps = statsManager.getPlayerStats(player.getUniqueId());
        int unlockedSlots = statsManager.getUnlockedEssenceSlots(player);
        List<GuiWidget> widgets = buildWidgets(statsManager, ps, unlockedSlots);
        WIDGETS.put(player.getUniqueId(), widgets);
        renderWidgets(inv, player, widgets);
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

    private static List<GuiWidget> buildWidgets(StatsManager statsManager,
                                                StatsManager.PlayerStats ps,
                                                int unlockedSlots) {
        List<GuiWidget> widgets = new ArrayList<>();
        for (int i = 0; i < ESSENCE_SLOTS.length; i++) {
            int slot = ESSENCE_SLOTS[i];
            int idx = i;
            widgets.add(new ActionWidget(slot,
                    context -> createEssenceItem(statsManager, ps, unlockedSlots, idx),
                    null));
        }
        return widgets;
    }

    private static ItemStack createEssenceItem(StatsManager statsManager,
                                               StatsManager.PlayerStats ps,
                                               int unlockedSlots,
                                               int slotIndex) {
        if (slotIndex >= unlockedSlots) {
            return createLockedSlotItem(slotIndex, statsManager);
        }
        ItemStack essence = ps.essenceSlots[slotIndex];
        if (essence != null) {
            if (ps.equippedEssences[slotIndex]) {
                ClassEssence.setEquipped(essence, true);
            }
            ClassEssence.addSlotTips(essence);
        }
        return essence;
    }

    private static void renderWidgets(Inventory inventory, Player player, List<GuiWidget> widgets) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }
}
