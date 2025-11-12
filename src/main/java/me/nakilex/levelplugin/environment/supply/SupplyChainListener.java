package me.nakilex.levelplugin.environment.supply;

import me.nakilex.levelplugin.environment.supply.gui.SupplyChainBoard;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** Handles interactions with the supply chain GUI. */
public final class SupplyChainListener implements Listener {

    private final SupplyChainManager manager;

    public SupplyChainListener(SupplyChainManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!SupplyChainBoard.TITLE.equals(event.getView().getTitle())) {
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
                .get(SupplyChainBoard.KEY_ACTION, PersistentDataType.STRING);
        if (SupplyChainBoard.ACTION_DEPOSIT.equals(action)) {
            player.closeInventory();
            manager.handleDeposit(player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (SupplyChainBoard.TITLE.equals(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }
}

