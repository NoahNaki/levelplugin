package me.nakilex.levelplugin.spells;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SpellManager {

    private static SpellManager instance;
    public static SpellManager getInstance() { return instance; }

    // Map: class -> (combo -> Spell)
    private final Map<String, Map<String, Spell>> spellsByClass = new HashMap<>();

    public SpellManager(Plugin plugin) {
        instance = this;
        loadSpellsConfig(plugin);
    }

    private void loadSpellsConfig(Plugin plugin) {
        File file = new File(plugin.getDataFolder(), "spells.yml");
        if (!file.exists()) {
            plugin.saveResource("spells.yml", false); // copy default from jar
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        if (!config.contains("spells")) {
            plugin.getLogger().warning("No 'spells' section found in spells.yml!");
            return;
        }

        // For each class key
        for (String classKey : config.getConfigurationSection("spells").getKeys(false)) {
            Map<String, Spell> classSpells = new HashMap<>();

            // Path: spells.warrior.*   spells.rogue.*
            String classPath = "spells." + classKey;
            for (String spellId : config.getConfigurationSection(classPath).getKeys(false)) {
                String path = classPath + "." + spellId;

                String displayName   = config.getString(path + ".display_name", spellId);
                String combo         = config.getString(path + ".combo", "RRR");
                int manaCost         = config.getInt(path + ".mana_cost", 5);
                int cooldown         = config.getInt(path + ".cooldown", 5);
                int levelReq         = config.getInt(path + ".level_req", 1);
                String effectKey     = config.getString(path + ".effect_key", "NONE");
                double dmgMultiplier = config.getDouble(path + ".damage_multiplier", 1.0);

                // allowed_weapons is a list of strings we must convert to Material
                List<String> weaponStrList = config.getStringList(path + ".allowed_weapons");
                List<Material> allowedWeps = new ArrayList<>();
                for (String w : weaponStrList) {
                    try {
                        Material mat = Material.valueOf(w.toUpperCase());
                        allowedWeps.add(mat);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid material '" + w + "' in " + path);
                    }
                }

                Spell spell = new Spell(
                    spellId,
                    displayName,
                    combo,
                    manaCost,
                    cooldown,
                    levelReq,
                    allowedWeps,
                    effectKey,
                    dmgMultiplier
                );

                classSpells.put(combo, spell);
            }
            spellsByClass.put(classKey.toLowerCase(), classSpells);
        }
    }

    public Spell getSpell(String className, String combo) {
        Map<String, Spell> classMap = spellsByClass.get(className.toLowerCase());
        if (classMap == null) return null;
        return classMap.get(combo);
    }
}
