package me.nakilex.levelplugin.cursormenu.display;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handles simple spawning of item displays for players.
 */
public class ItemDisplayManager extends AbstractDisplayManager<ItemDisplay> {
    private final Map<String, ItemStack> items = new HashMap<>();
    private final File configFile;

    public ItemDisplayManager(JavaPlugin plugin, File configFile) {
        super(plugin);
        this.configFile = configFile;
        reloadConfig();
    }

    public void reloadConfig() {
        items.clear();
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);
        if (cfg.getConfigurationSection("items") == null) return;
        for (String id : cfg.getConfigurationSection("items").getKeys(false)) {
            ItemStack stack = cfg.getItemStack("items." + id + ".stack");
            if (stack != null) items.put(id, stack);
        }
    }

    public void showItem(Player player, String id, Location loc) {
        ItemStack stack = items.get(id);
        if (stack == null) return;
        hide(player);
        ItemDisplay display = loc.getWorld().spawn(loc, ItemDisplay.class, d -> d.setItemStack(stack));
        playerDisplays.put(player.getUniqueId(), display);
    }

    public void hideItem(Player player) {
        hide(player);
    }

    public Set<String> getAllItemIds() {
        return items.keySet();
    }
}
