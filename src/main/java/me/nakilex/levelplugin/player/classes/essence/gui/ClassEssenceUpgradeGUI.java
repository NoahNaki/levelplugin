package me.nakilex.levelplugin.player.classes.essence.gui;

import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Paginated menu for selecting a class essence to upgrade. Mirrors the
 * blacksmith interface using Nexo items for navigation and filler panes.
 */
public class ClassEssenceUpgradeGUI implements Listener {

    public static final String TITLE = ChatColor.DARK_PURPLE + "Essence Upgrade";
    private static final int[] ESSENCE_SLOTS = GuiUtil.PAGED_SLOTS;
    private static final int ITEMS_PER_PAGE = ESSENCE_SLOTS.length;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;

    private static final Map<UUID, Integer> PAGE = new HashMap<>();
    private static final Map<UUID, List<ItemStack>> CACHE = new HashMap<>();

    /**
     * Public constructor required for registering event listeners. The GUI
     * logic is otherwise driven by static helpers, so this constructor has no
     * state.
     */
    public ClassEssenceUpgradeGUI() {}

    public static void open(Player player) {
        open(player, 0);
    }

    public static void open(Player player, int page) {
        List<ItemStack> essences = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && ClassEssence.isEssence(item)) {
                essences.add(item);
            }
        }
        CACHE.put(player.getUniqueId(), essences);
        PAGE.put(player.getUniqueId(), page);

        Inventory inv = GuiBuilder.create(54, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .build();

        int start = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && start + i < essences.size(); i++) {
            inv.setItem(ESSENCE_SLOTS[i], essences.get(start + i).clone());
        }
        if (page > 0) {
            inv.setItem(PREV_SLOT, GuiUtil.getNexoItem("arrow_left", ChatColor.GREEN + "Previous"));
        }
        if (start + ITEMS_PER_PAGE < essences.size()) {
            inv.setItem(NEXT_SLOT, GuiUtil.getNexoItem("arrow_right", ChatColor.GREEN + "Next"));
        }
        player.openInventory(inv);
    }

    private static int indexFromSlot(int slot) {
        for (int i = 0; i < ESSENCE_SLOTS.length; i++) {
            if (ESSENCE_SLOTS[i] == slot) return i;
        }
        return -1;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!TITLE.equals(e.getView().getTitle())) return;
        e.setCancelled(true);
        Player player = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();
        if (slot == PREV_SLOT) {
            int p = PAGE.getOrDefault(player.getUniqueId(), 0);
            open(player, Math.max(0, p - 1));
            return;
        }
        if (slot == NEXT_SLOT) {
            int p = PAGE.getOrDefault(player.getUniqueId(), 0);
            open(player, p + 1);
            return;
        }
        int idx = indexFromSlot(slot);
        if (idx >= 0) {
            List<ItemStack> list = CACHE.get(player.getUniqueId());
            int page = PAGE.getOrDefault(player.getUniqueId(), 0);
            int index = page * ITEMS_PER_PAGE + idx;
            if (list != null && index < list.size()) {
                ClassEssenceProgressGUI.open(player, list.get(index));
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!TITLE.equals(e.getView().getTitle())) return;
        UUID id = ((Player) e.getPlayer()).getUniqueId();
        PAGE.remove(id);
        CACHE.remove(id);
    }
}

