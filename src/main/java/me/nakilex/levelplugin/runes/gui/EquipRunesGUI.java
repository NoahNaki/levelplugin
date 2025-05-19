package me.nakilex.levelplugin.runes.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.runes.manager.RunesManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * GUI for equipping identified runes. Enhanced to support direct-click equipping
 * and proper cancellation only for pickup actions, with null-safe display names.
 */
public class EquipRunesGUI implements Listener {
    private final Main plugin;
    private final RunesManager runesManager;

    public static final String TITLE = ChatColor.DARK_GRAY + "Equip Runes";
    private static final int SIZE = 9;

    public EquipRunesGUI(Main plugin, RunesManager runesManager) {
        this.plugin = plugin;
        this.runesManager = runesManager;
    }

    public Inventory createInventory(Player player) {
        return Bukkit.createInventory(null, SIZE, TITLE);
    }

    public void open(Player player) {
        plugin.getLogger().info("Opening EquipRunesGUI for " + player.getName());
        player.openInventory(createInventory(player));
    }

    private boolean isIdentifiedRune(ItemStack stack) {
        boolean identified = runesManager.isIdentified(stack);
        plugin.getLogger().info("isIdentifiedRune? " + identified + " for stack=" + stack);
        return identified;
    }

    private boolean isEquipGUI(InventoryView view) {
        boolean correct = TITLE.equals(view.getTitle());
        plugin.getLogger().info("isEquipGUI? " + correct + " title=" + view.getTitle());
        return correct;
    }

    // Utility to get display name safely
    private String getSafeName(ItemStack item) {
        if (item == null) return "<none>";
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        return item.getType().toString();
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        InventoryView view = e.getView();
        InventoryAction action = e.getAction();
        ItemStack cursor = e.getCursor();
        ItemStack clicked = e.getCurrentItem();

        plugin.getLogger().info("InventoryClickEvent: action=" + action
            + ", slot=" + e.getSlot() + ", rawSlot=" + e.getRawSlot());
        if (!isEquipGUI(view)) return;

        // 1) Cancel any item pickup from the GUI
        if (e.getClickedInventory() == view.getTopInventory()) {
            if (action == InventoryAction.PICKUP_ONE
                || action == InventoryAction.PICKUP_ALL
                || action == InventoryAction.COLLECT_TO_CURSOR) {
                plugin.getLogger().info("Cancelling pickup from top inventory slot=" + e.getRawSlot());
                e.setCancelled(true);
                return;
            }
        }

        // 2) Shift-click INTO GUI: equip immediately
        if (e.isShiftClick()) {
            ItemStack toShift = e.getClickedInventory() == view.getBottomInventory() ? clicked : null;
            plugin.getLogger().info("Shift-click detected, toShift=" + toShift);
            if (toShift == null || !isIdentifiedRune(toShift)) {
                plugin.getLogger().info("Cancelling shift-click with invalid rune");
                e.setCancelled(true);
            } else {
                plugin.getLogger().info("Equipping rune via shift-click: " + toShift);
                e.setCancelled(true);
                Player p = (Player) e.getWhoClicked();
                boolean equipped = runesManager.equipRune(p, toShift);
                p.closeInventory();
                plugin.getLogger().info("equipRune via shift-click returned " + equipped);
                String name = getSafeName(toShift);
                p.sendMessage(equipped
                    ? ChatColor.GREEN + "Equipped rune: " + name
                    : ChatColor.RED + "Failed to equip rune: " + name);
            }
            return;
        }

        // 3) Direct click placement of rune into GUI → equip
        if (e.getClickedInventory() == view.getTopInventory()
            && cursor != null && cursor.getType() != Material.AIR
            && isIdentifiedRune(cursor)
            && (action == InventoryAction.PLACE_ONE
            || action == InventoryAction.PLACE_ALL
            || action == InventoryAction.PLACE_SOME)) {
            plugin.getLogger().info("Direct-click equipping rune: " + cursor);
            e.setCancelled(true);
            Player p = (Player) e.getWhoClicked();
            boolean equipped = runesManager.equipRune(p, cursor);
            p.closeInventory();
            plugin.getLogger().info("equipRune via direct-click returned " + equipped);
            String name = getSafeName(cursor);
            p.sendMessage(equipped
                ? ChatColor.GREEN + "Equipped rune: " + name
                : ChatColor.RED + "Failed to equip rune: " + name);
            return;
        }

        // 4) Hotbar swap (number-key) into GUI
        if (e.getHotbarButton() != -1) {
            ItemStack hotbarItem = view.getBottomInventory().getItem(e.getHotbarButton());
            plugin.getLogger().info("Hotbar button swap: " + hotbarItem);
            if (!isIdentifiedRune(hotbarItem)) {
                plugin.getLogger().info("Cancelling hotbar swap with invalid rune");
                e.setCancelled(true);
            }
            return;
        }

        // 5) Prevent placing invalid items into GUI
        if (e.getClickedInventory() == view.getTopInventory()
            && cursor != null && cursor.getType() != Material.AIR
            && !isIdentifiedRune(cursor)) {
            plugin.getLogger().info("Cancelling invalid placement: " + cursor);
            e.setCancelled(true);
            return;
        }

        // 6) Clicking on a rune already in the GUI → equip
        if (clicked != null && clicked.getType() != Material.AIR
            && isIdentifiedRune(clicked)
            && e.getClickedInventory() == view.getTopInventory()
            && (action == InventoryAction.PICKUP_ONE || action == InventoryAction.PICKUP_ALL)) {
            plugin.getLogger().info("Equipping existing GUI rune on click: " + clicked);
            e.setCancelled(true);
            Player p = (Player) e.getWhoClicked();
            boolean equipped = runesManager.equipRune(p, clicked);
            p.closeInventory();
            plugin.getLogger().info("equipRune on click returned " + equipped);
            String name = getSafeName(clicked);
            p.sendMessage(equipped
                ? ChatColor.GREEN + "Equipped rune: " + name
                : ChatColor.RED + "Failed to equip rune: " + name);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent e) {
        plugin.getLogger().info("InventoryDragEvent: rawSlots=" + e.getRawSlots()
            + ", newItems=" + e.getNewItems().values());
        if (!isEquipGUI(e.getView())) return;

        e.getNewItems().forEach((slot, stack) -> {
            if (slot < e.getView().getTopInventory().getSize()) {
                plugin.getLogger().info("Drag into slot " + slot + ": " + stack);
                if (!isIdentifiedRune(stack)) {
                    plugin.getLogger().info("Cancelling drag: invalid rune " + stack);
                    e.setCancelled(true);
                }
            }
        });
    }
}
