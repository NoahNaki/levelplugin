package me.nakilex.levelplugin.player.classes.essence.listener;

import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.player.classes.essence.gui.ClassEssenceProgressGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

/** Handles sacrifice interactions in the progress GUI. */
public class ClassEssenceProgressListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!ClassEssenceProgressGUI.TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        if (event.getRawSlot() == ClassEssenceProgressGUI.getSacrificeSlot()) {
            ItemStack cursor = event.getCursor();
            ItemStack target = ClassEssenceProgressGUI.getTarget(player);
            if (cursor != null && target != null && ClassEssence.isEssence(cursor) &&
                    ClassEssence.getClass(cursor) == ClassEssence.getClass(target)) {
                if (ClassEssence.getRarity(cursor) == ClassEssence.getRarity(target)) {
                    ClassEssence.upgradeStar(target);
                } else {
                    ClassEssence.addExp(target, 50);
                }
                event.getWhoClicked().setItemOnCursor(null);
                ClassEssence.updateLore(target);
                ClassEssenceProgressGUI.open(player, target);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!ClassEssenceProgressGUI.TITLE.equals(event.getView().getTitle())) return;
        ClassEssenceProgressGUI.clear((Player) event.getPlayer());
    }
}

