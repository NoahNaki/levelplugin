package me.nakilex.levelplugin.player.classes.essence.listener;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.player.classes.essence.gui.ClassEssenceGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

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
        event.setCancelled(true);

        int idx = ClassEssenceGUI.indexFromSlot(event.getRawSlot());
        if (idx == -1) return;

        Player player = (Player) event.getWhoClicked();
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        ClickType click = event.getClick();

        if (click.isLeftClick()) {
            if (current == null && cursor != null && ClassEssence.isEssence(cursor)) {
                ItemStack placed = cursor.clone();
                ps.essenceSlots[idx] = placed;
                event.getView().setItem(event.getRawSlot(), placed);
                event.getWhoClicked().setItemOnCursor(null);
            } else if (current != null && ClassEssence.isEssence(current) && !ps.equippedEssences[idx]) {
                ClassEssence.setEquipped(current, true);
                ClassEssence.applyAttributes(player, current);
                ps.equippedEssences[idx] = true;
            }
        } else if (click.isRightClick()) {
            if (current != null && ClassEssence.isEssence(current)) {
                if (ps.equippedEssences[idx]) {
                    ClassEssence.removeAttributes(player, current);
                    ps.equippedEssences[idx] = false;
                    ClassEssence.setEquipped(current, false);
                }
                player.getInventory().addItem(current);
                ps.essenceSlots[idx] = null;
                event.getView().setItem(event.getRawSlot(), null);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!ClassEssenceGUI.TITLE.equals(event.getView().getTitle())) return;
        Player player = (Player) event.getPlayer();
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        for (int i = 0; i < 3; i++) {
            ItemStack item = event.getInventory().getItem(ClassEssenceGUI.slotFromIndex(i));
            ps.essenceSlots[i] = item;
            ps.equippedEssences[i] = item != null && ClassEssence.isEssence(item) && ClassEssence.isEquipped(item);
        }
    }
}
