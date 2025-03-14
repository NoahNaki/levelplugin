package me.nakilex.levelplugin.items.listeners;

import me.nakilex.levelplugin.items.events.ArmorEquipEvent;
import me.nakilex.levelplugin.items.events.ArmorEquipEvent.EquipMethod;
import me.nakilex.levelplugin.items.data.ArmorType;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.inventory.ItemStack;

public class ArmorListener implements Listener {

    // ----------------------------------------------------------------
    // 1) RIGHT-CLICK FROM HOTBAR HANDLED MANUALLY
    // ----------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only care about right-click air or block
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack inHand = event.getItem();
        if (inHand == null || inHand.getType() == Material.AIR) return;

        // Check if it's recognized as armor
        ArmorType type = ArmorType.matchType(inHand);
        if (type == null) {
            // Not armor => do nothing
            return;
        }

        // We do our own logic: check requirements, etc.
        // First, let's see if anything is already in that armor slot.
        ItemStack currentArmor = getArmorSlotItem(player, type);

        // If the slot is occupied, SHIFT-click or inventory click is needed to swap.
        // But if it's air, normally vanilla would equip it. We'll replicate that logic:

        // 1) Attempt to parse the item as a CustomItem
        CustomItem cItem = ItemManager.getInstance().getCustomItemFromItemStack(inHand);

        // Let’s decide if they meet the requirement or not
        boolean meetsRequirement = true; // default
        if (cItem != null) {
            int requiredLevel = cItem.getLevelRequirement();
            int playerLevel = LevelManager.getInstance().getLevel(player);
            if (playerLevel < requiredLevel) {
                meetsRequirement = false;
            }
            // If you have class requirements for armor, check them here.
            // e.g. cItem.getClassRequirement() vs player’s class
        }

        if (!meetsRequirement) {
            // They do NOT meet the requirement => block equip
            event.setCancelled(true);
            player.sendMessage("§cYou do not meet the armor’s requirements.");
            return;
        }

        // They meet the requirement => we handle equipping ourselves
        event.setCancelled(true); // Cancel vanilla so it doesn’t cause weird behavior
        // 2) Fire a custom ArmorEquipEvent
        ArmorEquipEvent equipEvent = new ArmorEquipEvent(
            player,
            EquipMethod.HOTBAR, // custom name
            type,
            currentArmor, // old
            inHand         // new
        );
        Bukkit.getPluginManager().callEvent(equipEvent);

        // If still not cancelled, physically equip
        if (!equipEvent.isCancelled()) {
            // 1) Get the old item (the one currently equipped)
            ItemStack oldArmor = getArmorSlotItem(player, type);

            // 2) Equip the new item from hand
            setArmorSlotItem(player, type, inHand);

            // 3) Put the old item into the player's hand
            player.getInventory().setItemInMainHand(oldArmor);

            // Force an inventory update
            player.updateInventory();
        }
    }

        // Helper to retrieve the item in the relevant armor slot
    private ItemStack getArmorSlotItem(Player player, ArmorType type) {
        switch (type) {
            case HELMET:     return player.getInventory().getHelmet();
            case CHESTPLATE: return player.getInventory().getChestplate();
            case LEGGINGS:   return player.getInventory().getLeggings();
            case BOOTS:      return player.getInventory().getBoots();
        }
        return null;
    }

    // Helper to set the relevant armor slot
    private void setArmorSlotItem(Player player, ArmorType type, ItemStack item) {
        switch (type) {
            case HELMET:
                player.getInventory().setHelmet(item);
                break;
            case CHESTPLATE:
                player.getInventory().setChestplate(item);
                break;
            case LEGGINGS:
                player.getInventory().setLeggings(item);
                break;
            case BOOTS:
                player.getInventory().setBoots(item);
                break;
        }
    }


    // ----------------------------------------------------------------
    // 2) SHIFT-CLICK, INVENTORY CLICK, DRAG, ETC.
    //    (Mostly unchanged, but let's ensure we only do logic if
    //     the item truly is armor.)
    // ----------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        // Only proceed if the clicker is a Player
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        // 1) Check if the top inventory is the Merchant GUI by comparing its title
        InventoryType topInventoryType = event.getView().getTopInventory().getType();
        if (topInventoryType != InventoryType.PLAYER && topInventoryType != InventoryType.CRAFTING) {
            // If the top inventory is not the player's inventory, skip the logic
            return;
        }

        // Ensure the clicked inventory is the player's inventory
        if (event.getClickedInventory() == null ||
            event.getClickedInventory().getType() != InventoryType.PLAYER) {
            // Not interacting with the player's inventory
            return;
        }

        // If we get here, it's not the Merchant GUI. Proceed with armor logic as before.

        if (event.getAction() == InventoryAction.NOTHING) return;
        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().getType().equals(InventoryType.PLAYER)
            && !event.getClickedInventory().getType().equals(InventoryType.CRAFTING)) return;

        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem  = event.getCursor();

        boolean shiftClick = (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT);
        boolean numberKey  = (event.getClick() == ClickType.NUMBER_KEY);

        // SHIFT-CLICK scenario
        // SHIFT-CLICK scenario
        if (shiftClick) {
            // 1) Make sure this is actually armor
            ArmorType newArmorType = ArmorType.matchType(currentItem);
            if (newArmorType == null) return;  // Not armor => skip

            // The slot index for that armor in the player's inventory
            int armorSlotIndex = getArmorSlotIndex(newArmorType);
            int rawSlot = event.getRawSlot();

            // Are we SHIFT-clicking the item FROM the armor slot (unequipping)
            // or FROM the inventory (equipping)?
            boolean equipping = (rawSlot != armorSlotIndex);

            if (equipping) {
                // If we're trying to equip via SHIFT-click, only proceed if the relevant slot is empty.
                ItemStack inThatArmorSlot = event.getView().getItem(armorSlotIndex);
                if (inThatArmorSlot != null && inThatArmorSlot.getType() != Material.AIR) {
                    // Slot is occupied => SHIFT-click won't equip; skip firing an event.
                    return;
                }
            }

            // If we get here, the slot is empty (so SHIFT-click will equip), or
            // we are SHIFT-clicking from the armor slot (thus unequipping).
            ItemStack oldItem = equipping ? null : currentItem;  // If equipping, old slot is empty
            ItemStack newItem = equipping ? currentItem : null;  // If unequipping, new slot is empty

            // Fire your event
            ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent(
                player,
                EquipMethod.SHIFT_CLICK,
                newArmorType,
                oldItem,
                newItem
            );
            Bukkit.getPluginManager().callEvent(armorEquipEvent);

            // If something else cancels it, respect that
            if (armorEquipEvent.isCancelled()) {
                event.setCancelled(true);
            }
        }



        // DRAG / PICKUP / PLACE scenario
        if (!shiftClick && !numberKey) {
            ArmorType newArmorType = ArmorType.matchType(cursorItem);
            ArmorType oldArmorType = ArmorType.matchType(currentItem);

            if (newArmorType != null && event.getRawSlot() == getArmorSlotIndex(newArmorType)) {
                // Equipping
                ArmorEquipEvent equipEvent = new ArmorEquipEvent(
                    player,
                    EquipMethod.PICK_DROP,
                    newArmorType,
                    currentItem,
                    cursorItem
                );
                Bukkit.getPluginManager().callEvent(equipEvent);
                if (equipEvent.isCancelled()) {
                    event.setCancelled(true);
                }
            }
            else if (oldArmorType != null && event.getRawSlot() == getArmorSlotIndex(oldArmorType)
                && (cursorItem == null || cursorItem.getType() == Material.AIR)) {
                // Unequipping
                ArmorEquipEvent unequipEvent = new ArmorEquipEvent(
                    player,
                    EquipMethod.PICK_DROP,
                    oldArmorType,
                    currentItem,
                    null
                );
                Bukkit.getPluginManager().callEvent(unequipEvent);
                if (unequipEvent.isCancelled()) {
                    event.setCancelled(true);
                }
            }
        }

        // NUMBER-KEY scenario
        if (numberKey) {
            if (event.getSlotType() != SlotType.ARMOR) {
                return; // not an armor slot => ignore
            }
            ItemStack hotbarItem = event.getClickedInventory().getItem(event.getHotbarButton());
            ArmorType hotbarType = ArmorType.matchType(hotbarItem);
            ArmorType slotType   = ArmorType.matchType(currentItem);

            if (hotbarType != null) {
                // Equipping from hotbar
                ArmorEquipEvent equipEvent = new ArmorEquipEvent(
                    player,
                    EquipMethod.HOTBAR_SWAP,
                    hotbarType,
                    currentItem,
                    hotbarItem
                );
                Bukkit.getPluginManager().callEvent(equipEvent);
                if (equipEvent.isCancelled()) {
                    event.setCancelled(true);
                }
            }
            else if (slotType != null) {
                // Unequipping
                ArmorEquipEvent unequipEvent = new ArmorEquipEvent(
                    player,
                    EquipMethod.HOTBAR_SWAP,
                    slotType,
                    currentItem,
                    hotbarItem
                );
                Bukkit.getPluginManager().callEvent(unequipEvent);
                if (unequipEvent.isCancelled()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private int getArmorSlotIndex(ArmorType type) {
        switch (type) {
            case HELMET:     return 5;
            case CHESTPLATE: return 6;
            case LEGGINGS:   return 7;
            case BOOTS:      return 8;
        }
        return -1;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack draggedItem = event.getOldCursor();
        ArmorType type = ArmorType.matchType(draggedItem);
        if (type == null) return; // not armor => skip

        if (event.getRawSlots().contains(getArmorSlotIndex(type))) {
            ArmorEquipEvent dragEquipEvent = new ArmorEquipEvent(
                player,
                EquipMethod.DRAG,
                type,
                null,
                draggedItem
            );
            Bukkit.getPluginManager().callEvent(dragEquipEvent);
            if (dragEquipEvent.isCancelled()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onItemBreak(PlayerItemBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack brokenItem = event.getBrokenItem();
        ArmorType type = ArmorType.matchType(brokenItem);
        if (type != null) {
            ArmorEquipEvent breakEvent = new ArmorEquipEvent(
                player,
                EquipMethod.BROKE,
                type,
                brokenItem,
                null
            );
            Bukkit.getPluginManager().callEvent(breakEvent);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (event.getKeepInventory()) return;

        for (ItemStack armor : player.getInventory().getArmorContents()) {
            ArmorType type = ArmorType.matchType(armor);
            if (type != null && armor != null && armor.getType() != Material.AIR) {
                ArmorEquipEvent deathUnequipEvent = new ArmorEquipEvent(
                    player,
                    EquipMethod.DEATH,
                    type,
                    armor,
                    null
                );
                Bukkit.getPluginManager().callEvent(deathUnequipEvent);
            }
        }
    }
}
