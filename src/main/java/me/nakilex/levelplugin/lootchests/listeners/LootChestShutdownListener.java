package me.nakilex.levelplugin.lootchests.listeners;

import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

/**
 * Ensures all loot chest crate models are cleaned up when the plugin
 * or server shuts down.
 */
public class LootChestShutdownListener implements Listener {

    private final Plugin plugin;
    private final LootChestManager lootChestManager;

    public LootChestShutdownListener(Plugin plugin, LootChestManager lootChestManager) {
        this.plugin = plugin;
        this.lootChestManager = lootChestManager;
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (!event.getPlugin().equals(plugin)) return;
        plugin.getLogger().info("[LootChestShutdownListener] Cleaning up loot chest crates...");
        lootChestManager.removeAllChests();
    }
}
