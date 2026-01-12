package me.nakilex.npc.plugin.integration;

import me.nakilex.npc.plugin.service.NpcService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class IntegrationManager {
    private final NpcService service;

    public IntegrationManager(NpcService service) {
        this.service = service;
    }

    public void register() {
        Plugin plugin = service.getPlugin();
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NpcPlaceholderExpansion(service).register();
            plugin.getLogger().info("Registered PlaceholderAPI integration.");
        }
        logIfPresent("Vault", plugin);
        logIfPresent("WorldGuard", plugin);
        logIfPresent("ModelEngine", plugin);
        logIfPresent("MythicMobs", plugin);
    }

    private void logIfPresent(String name, Plugin plugin) {
        if (Bukkit.getPluginManager().getPlugin(name) != null) {
            plugin.getLogger().info("Detected " + name + " integration support.");
        }
    }
}
