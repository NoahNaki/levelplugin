package me.nakilex.levelplugin.managers;

import me.nakilex.levelplugin.mob.HorseData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class HorseConfigManager {

    private final File file;
    private final FileConfiguration config;

    public HorseConfigManager(File dataFolder) {
        file = new File(dataFolder, "horses.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    // Save horse data
    public void saveHorseData(UUID uuid, HorseData horseData) {
        config.set(uuid.toString() + ".type", horseData.getType());
        config.set(uuid.toString() + ".speed", horseData.getSpeed());
        config.set(uuid.toString() + ".jumpHeight", horseData.getJumpHeight());
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load horse data
    public HorseData loadHorseData(UUID uuid) {
        if (config.contains(uuid.toString())) {
            String type = config.getString(uuid.toString() + ".type");
            int speed = config.getInt(uuid.toString() + ".speed");
            int jumpHeight = config.getInt(uuid.toString() + ".jumpHeight");
            return new HorseData(type, speed, jumpHeight, uuid);
        }
        return null;
    }
}
