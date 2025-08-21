package me.nakilex.levelplugin.spells.utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

/**
 * Utility to load MythicMobs skill configurations so we can read cooldown values.
 */
public class MythicSkillConfig {
    private static final Map<String, Long> COOLDOWNS = new HashMap<>();

    private static File getSkillsDir() {
        var plugin = Bukkit.getPluginManager().getPlugin("MythicMobs");
        if (plugin == null) return null;
        return new File(plugin.getDataFolder(), "Skills");
    }

    /**
     * Ensure the given MythicMobs skill configuration from our jar exists in the
     * MythicMobs/Skills directory. Does nothing if MythicMobs is absent or the
     * file already exists.
     *
     * @param source       plugin providing the resource
     * @param resourceName resource path within the plugin jar
     */
    public static void ensureSkillFile(JavaPlugin source, String resourceName) {
        File dir = getSkillsDir();
        if (dir == null) return;
        if (!dir.exists() && !dir.mkdirs()) return;
        File target = new File(dir, resourceName);
        if (target.exists()) return;
        try (var in = source.getResource(resourceName)) {
            if (in != null) {
                Files.copy(in, target.toPath());
            }
        } catch (IOException e) {
            source.getLogger().log(Level.WARNING,
                    "Failed to copy Mythic skill file " + resourceName, e);
        }
    }

    /**
     * Returns the cooldown in seconds for the given Mythic skill name.
     * Returns 0 if the skill or cooldown is not found.
     */
    public static long getCooldownSeconds(String skillName) {
        if (COOLDOWNS.containsKey(skillName)) {
            return COOLDOWNS.get(skillName);
        }
        long cd = loadCooldown(skillName);
        COOLDOWNS.put(skillName, cd);
        return cd;
    }

    private static long loadCooldown(String skillName) {
        File dir = getSkillsDir();
        if (dir == null || !dir.isDirectory()) return 0L;
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return 0L;
        for (File f : files) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            if (cfg.contains(skillName)) {
                String path = skillName + ".Cooldown";
                if (cfg.contains(path)) {
                    return Math.round(cfg.getDouble(path, 0.0));
                }
            }
        }
        return 0L;
    }
}
