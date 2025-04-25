package me.nakilex.levelplugin.horse.utils;

import me.nakilex.levelplugin.horse.managers.HorseConfigManager;
import me.nakilex.levelplugin.horse.data.HorseData;
import me.nakilex.levelplugin.horse.managers.HorseManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class HorseSaverTask implements Runnable {

    private final HorseManager horseManager;
    private final HorseConfigManager configManager;


    public HorseSaverTask(HorseManager horseManager, HorseConfigManager configManager) {
        this.horseManager = horseManager;
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
    private void saveAllHorseData() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            HorseData data = horseManager.getHorse(uuid);
            if (data != null) {
                configManager.saveHorseData(uuid, data);
            }
        }
    }

    // Schedule periodic saves
    public void runTaskTimer(JavaPlugin plugin, long delay, long period) {
        new BukkitRunnable() {
            @Override
            public void run() {
                saveAllHorseData();
            }
        }.runTaskTimer(plugin, delay, period);
    }
}
