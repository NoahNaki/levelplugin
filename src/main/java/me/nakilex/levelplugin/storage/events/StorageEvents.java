package me.nakilex.levelplugin.storage.events;

import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.storage.gui.StorageGUI;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Listens for inventory interactions and delegates them to
 * the appropriate StorageGUI instance if it’s a player’s
 * personal storage.
 */
public class StorageEvents implements Listener {

    /**
     * Tracks which Inventory objects belong to which StorageGUI instance.
     */
    private final Map<Inventory, StorageGUI> trackedInventories = new HashMap<>();

    /**
     * Registers a StorageGUI’s Inventory so we can handle clicks.
     */
    public void registerInventory(StorageGUI storageGUI, Inventory inventory) {
        trackedInventories.put(inventory, storageGUI);
    }

    /**
     * Unregisters an inventory when it’s closed or no longer needed.
     */
    public void unregisterInventory(Inventory inventory) {
        trackedInventories.remove(inventory);
    }

    /**
     * Listen for clicks inside any tracked inventory.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (trackedInventories.containsKey(top)) {
            StorageGUI gui = trackedInventories.get(top);
            if (gui.isFilteredView(top)
                    && gui.isStorageSlot(event.getRawSlot())
                    && event.getRawSlot() < top.getSize()) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player player) {
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                            "Disable the filter to move items.");
                }
                return;
            }
            if (event.getWhoClicked() instanceof Player player
                    && shouldBlockDungeonItem(event, top, player)) {
                event.setCancelled(true);
                return;
            }
            if (gui.isFilteredView(top)) {
                gui.handleClick(event);
                return;
            }
            if (!gui.allowsSoulbound() && event.getClickedInventory() != top) {
                ItemStack cursor = event.getCursor();
                ItemStack current = event.getCurrentItem();
                if (me.nakilex.levelplugin.items.utils.ItemUtil.isSoulbound(cursor) ||
                    me.nakilex.levelplugin.items.utils.ItemUtil.isSoulbound(current)) {
                    event.setCancelled(true);
                    if (event.getWhoClicked() instanceof Player p) {
                        ChatMessageUtil.send(p, ChatMessageUtil.MessageType.ERROR,
                                "Soulbound items cannot be stored here.");
                    }
                    return;
                }
            }
            gui.handleClick(event);
        }
    }

    /**
     * Listen for drag events so menu items cannot be pulled out.
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (trackedInventories.containsKey(top)) {
            StorageGUI gui = trackedInventories.get(top);
            if (gui.isFilteredView(top)) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player player) {
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                            "Disable the filter to move items.");
                }
                return;
            }
            if (event.getWhoClicked() instanceof Player player
                    && shouldBlockDungeonItem(event, top, player)) {
                event.setCancelled(true);
                return;
            }
            if (!gui.allowsSoulbound() && me.nakilex.levelplugin.items.utils.ItemUtil.isSoulbound(event.getOldCursor())) {
                event.setCancelled(true);
                return;
            }
            gui.handleDrag(event);
        }
    }

    /**
     * Listen for the moment an inventory is closed; trigger saving logic.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory closedInventory = event.getInventory();
        if (trackedInventories.containsKey(closedInventory)) {
            HumanEntity human = event.getPlayer();
            if (human instanceof Player) {
                Player player = (Player) human;

                // Optionally save the player's storage on close
                StorageGUI gui = trackedInventories.get(closedInventory);
                gui.cleanupView(closedInventory);
                gui.saveToDisk(); // Or pass to PersonalStorage for saving

                // Unregister this inventory to free up references
                unregisterInventory(closedInventory);
            }
        }
    }

    private boolean shouldBlockDungeonItem(InventoryClickEvent event, Inventory top, Player player) {
        Inventory clicked = event.getClickedInventory();
        if (clicked == null) {
            return false;
        }

        if (clicked.equals(top)) {
            if (ItemUtil.isDungeonItem(event.getCursor())) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                        "Dungeon items cannot be stored here.");
                return true;
            }
            if (event.getAction() == InventoryAction.HOTBAR_SWAP
                    || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
                int hotbar = event.getHotbarButton();
                if (hotbar >= 0) {
                    ItemStack hotbarItem = player.getInventory().getItem(hotbar);
                    if (ItemUtil.isDungeonItem(hotbarItem)) {
                        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                                "Dungeon items cannot be stored here.");
                        return true;
                    }
                }
            }
            return false;
        }

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && clicked.equals(event.getView().getBottomInventory())) {
            if (ItemUtil.isDungeonItem(event.getCurrentItem())) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                        "Dungeon items cannot be stored here.");
                return true;
            }
        }

        return false;
    }

    private boolean shouldBlockDungeonItem(InventoryDragEvent event, Inventory top, Player player) {
        for (var entry : event.getNewItems().entrySet()) {
            int rawSlot = entry.getKey();
            if (rawSlot < top.getSize() && ItemUtil.isDungeonItem(entry.getValue())) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                        "Dungeon items cannot be stored here.");
                return true;
            }
        }
        return false;
    }

}
