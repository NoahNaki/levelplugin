package me.nakilex.levelplugin.dungeon.trial;

import me.nakilex.levelplugin.dungeon.trial.gui.ArcaneTrialBoard;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** Handles GUI interactions for arcane trials. */
public final class ArcaneTrialListener implements Listener {

    private final ArcaneTrialManager manager;

    public ArcaneTrialListener(ArcaneTrialManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!ArcaneTrialBoard.TITLE.equals(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        String action = item.getItemMeta().getPersistentDataContainer()
                .get(ArcaneTrialBoard.KEY_ACTION, PersistentDataType.STRING);
        if (action == null) {
            return;
        }
        player.closeInventory();
        if (action.startsWith(ArcaneTrialBoard.ACTION_START)) {
            String[] parts = action.split(":");
            int tier = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
            manager.startTrial(player, tier);
        } else if (ArcaneTrialBoard.ACTION_PRESTIGE.equals(action)) {
            manager.prestige(player);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (ArcaneTrialBoard.TITLE.equals(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }
}

