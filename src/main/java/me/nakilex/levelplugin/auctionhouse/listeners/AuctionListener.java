package me.nakilex.levelplugin.auctionhouse.listeners;

import me.nakilex.levelplugin.auctionhouse.AuctionHouseManager;
import me.nakilex.levelplugin.auctionhouse.gui.AuctionGUI;
import me.nakilex.levelplugin.auctionhouse.gui.CollectionBinGUI;
import me.nakilex.levelplugin.auctionhouse.gui.ListingMenu;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;

public class AuctionListener implements Listener {
    private final AuctionHouseManager manager;
    private final ListingMenu listingMenu;
    private final CollectionBinGUI binGUI;

    public AuctionListener(AuctionHouseManager manager) {
        this.manager = manager;
        this.listingMenu = new ListingMenu(manager);
        this.binGUI = new CollectionBinGUI(manager);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        Player player = (Player) event.getWhoClicked();
        if (view.getTitle().equals(AuctionGUI.TITLE)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 45) {
                if (event.getCurrentItem() != null) {
                    int id = slot + 1 + (0 * 45); // page 0 only
                    manager.buy(player, id);
                    player.closeInventory();
                }
            } else if (slot == 51) {
                listingMenu.open(player);
            } else if (slot == 50) {
                binGUI.open(player);
            }
        } else if (view.getTitle().equals(ListingMenu.TITLE)) {
            event.setCancelled(true);
            listingMenu.handleClick(player, event.getRawSlot(), event.isShiftClick(), event.isRightClick());
        } else if (view.getTitle().equals(CollectionBinGUI.TITLE)) {
            if (event.getRawSlot() == 53) {
                event.setCancelled(true);
                player.closeInventory();
            }
        }
    }
}
