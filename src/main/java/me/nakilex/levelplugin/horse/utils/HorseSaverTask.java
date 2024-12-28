package me.nakilex.levelplugin.horse.utils;

import me.nakilex.levelplugin.horse.managers.HorseConfigManager;
import me.nakilex.levelplugin.horse.data.HorseData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class HorseSaverTask implements Runnable {

    private final HorseConfigManager configManager;

    public HorseSaverTask(HorseConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            HorseData horseData = configManager.loadHorseData(uuid);
            if (horseData != null) {
                configManager.saveHorseData(uuid, horseData);
            }
        }
    }
    public void runTaskTimer(JavaPlugin plugin, long delay, long period) {
        new BukkitRunnable() {
            @Override
            public void run() {
                saveAllHorseData();
            }
        }.runTaskTimer(plugin, delay, period);
    }

    // Method to save all horse data (example implementation)
    private void saveAllHorseData() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            HorseData horseData = configManager.loadHorseData(uuid);
            if (horseData != null) {
                configManager.saveHorseData(uuid, horseData);
            }
        }
    }

}
