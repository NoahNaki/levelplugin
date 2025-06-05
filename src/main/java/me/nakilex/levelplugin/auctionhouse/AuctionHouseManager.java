package me.nakilex.levelplugin.auctionhouse;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles loading, saving and basic manipulation of auction listings.
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
    }

    public List<AuctionItem> getAuctions() {
        return auctions;
    }

    public void listItem(Player seller, ItemStack item, int price) {
        auctions.add(new AuctionItem(seller.getUniqueId(), item.clone(), price));
        saveAuctions();
    }

    public void purchase(Player buyer, int index) {
        if (index < 0 || index >= auctions.size()) return;
        AuctionItem ai = auctions.get(index);
        if (economyManager.getBalance(buyer) < ai.getPrice()) {
            buyer.sendMessage("Not enough coins!");
            return;
        }
        economyManager.deductCoins(buyer, ai.getPrice());
        economyManager.addCoins(ai.getSeller(), ai.getPrice());
        buyer.getInventory().addItem(ai.getItem());
        auctions.remove(index);
        saveAuctions();
        Player seller = Bukkit.getPlayer(ai.getSeller());
        if (seller != null) {
            seller.sendMessage(buyer.getName() + " bought your item for " + ai.getPrice() + " coins.");
        }
    }

    public void loadAuctions() {
        auctions.clear();
        if (!config.contains("auctions")) return;
        for (String key : config.getConfigurationSection("auctions").getKeys(false)) {
            String base = "auctions." + key + ".";
            UUID seller = UUID.fromString(config.getString(base + "seller"));
            int price = config.getInt(base + "price");
            ItemStack item = config.getItemStack(base + "item");
            auctions.add(new AuctionItem(seller, item, price));
        }
    }

    public void saveAuctions() {
        config.set("auctions", null);
        for (int i = 0; i < auctions.size(); i++) {
            AuctionItem ai = auctions.get(i);
            String base = "auctions." + i + ".";
            config.set(base + "seller", ai.getSeller().toString());
            config.set(base + "price", ai.getPrice());
            config.set(base + "item", ai.getItem());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
