package me.nakilex.levelplugin.cursormenu;

import me.nakilex.levelplugin.cursormenu.command.CursorMenuCommand;
import me.nakilex.levelplugin.cursormenu.display.ItemDisplayManager;
import me.nakilex.levelplugin.cursormenu.display.TextDisplayManager;
import me.nakilex.levelplugin.cursormenu.menu.SectionManager;
import me.nakilex.levelplugin.cursormenu.placeholder.CursorMenuPlaceholder;
import me.nakilex.levelplugin.cursormenu.util.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entry point for the cursor menu system. This implementation focuses on
 * providing a reusable structure rather than concrete visuals.
 */
public class CursorMenuPlugin extends JavaPlugin {
    private final Map<UUID, String> currentMenu = new ConcurrentHashMap<>();
    private SectionManager sectionManager;
    private ItemDisplayManager itemDisplayManager;
    private TextDisplayManager textDisplayManager;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = ConfigUtils.loadConfig(this, "cursormenu/config.yml");

        sectionManager = new SectionManager();
        itemDisplayManager = new ItemDisplayManager(this);
        textDisplayManager = new TextDisplayManager(this);

        Bukkit.getPluginManager().registerEvents(itemDisplayManager, this);
        Bukkit.getPluginManager().registerEvents(textDisplayManager, this);

        CursorMenuCommand command = new CursorMenuCommand(this);
        if (getCommand("cursormenu") != null) {
            getCommand("cursormenu").setExecutor(command);
            getCommand("cursormenu").setTabCompleter(command);
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new CursorMenuPlaceholder(this).register();
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(this::stopCursor);
    }

    public void reloadConfigs() {
        reloadConfig();
        config = ConfigUtils.loadConfig(this, "cursormenu/config.yml");
        itemDisplayManager.reload();
        textDisplayManager.reload();
    }

    public void setupCursor(Player player, String menuKey) {
        currentMenu.put(player.getUniqueId(), menuKey);
        // Additional cursor setup would be implemented by extensions
    }

    public void stopCursor(Player player) {
        currentMenu.remove(player.getUniqueId());
        itemDisplayManager.cleanup(player);
        textDisplayManager.cleanup(player);
    }

    public String getCurrentMenu(Player player) {
        return currentMenu.get(player.getUniqueId());
    }

    public SectionManager getSectionManager() { return sectionManager; }
    public ItemDisplayManager getItemDisplayManager() { return itemDisplayManager; }
    public TextDisplayManager getTextDisplayManager() { return textDisplayManager; }
    public FileConfiguration getCursorConfig() { return config; }
}
