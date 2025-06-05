package me.nakilex.levelplugin.auction.managers;

import me.nakilex.levelplugin.auction.AuctionItem;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Handles loading and saving auction listings.
 */
public class AuctionManager {
    private final Plugin plugin;
    private final File file;
    private final FileConfiguration config;
    private final Map<UUID, AuctionItem> auctions = new HashMap<>();

    public AuctionManager(Plugin plugin) {
        this.plugin = plugin;
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

    private void loadAuctions() {
        if (config.contains("auctions")) {
            for (String key : config.getConfigurationSection("auctions").getKeys(false)) {
                String base = "auctions." + key + ".";
                UUID id = UUID.fromString(key);
                UUID owner = UUID.fromString(config.getString(base + "owner"));
                String ownerName = config.getString(base + "ownerName", "Unknown");
                int price = config.getInt(base + "price");
                long timestamp = config.getLong(base + "timestamp");
                ItemStack item = config.getItemStack(base + "item");
                if (item != null) {
                    auctions.put(id, new AuctionItem(id, owner, ownerName, item, price, timestamp));
                }
            }
        }
    }

    public void saveAuctions() {
        config.set("auctions", null);
        for (AuctionItem ai : auctions.values()) {
            String base = "auctions." + ai.getId().toString() + ".";
            config.set(base + "owner", ai.getOwner().toString());
            config.set(base + "ownerName", ai.getOwnerName());
            config.set(base + "price", ai.getPrice());
            config.set(base + "timestamp", ai.getTimestamp());
            config.set(base + "item", ai.getItem());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Collection<AuctionItem> getAuctions() {
        return Collections.unmodifiableCollection(auctions.values());
    }

    public void addAuction(AuctionItem item) {
        auctions.put(item.getId(), item);
        saveAuctions();
    }

    public void removeAuction(UUID id) {
        auctions.remove(id);
        saveAuctions();
    }
}
