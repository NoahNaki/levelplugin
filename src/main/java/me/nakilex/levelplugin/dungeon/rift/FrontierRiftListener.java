package me.nakilex.levelplugin.dungeon.rift;

import me.nakilex.levelplugin.dungeon.rift.gui.FrontierRiftBoard;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** Handles GUI interactions for the frontier rift board. */
public final class FrontierRiftListener implements Listener {

    private final FrontierRiftManager manager;

    public FrontierRiftListener(FrontierRiftManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        if (!FrontierRiftBoard.TITLE.equals(view.getTitle())) {
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
                .get(FrontierRiftBoard.KEY_ACTION, PersistentDataType.STRING);
        if (FrontierRiftBoard.ACTION_START.equals(action)) {
            player.closeInventory();
            manager.startNextStage(player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (FrontierRiftBoard.TITLE.equals(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }
}

