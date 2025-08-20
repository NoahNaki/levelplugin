package me.nakilex.levelplugin.items.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.events.WeaponEquipEvent;
import me.nakilex.levelplugin.items.events.WeaponEquipEvent.EquipMethod;
import me.nakilex.levelplugin.items.events.WeaponEquipEvent.HandSlot;
import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import me.nakilex.levelplugin.items.data.ArmorType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Material;

import java.util.Set;

public class WeaponListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHotbarSlotChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        int oldSlot = event.getPreviousSlot();
        int newSlot = event.getNewSlot();

        ItemStack oldItem = player.getInventory().getItem(oldSlot);
        ItemStack newItem = player.getInventory().getItem(newSlot);


        // If the items are effectively the same, skip
        if (isSameItem(oldItem, newItem)) {
            // Nothing to do if items haven't changed
            return;
        }

        // Previously this handler ignored hotbar changes when either slot held
        // armor to avoid conflicts with ArmorListener. However, this prevented
        // weapon stats from being removed when switching from a weapon to an
        // armor item in the hotbar.  We now allow the event to continue and
        // let WeaponStatsListener decide whether anything needs to be done.

        // Declare these variables so they can be used later.
        WeaponType oldWeapon = WeaponType.matchType(oldItem);
        WeaponType newWeapon = WeaponType.matchType(newItem);
        CustomItem customOld = ItemManager.getInstance().getCustomItemFromItemStack(oldItem);
        CustomItem customNew = ItemManager.getInstance().getCustomItemFromItemStack(newItem);

        // If neither item is recognized as a weapon and neither has custom data, skip the event.
        if (oldWeapon == null && newWeapon == null && customOld == null && customNew == null) {
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
        Bukkit.getPluginManager().callEvent(equipEvent);

        if (equipEvent.isCancelled()) {
            event.setCancelled(true);
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack mainItem = event.getMainHandItem();
        ItemStack offItem  = event.getOffHandItem();

        // Removed verbose swap logging



        // Also skip if neither item is a weapon.
        WeaponType mainWeapon = WeaponType.matchType(mainItem);
        WeaponType offWeapon = WeaponType.matchType(offItem);
        if (mainWeapon == null && offWeapon == null) {
            // Skip event if neither item is a weapon
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
        // Fire swap events
        Bukkit.getPluginManager().callEvent(mainSwapEvent);
        if (mainSwapEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }


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
        Bukkit.getPluginManager().callEvent(offSwapEvent);
        if (offSwapEvent.isCancelled()) {
            event.setCancelled(true);
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // Typically off-hand is slot 40
        if (event.getSlot() != 40) return;


        ItemStack cursor = event.getCursor();
        ItemStack offItem = event.getCurrentItem();

        InventoryAction act = event.getAction();

        // ------------------------------------------------------------------
        // Placement INTO the off-hand slot
        // ------------------------------------------------------------------
        if (act == InventoryAction.PLACE_ALL
            || act == InventoryAction.PLACE_ONE
            || act == InventoryAction.PLACE_SOME
            || act == InventoryAction.SWAP_WITH_CURSOR) {

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

            return;
        }

        // ------------------------------------------------------------------
        // Removal FROM the off-hand slot (pickup/shift-click/etc.)
        // ------------------------------------------------------------------
        if (act == InventoryAction.PICKUP_ALL
            || act == InventoryAction.PICKUP_ONE
            || act == InventoryAction.PICKUP_SOME
            || act == InventoryAction.MOVE_TO_OTHER_INVENTORY
            || act == InventoryAction.HOTBAR_SWAP) {

            if (offItem != null && offItem.getType() != Material.AIR) {
                WeaponEquipEvent we = new WeaponEquipEvent(
                    player,
                    EquipMethod.MANUAL,
                    WeaponType.matchType(null),
                    HandSlot.OFF_HAND,
                    offItem,
                    null
                );
                Bukkit.getPluginManager().callEvent(we);
                if (we.isCancelled()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClickMainHand(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        Inventory clicked = event.getClickedInventory();
        if (clicked == null || !clicked.equals(player.getInventory())) return;

        int handSlot = player.getInventory().getHeldItemSlot();
        if (event.getSlot() != handSlot) return;

        InventoryAction action = event.getAction();
        ItemStack oldItem = event.getCurrentItem();
        ItemStack newItem = event.getCursor();

        switch (action) {
            // **** removal from hand (including shift-click) ****
            case PICKUP_ALL:
            case PICKUP_ONE:
            case PICKUP_SOME:
            case MOVE_TO_OTHER_INVENTORY:
                fireManualEquip(player, oldItem, null, event);
                break;

            // **** placement into hand ****
            case PLACE_ALL:
            case PLACE_ONE:
            case PLACE_SOME:
            case SWAP_WITH_CURSOR:
                fireManualEquip(player, oldItem, newItem, event);
                break;

            default:
                return;
        }
    }

    /** Now at class level, not nested inside another method! */
    private void fireManualEquip(Player p,
                                 ItemStack oldIt,
                                 ItemStack newIt,
                                 InventoryClickEvent event) {
        if (isSameItem(oldIt, newIt)) return;

        WeaponType newType = WeaponType.matchType(newIt);
        WeaponEquipEvent we = new WeaponEquipEvent(
            p,
            EquipMethod.MANUAL,
            newType,
            HandSlot.MAIN_HAND,
            oldIt,
            newIt
        );
        Bukkit.getPluginManager().callEvent(we);
        if (we.isCancelled()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShiftClickFromTopInv(InventoryClickEvent event) {
        // only shift-click
        if (event.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // ensure they clicked *outside* their own bottom inventory (i.e. a chest, workbench, etc.)
        Inventory clicked = event.getClickedInventory();
        Inventory bottom = event.getView().getBottomInventory();
        if (clicked == null || clicked.equals(bottom)) return;

        // schedule one tick later so Bukkit has already moved the item into the player inv
        new BukkitRunnable() {
            @Override public void run() {
                int handSlot = player.getInventory().getHeldItemSlot();
                ItemStack inHand = player.getInventory().getItem(handSlot);

                CustomItem inst = ItemManager.getInstance().getCustomItemFromItemStack(inHand);
                Set<Integer> eq = StatsManager.getInstance().getEquippedItems(player.getUniqueId());
                // if it's a new custom weapon not yet applied, fire equip
                if (inst != null && !eq.contains(inst.getId())) {
                    WeaponEquipEvent we = new WeaponEquipEvent(
                        player,
                        EquipMethod.MANUAL,
                        WeaponType.matchType(inHand),
                        HandSlot.MAIN_HAND,
                        null,
                        inHand
                    );
                    Bukkit.getPluginManager().callEvent(we);
                }
            }
        }.runTask(Main.getInstance());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack dropped = event.getItemDrop().getItemStack();

        if (me.nakilex.levelplugin.items.utils.ItemUtil.isSoulbound(dropped)) {
            event.setCancelled(true);
            return;
        }

        CustomItem inst = ItemManager.getInstance().getCustomItemFromItemStack(dropped);
        Set<Integer> eq = StatsManager.getInstance().getEquippedItems(player.getUniqueId());
        // if they dropped a weapon you currently had applied
        if (inst != null && eq.contains(inst.getId())) {
            WeaponEquipEvent we = new WeaponEquipEvent(
                player,
                EquipMethod.OTHER,
                WeaponType.matchType(dropped),
                HandSlot.MAIN_HAND,
                dropped,
                null
            );
            Bukkit.getPluginManager().callEvent(we);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        ItemStack picked = event.getItem().getItemStack();

        CustomItem inst = ItemManager.getInstance().getCustomItemFromItemStack(picked);
        if (inst == null) return;

        // wait one tick for the item to actually land in the player's inventory/hand
        new BukkitRunnable() {
            @Override public void run() {
                int handSlot = player.getInventory().getHeldItemSlot();
                ItemStack nowInHand = player.getInventory().getItem(handSlot);
                // if it's our same custom item and not yet applied
                if (nowInHand != null &&
                    inst.getUuid().equals(ItemUtil.getItemUUID(nowInHand))) {
                    Set<Integer> eq = StatsManager.getInstance().getEquippedItems(player.getUniqueId());
                    if (!eq.contains(inst.getId())) {
                        WeaponEquipEvent we = new WeaponEquipEvent(
                            player,
                            EquipMethod.MANUAL,
                            WeaponType.matchType(nowInHand),
                            HandSlot.MAIN_HAND,
                            null,
                            nowInHand
                        );
                        Bukkit.getPluginManager().callEvent(we);
                    }
                }
            }
        }.runTask(Main.getInstance());
    }



    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShiftClickFromStorage(InventoryClickEvent event) {
        // only shift-click
        if (event.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        // make sure they clicked *inside* their own bottom inventory (not off-hand GUI, not chest)
        Inventory clickedInv = event.getClickedInventory();
        Inventory bottom = event.getView().getBottomInventory();
        if (clickedInv == null || !clickedInv.equals(bottom)) return;

        // raw slot < bottom size: they're in the player inventory
        int raw = event.getRawSlot();
        if (raw < 0 || raw >= bottom.getSize()) return;

        // we only care if they clicked *storage* (not hotbar).  Storage in the bottom inv is raw 0–26.
        if (raw >= 27) return;

        // schedule one-tick later so the move has already happened
        new BukkitRunnable() {
            @Override public void run() {
                int handSlot = player.getInventory().getHeldItemSlot();
                ItemStack inHand = player.getInventory().getItem(handSlot);

                // is it a weapon you haven't already equipped?
                CustomItem inst = ItemManager.getInstance()
                    .getCustomItemFromItemStack(inHand);
                Set<Integer> equipped = StatsManager
                    .getInstance()
                    .getEquippedItems(player.getUniqueId());

                if (inst != null && !equipped.contains(inst.getId())) {
                    // fire the manual‐equip event
                    WeaponEquipEvent we = new WeaponEquipEvent(
                        player,
                        EquipMethod.MANUAL,
                        WeaponType.matchType(inHand),
                        HandSlot.MAIN_HAND,
                        null,    // oldWeapon was empty
                        inHand
                    );
                    Bukkit.getPluginManager().callEvent(we);
                }
            }
        }.runTask(Main.getInstance());
    }



    private boolean isSameItem(ItemStack a, ItemStack b) {
        if (isAirOrNull(a) && isAirOrNull(b)) return true;
        if (isAirOrNull(a) != isAirOrNull(b)) return false;
        return a != null && b != null && a.isSimilar(b);
    }

    private boolean isAirOrNull(ItemStack item) {
        return (item == null || item.getType().isAir());
    }

}
