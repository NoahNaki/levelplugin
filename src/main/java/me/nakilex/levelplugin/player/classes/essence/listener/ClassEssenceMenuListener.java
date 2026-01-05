package me.nakilex.levelplugin.player.classes.essence.listener;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.player.classes.essence.gui.ClassEssenceGUI;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ClassEssenceMenuListener implements Listener {

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (ClassEssenceGUI.TITLE.equals(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!ClassEssenceGUI.TITLE.equals(event.getView().getTitle())) return;

        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlot() >= topSize) {
            // allow normal interaction in player inventory
            return;
        }
        event.setCancelled(true);

        int idx = ClassEssenceGUI.indexFromSlot(event.getRawSlot());
        if (idx == -1) return;

        Player player = (Player) event.getWhoClicked();
        StatsManager statsManager = StatsManager.getInstance();
        StatsManager.PlayerStats ps = statsManager.getPlayerStats(player.getUniqueId());

        if (!statsManager.isEssenceSlotUnlocked(player, idx)) {
            if (idx == 1) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                        "Complete " + ChatColor.YELLOW + "Essence Weaver's Lesson"
                                + ChatColor.RED + " to unlock this Essence Slot.");
            } else {
                int required = statsManager.getEssenceSlotUnlockLevel(idx);
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                        "Reach level " + ChatColor.YELLOW + required + ChatColor.RED + " to unlock this Essence Slot.");
            }
            return;
        }

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        ClickType click = event.getClick();

        if (click.isLeftClick()) {
            if (current == null && cursor != null && ClassEssence.isEssence(cursor)) {
                if (cursor.getAmount() > 1) {
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                            "Only one essence can be placed in a slot. Split the stack first.");
                    return;
                }
                ItemStack placed = cursor.clone();
                placed.setAmount(1);
                ClassEssence.addSlotTips(placed);
                ps.essenceSlots[idx] = placed;
                event.getView().setItem(event.getRawSlot(), placed);
                event.getWhoClicked().setItemOnCursor(null);
            } else if (current != null && ClassEssence.isEssence(current) && !ps.equippedEssences[idx]) {
                ps.essenceSlots[idx] = current;
                ClassEssenceEquipHelper.equip(player, ps, idx, current, event.getView().getTopInventory());
            }
        } else if (click.isRightClick()) {
            if (current != null && ClassEssence.isEssence(current)) {
                if (ps.equippedEssences[idx]) {
                    ClassEssenceEquipHelper.unequip(player, ps, idx, current);
                }
                ClassEssence.updateLore(current);
                ItemStack returned = current.clone();
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(returned);
                overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                ps.essenceSlots[idx] = null;
                event.getView().setItem(event.getRawSlot(), null);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!ClassEssenceGUI.TITLE.equals(event.getView().getTitle())) return;
        Player player = (Player) event.getPlayer();
        StatsManager statsManager = StatsManager.getInstance();
        StatsManager.PlayerStats ps = statsManager.getPlayerStats(player.getUniqueId());
        int unlockedSlots = statsManager.getUnlockedEssenceSlots(player);
        for (int i = 0; i < ps.essenceSlots.length; i++) {
            if (i >= unlockedSlots) {
                ps.essenceSlots[i] = null;
                ps.equippedEssences[i] = false;
                continue;
            }
            ItemStack item = event.getInventory().getItem(ClassEssenceGUI.slotFromIndex(i));
            if (item != null && ClassEssence.isEssence(item)) {
                ClassEssence.updateLore(item);
            }
            ps.essenceSlots[i] = item;
            ps.equippedEssences[i] = item != null && ClassEssence.isEssence(item) && ClassEssence.isEquipped(item);
        }
    }
}
