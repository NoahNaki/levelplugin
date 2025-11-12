package me.nakilex.levelplugin.guild.expedition;

import me.nakilex.levelplugin.guild.expedition.gui.ExpeditionRelicBoard;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** Handles interactions with the expedition relic board GUI. */
public final class ExpeditionRelicListener implements Listener {

    private final ExpeditionRelicManager manager;

    public ExpeditionRelicListener(ExpeditionRelicManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!ExpeditionRelicBoard.TITLE.equals(event.getView().getTitle())) {
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
                .get(ExpeditionRelicBoard.KEY_ACTION, PersistentDataType.STRING);
        if (action == null) {
            return;
        }
        player.closeInventory();
        switch (action) {
            case ExpeditionRelicBoard.ACTION_INVEST -> manager.invest(player);
            case ExpeditionRelicBoard.ACTION_START -> manager.startExpedition(player);
            case ExpeditionRelicBoard.ACTION_MAINTAIN -> manager.depositMaintenance(player);
            default -> { }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (ExpeditionRelicBoard.TITLE.equals(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }
}
