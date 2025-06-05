package me.nakilex.levelplugin.auctionhouse;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.auctionhouse.data.AuctionStorageProvider;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Core manager handling auction listings and player bins.
 */
public class AuctionHouseManager {
    private final JavaPlugin plugin;
    private final EconomyManager economy;
    private final AuctionStorageProvider storage;
    private final Map<Integer, AuctionListing> listings = new HashMap<>();
    private final Map<UUID, List<ItemStack>> collectionBins = new HashMap<>();
    private final long listingDuration;
    private final int maxListings;

    public AuctionHouseManager(Main plugin, EconomyManager economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.storage = new AuctionStorageProvider(plugin.getDataFolder());
        this.listingDuration = plugin.getConfig().getLong("auction.duration-hours", 48) * 3600_000L;
        this.maxListings = plugin.getConfig().getInt("auction.max-listings", 5);
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public void init() {
        storage.init();
        for (AuctionListing l : storage.loadActiveListings()) {
            listings.put(l.getId(), l);
        }
    }

    public void shutdown() {
        storage.close();
    }

    public Collection<AuctionListing> getListings() {
        checkExpired();
        return listings.values();
    }

    public List<AuctionListing> getPlayerListings(UUID id) {
        List<AuctionListing> list = new ArrayList<>();
        for (AuctionListing l : listings.values()) {
            if (l.getSeller().equals(id)) list.add(l);
        }
        return list;
    }

    public boolean listItem(Player seller, ItemStack item, double price) {
        if (ItemManager.getInstance().getCustomItemFromItemStack(item) == null) {
            seller.sendMessage(ChatColor.RED + "Only custom items can be listed.");
            return false;
        }
        if (item.getEnchantments().keySet().stream().anyMatch(e -> e.getKey().getKey().contains("curse"))) {
            seller.sendMessage(ChatColor.RED + "Cannot list cursed items.");
            return false;
        }
        List<AuctionListing> own = getPlayerListings(seller.getUniqueId());
        if (own.size() >= maxListings) {
            seller.sendMessage(ChatColor.RED + "You reached max listings.");
            return false;
        }
        long expire = System.currentTimeMillis() + listingDuration;
        int id = storage.insertListing(seller.getUniqueId(), item, price, expire);
        if (id == -1) return false;
        listings.put(id, new AuctionListing(id, seller.getUniqueId(), item, price, expire, true));
        seller.getInventory().removeItem(item);
        return true;
    }

    public boolean buy(Player buyer, int listingId) {
        AuctionListing listing = listings.get(listingId);
        if (listing == null || !listing.isActive()) return false;
        if (listing.getSeller().equals(buyer.getUniqueId())) {
            buyer.sendMessage(ChatColor.RED + "You cannot buy your own listing.");
            return false;
        }
        int balance = economy.getBalance(buyer);
        if (balance < listing.getPrice()) {
            buyer.sendMessage(ChatColor.RED + "Not enough coins.");
            return false;
        }
        economy.deductCoins(buyer, (int) listing.getPrice());
        Player sellerPlayer = Bukkit.getPlayer(listing.getSeller());
        if (sellerPlayer != null) {
            economy.addCoins(sellerPlayer, (int) listing.getPrice());
            sellerPlayer.sendMessage(ChatColor.GREEN + buyer.getName() + " bought your item for " + listing.getPrice());
        }
        buyer.getInventory().addItem(listing.getItem());
        listing.setActive(false);
        storage.markInactive(listingId);
        listings.remove(listingId);
        return true;
    }

    public boolean cancel(Player player, int listingId) {
        AuctionListing listing = listings.get(listingId);
        if (listing == null || !listing.getSeller().equals(player.getUniqueId())) return false;
        listing.setActive(false);
        storage.markInactive(listingId);
        listings.remove(listingId);
        addToBin(player.getUniqueId(), listing.getItem());
        return true;
    }

    private void addToBin(UUID player, ItemStack item) {
        collectionBins.computeIfAbsent(player, k -> new ArrayList<>()).add(item);
    }

    public List<ItemStack> getBin(UUID player) {
        return collectionBins.getOrDefault(player, new ArrayList<>());
    }

    public void checkExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, AuctionListing>> it = listings.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, AuctionListing> e = it.next();
            AuctionListing l = e.getValue();
            if (now > l.getExpireAt()) {
                addToBin(l.getSeller(), l.getItem());
                storage.markInactive(l.getId());
                it.remove();
            }
        }
    }
}
