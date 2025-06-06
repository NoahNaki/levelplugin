package me.nakilex.levelplugin.auctionhouse;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Handles loading, saving and manipulation of auction listings.
 */
public class AuctionHouseManager {

    private final Plugin plugin;
    private final EconomyManager economyManager;
    private final File file;
    private final FileConfiguration config;
    private final List<AuctionItem> auctions = new ArrayList<>();

    public AuctionHouseManager(Plugin plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.file = new File(plugin.getDataFolder(), "auctions.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        loadAuctions();

        // Periodically expire auctions
        new BukkitRunnable() {
            @Override
            public void run() {
                checkExpired();
            }
        }.runTaskTimer(plugin, 20L, 1200L); // every minute
    }

    public synchronized List<AuctionItem> getAuctions() {
        return auctions;
    }

    public synchronized void listItem(Player seller, ItemStack item, int startPrice, int binPrice, long durationHours) {
        auctions.add(new AuctionItem(seller.getUniqueId(), item.clone(), startPrice, binPrice, durationHours));
        saveAuctions();
    }

    public synchronized boolean bid(Player bidder, int index, int amount) {
        if (index < 0 || index >= auctions.size()) return false;
        AuctionItem ai = auctions.get(index);
        if (ai.getStatus() != AuctionStatus.ACTIVE) return false;
        int minBid = Math.max(ai.getStartingPrice(), ai.getCurrentBid() + 1);
        if (amount < minBid) {
            bidder.sendMessage("Bid must be at least " + minBid + " coins.");
            return false;
        }
        if (economyManager.getBalance(bidder) < amount) {
            bidder.sendMessage("Not enough coins!");
            return false;
        }
        // refund previous bidder
        if (ai.getHighestBidder() != null) {
            economyManager.addCoins(ai.getHighestBidder(), ai.getCurrentBid());
            Player prev = Bukkit.getPlayer(ai.getHighestBidder());
            if (prev != null) {
                prev.sendMessage(bidder.getName() + " outbid you on an auction.");
            }
        }
        economyManager.deductCoins(bidder, amount);
        ai.setCurrentBid(amount);
        ai.setHighestBidder(bidder.getUniqueId());
        saveAuctions();
        return true;
    }

    public synchronized boolean buyNow(Player buyer, int index) {
        if (index < 0 || index >= auctions.size()) return false;
        AuctionItem ai = auctions.get(index);
        if (ai.getStatus() != AuctionStatus.ACTIVE) return false;
        int price = ai.getBinPrice();
        if (price <= 0) return false;
        if (economyManager.getBalance(buyer) < price) {
            buyer.sendMessage("Not enough coins!");
            return false;
        }
        economyManager.deductCoins(buyer, price);
        economyManager.addCoins(ai.getSeller(), price);
        buyer.getInventory().addItem(ai.getItem());
        ai.setStatus(AuctionStatus.SOLD);
        auctions.remove(index);
        saveAuctions();
        Player seller = Bukkit.getPlayer(ai.getSeller());
        if (seller != null) {
            seller.sendMessage(buyer.getName() + " bought your item for " + price + " coins.");
        }
        return true;
    }

    /**
     * Cancel an active listing owned by the given player.
     * The item is returned to the seller's inventory.
     */
    public synchronized boolean cancelListing(Player seller, int index) {
        if (index < 0 || index >= auctions.size()) return false;
        AuctionItem ai = auctions.get(index);
        if (!ai.getSeller().equals(seller.getUniqueId())) return false;
        if (ai.getStatus() != AuctionStatus.ACTIVE) return false;
        seller.getInventory().addItem(ai.getItem());
        auctions.remove(index);
        saveAuctions();
        return true;
    }

    private synchronized void checkExpired() {
        long now = System.currentTimeMillis();
        Iterator<AuctionItem> it = auctions.iterator();
        while (it.hasNext()) {
            AuctionItem ai = it.next();
            if (ai.getStatus() != AuctionStatus.ACTIVE) continue;
            if (now >= ai.getEndTime()) {
                if (ai.getHighestBidder() != null) {
                    // give item to highest bidder and coins to seller
                    economyManager.addCoins(ai.getSeller(), ai.getCurrentBid());
                    Player buyer = Bukkit.getPlayer(ai.getHighestBidder());
                    if (buyer != null) {
                        buyer.getInventory().addItem(ai.getItem());
                        buyer.sendMessage("You won an auction!");
                    }
                } else {
                    // return item to seller
                    Player seller = Bukkit.getPlayer(ai.getSeller());
                    if (seller != null) {
                        seller.getInventory().addItem(ai.getItem());
                        seller.sendMessage("Your auction expired without bids.");
                    }
                }
                ai.setStatus(AuctionStatus.EXPIRED);
                it.remove();
            }
        }
        if (!auctions.isEmpty()) saveAuctions();
    }

    private synchronized void loadAuctions() {
        auctions.clear();
        if (!config.contains("auctions")) return;
        for (String key : config.getConfigurationSection("auctions").getKeys(false)) {
            String base = "auctions." + key + ".";
            UUID seller = UUID.fromString(config.getString(base + "seller"));
            int start = config.getInt(base + "start");
            int bin = config.getInt(base + "bin");
            int currentBid = config.getInt(base + "currentBid");
            String bidderStr = config.getString(base + "bidder");
            long startTime = config.getLong(base + "startTime");
            long endTime = config.getLong(base + "endTime");
            String statusStr = config.getString(base + "status");
            AuctionStatus status = statusStr != null ? AuctionStatus.valueOf(statusStr) : AuctionStatus.ACTIVE;
            ItemStack item = config.getItemStack(base + "item");
            AuctionItem ai = new AuctionItem(seller, item, start, bin, 1); // duration ignored
            ai.setCurrentBid(currentBid);
            if (bidderStr != null) ai.setHighestBidder(UUID.fromString(bidderStr));
            // overwrite times and status
            try {
                java.lang.reflect.Field fStart = AuctionItem.class.getDeclaredField("startTime");
                fStart.setAccessible(true);
                fStart.set(ai, startTime);
                java.lang.reflect.Field fEnd = AuctionItem.class.getDeclaredField("endTime");
                fEnd.setAccessible(true);
                fEnd.set(ai, endTime);
                java.lang.reflect.Field fStatus = AuctionItem.class.getDeclaredField("status");
                fStatus.setAccessible(true);
                fStatus.set(ai, status);
            } catch (Exception ignored) {}
            auctions.add(ai);
        }
    }

    public synchronized void saveAuctions() {
        config.set("auctions", null);
        for (int i = 0; i < auctions.size(); i++) {
            AuctionItem ai = auctions.get(i);
            String base = "auctions." + i + ".";
            config.set(base + "seller", ai.getSeller().toString());
            config.set(base + "start", ai.getStartingPrice());
            config.set(base + "bin", ai.getBinPrice());
            config.set(base + "currentBid", ai.getCurrentBid());
            config.set(base + "bidder", ai.getHighestBidder() == null ? null : ai.getHighestBidder().toString());
            config.set(base + "startTime", ai.getStartTime());
            config.set(base + "endTime", ai.getEndTime());
            config.set(base + "status", ai.getStatus().name());
            config.set(base + "item", ai.getItem());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
