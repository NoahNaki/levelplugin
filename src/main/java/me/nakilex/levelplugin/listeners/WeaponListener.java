package me.nakilex.levelplugin.listeners;

import me.nakilex.levelplugin.events.WeaponEquipEvent;
import me.nakilex.levelplugin.events.WeaponEquipEvent.EquipMethod;
import me.nakilex.levelplugin.events.WeaponEquipEvent.HandSlot;
import me.nakilex.levelplugin.events.WeaponType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

public class WeaponListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHotbarSlotChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        int oldSlot = event.getPreviousSlot();
        int newSlot = event.getNewSlot();

        ItemStack oldItem = player.getInventory().getItem(oldSlot);
        ItemStack newItem = player.getInventory().getItem(newSlot);

        // DEBUG
        Bukkit.getLogger().info("[DEBUG][WeaponListener] onHotbarSlotChange fired.");
        Bukkit.getLogger().info("  OldSlot=" + oldSlot + " NewSlot=" + newSlot
            + " OldItem=" + debugItem(oldItem)
            + " NewItem=" + debugItem(newItem));

        // If old and new are effectively the same, skip
        if (isSameItem(oldItem, newItem)) {
            Bukkit.getLogger().info("  Skipping event because items are the same (or both null).");
            return;
        }

        // Fire WeaponEquipEvent
        WeaponType newType = WeaponType.matchType(newItem);
        WeaponEquipEvent equipEvent = new WeaponEquipEvent(
            player,
            EquipMethod.HELD_ITEM_CHANGE,
            newType,
            HandSlot.MAIN_HAND,
            oldItem,
            newItem
        );

        Bukkit.getLogger().info("  Calling WeaponEquipEvent: old=" + debugItem(oldItem)
            + " new=" + debugItem(newItem));
        Bukkit.getPluginManager().callEvent(equipEvent);

        if (equipEvent.isCancelled()) {
            Bukkit.getLogger().info("  WeaponEquipEvent was cancelled by something else.");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack mainItem = event.getMainHandItem();
        ItemStack offItem  = event.getOffHandItem();

        Bukkit.getLogger().info("[DEBUG][WeaponListener] onSwapHand fired.");
        Bukkit.getLogger().info("  mainItem=" + debugItem(mainItem)
            + " offItem=" + debugItem(offItem));

        // If they're the same, skip
        if (isSameItem(mainItem, offItem)) {
            Bukkit.getLogger().info("  Skipping event because items are the same.");
            return;
        }

        // Fire for main hand
        WeaponType mainNewType = WeaponType.matchType(offItem);
        WeaponEquipEvent mainSwapEvent = new WeaponEquipEvent(
            player,
            EquipMethod.OFFHAND_SWAP,
            mainNewType,
            HandSlot.MAIN_HAND,
            mainItem,
            offItem
        );
        Bukkit.getLogger().info("  Calling main-hand swap event: old=" + debugItem(mainItem)
            + " new=" + debugItem(offItem));
        Bukkit.getPluginManager().callEvent(mainSwapEvent);
        if (mainSwapEvent.isCancelled()) {
            Bukkit.getLogger().info("  mainSwapEvent was cancelled => stopping swap.");
            event.setCancelled(true);
            return;
        }

        Bukkit.getLogger().info("[DEBUG] Main hand swap event completed successfully."); // DEBUG

        // Fire for off-hand
        WeaponType offNewType = WeaponType.matchType(mainItem);
        WeaponEquipEvent offSwapEvent = new WeaponEquipEvent(
            player,
            EquipMethod.OFFHAND_SWAP,
            offNewType,
            HandSlot.OFF_HAND,
            offItem,
            mainItem
        );
        Bukkit.getLogger().info("  Calling off-hand swap event: old=" + debugItem(offItem)
            + " new=" + debugItem(mainItem));
        Bukkit.getPluginManager().callEvent(offSwapEvent);
        if (offSwapEvent.isCancelled()) {
            Bukkit.getLogger().info("  offSwapEvent was cancelled => stopping swap.");
            event.setCancelled(true);
        }

        Bukkit.getLogger().info("[DEBUG] Off-hand swap event completed successfully."); // DEBUG
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // Typically off-hand is slot 40
        if (event.getSlot() != 40) return;

        Bukkit.getLogger().info("[DEBUG][WeaponListener] onInventoryClick fired for off-hand (slot=40).");
        Bukkit.getLogger().info("  action=" + event.getAction());

        ItemStack cursor = event.getCursor();
        ItemStack offItem = event.getCurrentItem();

        Bukkit.getLogger().info("  offItem=" + debugItem(offItem)
            + " cursor=" + debugItem(cursor));

        // Placing an item into off-hand
        if (event.getAction() == InventoryAction.PLACE_ALL
            || event.getAction() == InventoryAction.PLACE_ONE
            || event.getAction() == InventoryAction.PLACE_SOME) {

            if (!isSameItem(offItem, cursor)) {
                WeaponType newType = WeaponType.matchType(cursor);
                WeaponEquipEvent we = new WeaponEquipEvent(
                    player,
                    EquipMethod.MANUAL,
                    newType,
                    HandSlot.OFF_HAND,
                    offItem,
                    cursor
                );
                Bukkit.getPluginManager().callEvent(we);
                if (we.isCancelled()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private boolean isSameItem(ItemStack a, ItemStack b) {
        if (isAirOrNull(a) && isAirOrNull(b)) return true;
        if (isAirOrNull(a) != isAirOrNull(b)) return false;
        return a != null && b != null && a.isSimilar(b);
    }

    private boolean isAirOrNull(ItemStack item) {
        return (item == null || item.getType().isAir());
    }

    private String debugItem(ItemStack item) {
        if (isAirOrNull(item)) return "(null)";
        return item.getType().name() + " x" + item.getAmount();
    }
}
