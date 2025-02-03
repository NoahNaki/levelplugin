package me.nakilex.levelplugin.storage.data;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Provides basic YAML-based save/load capabilities for player storage.
 * This version removes the references to StorageGUIPlaceholder.
 */
public class FileHandler {

    /**
     * Saves a list of Inventory pages for a given player.
     */
    public void saveStorage(UUID playerId, List<Inventory> pages) {
        File file = getStorageFile(playerId);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Clear out old data
        config.set("pages", null);

        for (int i = 0; i < pages.size(); i++) {
            Inventory page = pages.get(i);
            List<ItemStack> items = new ArrayList<>();
            for (int slot = 0; slot < page.getSize(); slot++) {
                items.add(page.getItem(slot));
            }
            config.set("pages." + i, items);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads a list of Inventory pages for a given player.
     */
    public List<Inventory> loadStorage(UUID playerId) {
        File file = getStorageFile(playerId);
        if (!file.exists()) {
            // No data, return empty
            return new ArrayList<>();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Inventory> loadedPages = new ArrayList<>();

        if (config.contains("pages")) {
            for (String key : config.getConfigurationSection("pages").getKeys(false)) {
                String pageKey = "pages." + key;
                List<ItemStack> items = (List<ItemStack>) config.get(pageKey);

                int pageIndex = Integer.parseInt(key) + 1;
                Inventory page = Bukkit.createInventory(
                    null,
                    54,
                    ChatColor.DARK_GREEN + "Personal Storage (Page " + pageIndex + ")"
                );

                for (int slot = 0; slot < items.size(); slot++) {
                    page.setItem(slot, items.get(slot));
                }

                // Re-add navigation arrows after loading (inline version).
                if (pageIndex > 1) {
                    page.setItem(45, createArrow(ChatColor.YELLOW + "Previous Page"));
                }
                page.setItem(53, createArrow(ChatColor.YELLOW + "Next Page"));

                loadedPages.add(page);
            }
        }
        return loadedPages;
    }

    /**
     * Creates an ItemStack arrow with a custom display name.
     */
    private ItemStack createArrow(String displayName) {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            arrow.setItemMeta(meta);
        }
        return arrow;
    }

    /**
     * Locates or creates the file storing player data in a folder named 'storage'.
     */
    private File getStorageFile(UUID playerId) {
        File folder = new File(Bukkit.getPluginManager()
            .getPlugin("LevelPlugin").getDataFolder(), "storage");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return new File(folder, "player_" + playerId + ".yml");
    }
}
