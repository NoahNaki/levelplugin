package me.nakilex.levelplugin.listeners;

import me.nakilex.levelplugin.events.ArmorEquipEvent;
import me.nakilex.levelplugin.events.ArmorEquipEvent.EquipMethod;
import me.nakilex.levelplugin.events.ArmorType;
import me.nakilex.levelplugin.managers.LevelManager;
import me.nakilex.levelplugin.items.CustomItem;
import me.nakilex.levelplugin.managers.ItemManager;
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
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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
            int playerLevel   = LevelManager.getInstance().getLevel(player);
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
            // Put the item in the appropriate armor slot
            setArmorSlotItem(player, type, inHand);

            // Now remove that item from hand
            player.getInventory().setItemInMainHand(null);
            // or reduce the stack by 1 if you prefer to replicate older MC mechanics
            // but typically it places the entire item in that slot
        } else {
            // If the event got cancelled by something else, do nothing
        }
        // Force an inventory update
        player.updateInventory();
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
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (event.getAction() == InventoryAction.NOTHING) return;
        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().getType().equals(InventoryType.PLAYER)
            && !event.getClickedInventory().getType().equals(InventoryType.CRAFTING)) return;

        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem  = event.getCursor();

        boolean shiftClick = (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT);
        boolean numberKey  = (event.getClick() == ClickType.NUMBER_KEY);

        // SHIFT-CLICK scenario
        if (shiftClick) {
            ArmorType newArmorType = ArmorType.matchType(currentItem);
            if (newArmorType == null) return;  // not armor => skip

            int rawSlot = event.getRawSlot();
            boolean equipping = (rawSlot != getArmorSlotIndex(newArmorType));
            ItemStack oldItem = equipping ? null : currentItem;
            ItemStack newItem = equipping ? currentItem : null;

            ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent(
                player,
                EquipMethod.SHIFT_CLICK,
                newArmorType,
                oldItem,
                newItem
            );
            Bukkit.getPluginManager().callEvent(armorEquipEvent);
            if (armorEquipEvent.isCancelled()) {
                event.setCancelled(true);
            }
            return;
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
