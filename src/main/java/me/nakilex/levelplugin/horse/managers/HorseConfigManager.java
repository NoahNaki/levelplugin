package me.nakilex.levelplugin.horse.managers;

import me.nakilex.levelplugin.horse.data.HorseData;
import me.nakilex.levelplugin.horse.traits.TraitRegistry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.Set;
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

    // Save horse data, including variant flag
    public void saveHorseData(UUID uuid, HorseData horseData) {
        String base = uuid.toString();
        config.set(base + ".type", horseData.getType());
        config.set(base + ".isVariant", horseData.isVariant());
        config.set(base + ".speed", horseData.getSpeed());
        config.set(base + ".jumpHeight", horseData.getJumpHeight());
        config.set(base + ".trait", horseData.getTraitId());
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load horse data
    public HorseData loadHorseData(UUID uuid) {
        String base = uuid.toString();
        if (config.contains(base)) {
            String type = config.getString(base + ".type");
            boolean isVariant = config.getBoolean(base + ".isVariant");
              int speed = config.getInt(base + ".speed");
              int jumpHeight = config.getInt(base + ".jumpHeight");
              String trait = config.getString(base + ".trait");
              if (trait == null || TraitRegistry.get(trait) == null) {
                  trait = TraitRegistry.getRandomId(new Random());
                  config.set(base + ".trait", trait);
                  try {
                      config.save(file);
                  } catch (IOException e) {
                      e.printStackTrace();
                  }
              }
              return new HorseData(type, isVariant, speed, jumpHeight, uuid, trait);
        }
        return null;
    }

    // Expose all saved UUIDs for initial loading
    public Set<String> getHorseUUIDStrings() {
        return config.getKeys(false);
    }

    // Delete stored horse data for a player
    public void deleteHorseData(UUID uuid) {
        config.set(uuid.toString(), null);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
