package me.nakilex.levelplugin.items.listeners;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.events.WeaponEquipEvent;
import me.nakilex.levelplugin.items.events.WeaponEquipEvent.EquipMethod;
import me.nakilex.levelplugin.items.events.WeaponEquipEvent.HandSlot;
import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.items.managers.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import me.nakilex.levelplugin.items.data.ArmorType;

public class WeaponListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHotbarSlotChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        int oldSlot = event.getPreviousSlot();
        int newSlot = event.getNewSlot();

        ItemStack oldItem = player.getInventory().getItem(oldSlot);
        ItemStack newItem = player.getInventory().getItem(newSlot);

        // DEBUG logging
        Bukkit.getLogger().info("[DEBUG][WeaponListener] onHotbarSlotChange fired.");
        Bukkit.getLogger().info("  OldSlot=" + oldSlot + " NewSlot=" + newSlot
            + " OldItem=" + debugItem(oldItem)
            + " NewItem=" + debugItem(newItem));

        // If the items are effectively the same, skip
        if (isSameItem(oldItem, newItem)) {
            Bukkit.getLogger().info("  Skipping event because items are the same (or both null).");
            return;
        }

        // Skip if either item is recognized as armor
        if ((oldItem != null && ArmorType.matchType(oldItem) != null) ||
            (newItem != null && ArmorType.matchType(newItem) != null)) {
            Bukkit.getLogger().info("  Skipping event because one of the items is recognized as armor.");
            return;
        }

        // Declare these variables so they can be used later.
        WeaponType oldWeapon = WeaponType.matchType(oldItem);
        WeaponType newWeapon = WeaponType.matchType(newItem);
        CustomItem customOld = ItemManager.getInstance().getCustomItemFromItemStack(oldItem);
        CustomItem customNew = ItemManager.getInstance().getCustomItemFromItemStack(newItem);

        // If neither item is recognized as a weapon and neither has custom data, skip the event.
        if (oldWeapon == null && newWeapon == null && customOld == null && customNew == null) {
            Bukkit.getLogger().info("  Skipping event because neither item is recognized as a weapon or custom weapon.");
            return;
        }

        // Fire the WeaponEquipEvent for the hotbar change.
        WeaponEquipEvent equipEvent = new WeaponEquipEvent(
            player,
            WeaponEquipEvent.EquipMethod.HELD_ITEM_CHANGE,
            newWeapon, // newWeapon may be null if the new item isn't recognized by WeaponType
            WeaponEquipEvent.HandSlot.MAIN_HAND,
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

        // *** NEW CHECKS: Skip if either item is recognized as armor ***
        if ((mainItem != null && ArmorType.matchType(mainItem) != null) ||
            (offItem != null && ArmorType.matchType(offItem) != null)) {
            Bukkit.getLogger().info("  Skipping swap event because one of the items is recognized as armor.");
            return;
        }

        // Also skip if neither item is a weapon.
        WeaponType mainWeapon = WeaponType.matchType(mainItem);
        WeaponType offWeapon = WeaponType.matchType(offItem);
        if (mainWeapon == null && offWeapon == null) {
            Bukkit.getLogger().info("  Skipping swap event because neither item is recognized as a weapon.");
            return;
        }

        // Fire for main hand: old = mainItem, new = offItem.
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

        // Fire for off-hand: old = offItem, new = mainItem.
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
