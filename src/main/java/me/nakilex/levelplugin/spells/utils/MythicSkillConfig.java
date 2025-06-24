package me.nakilex.levelplugin.spells.utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
