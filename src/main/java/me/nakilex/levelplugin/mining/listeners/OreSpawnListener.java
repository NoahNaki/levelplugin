package me.nakilex.levelplugin.mining.listeners;

import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import me.nakilex.levelplugin.mining.config.MiningConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class OreSpawnListener implements Listener {
    private final OreListener oreListener;
    private final MiningConfig config;

    public OreSpawnListener(OreListener oreListener, MiningConfig config) {
        this.oreListener = oreListener;
        this.config = config;
    }

    @EventHandler
    public void onSpawn(MythicMobSpawnEvent event) {
        String type = event.getMob().getMobType();
        ConfigurationSection sec = config.getConfig().getConfigurationSection("ores." + type);
        if (sec != null) {
            // MythicMobSpawnEvent#getEntity() already returns the Bukkit entity
            // so no need for getBukkitEntity() which caused a compile error
            oreListener.spawnHologram(event.getEntity(), type);
        }
    }
}
