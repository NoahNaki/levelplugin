package me.nakilex.levelplugin.auction.gui;

import me.nakilex.levelplugin.auction.AuctionItem;
import me.nakilex.levelplugin.auction.managers.AuctionManager;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple GUI to browse and purchase auctions.
 */
public class AuctionHouseGUI implements Listener {
    private static final String TITLE = ChatColor.DARK_AQUA + "Auction House";

    private final Plugin plugin;
    private final AuctionManager manager;
    private final EconomyManager economy;

    private Inventory inventory;

    public AuctionHouseGUI(Plugin plugin, AuctionManager manager, EconomyManager economy) {
        this.plugin = plugin;
        this.manager = manager;
        this.economy = economy;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        buildInventory();
    }

    private void buildInventory() {
        this.inventory = Bukkit.createInventory(null, 54, TITLE);
        refresh();
    }

    /** Refresh inventory items */
    public void refresh() {
        if (inventory == null) return;
        inventory.clear();
        int slot = 0;
        for (AuctionItem item : manager.getAuctions()) {
            if (slot >= inventory.getSize()) break;
            ItemStack stack = item.getItem().clone();
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("" + ChatColor.YELLOW + item.getPrice() + " coins");
                lore.add(ChatColor.GRAY + "Seller: " + item.getOwnerName());
                meta.setLore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                stack.setItemMeta(meta);
            }
            inventory.setItem(slot++, stack);
        }
    }

    public void open(Player player) {
        refresh();
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;
        int slot = event.getRawSlot();
        List<AuctionItem> list = new ArrayList<>(manager.getAuctions());
        if (slot < 0 || slot >= list.size()) return;
        AuctionItem auction = list.get(slot);
        Player player = (Player) event.getWhoClicked();
        if (auction.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You cannot buy your own item.");
            return;
        }
        int price = auction.getPrice();
        if (economy.getBalance(player) < price) {
            player.sendMessage(ChatColor.RED + "You can't afford this item.");
            return;
        }
        try {
            economy.deductCoins(player, price);
        } catch (IllegalArgumentException ex) {
            player.sendMessage(ChatColor.RED + "Transaction failed: " + ex.getMessage());
            return;
        }
        player.getInventory().addItem(auction.getItem());
        manager.removeAuction(auction.getId());
        player.sendMessage(ChatColor.GREEN + "Purchased item for " + price + " coins.");
        Player owner = Bukkit.getPlayer(auction.getOwner());
        if (owner != null && owner.isOnline()) {
            economy.addCoins(owner, price);
            owner.sendMessage(ChatColor.GREEN + player.getName() + " bought your item for " + price + " coins.");
        } else {
            // offline: add directly to their balance via economy
            economy.addCoins(Bukkit.getOfflinePlayer(auction.getOwner()).getPlayer(), price); // might be null
        }
        refresh();
    }
}
