package me.nakilex.levelplugin.debug.listeners;

import me.nakilex.levelplugin.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.StringJoiner;

/**
 * Temporary always-on debug listener for inventory slot mapping diagnostics.
 */
public class InventorySlotDebugListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory clickedInventory = event.getClickedInventory();
        Inventory top = event.getView().getTopInventory();
        Inventory bottom = event.getView().getBottomInventory();

        String clickedLabel;
        if (clickedInventory == null) {
            clickedLabel = "OUTSIDE";
        } else if (clickedInventory.equals(top)) {
            clickedLabel = "TOP";
        } else if (clickedInventory.equals(bottom)) {
            clickedLabel = "BOTTOM";
        } else {
            clickedLabel = "OTHER";
        }

        Main.getInstance().getLogger().info("[InvSlotDebug]"
                + " player=" + player.getName()
                + " viewTopType=" + top.getType()
                + " clickedInv=" + clickedLabel
                + " slotType=" + event.getSlotType()
                + " rawSlot=" + event.getRawSlot()
                + " slot=" + event.getSlot()
                + " action=" + event.getAction()
                + " click=" + event.getClick()
                + " hotbarButton=" + event.getHotbarButton()
                + " cursor=" + formatItem(event.getCursor())
                + " current=" + formatItem(event.getCurrentItem())
                + " cancelled=" + event.isCancelled());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        StringJoiner slots = new StringJoiner(",");
        for (Integer raw : event.getRawSlots()) {
            slots.add(String.valueOf(raw));
        }

        Main.getInstance().getLogger().info("[InvSlotDebug]"
                + " player=" + player.getName()
                + " dragType=" + event.getType()
                + " rawSlots=[" + slots + "]"
                + " oldCursor=" + formatItem(event.getOldCursor())
                + " newCursor=" + formatItem(event.getCursor())
                + " cancelled=" + event.isCancelled());
    }

    private String formatItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "AIR";
        }
        return item.getType() + "x" + item.getAmount();
    }
}
