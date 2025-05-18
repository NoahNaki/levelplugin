// File: src/main/java/me/nakilex/levelplugin/runes/loader/RuneLoader.java
package me.nakilex.levelplugin.runes.loader;

import me.nakilex.levelplugin.runes.model.Rune;
import me.nakilex.levelplugin.runes.model.Rune.Rarity;
import me.nakilex.levelplugin.runes.model.RuneEffect;
import me.nakilex.levelplugin.runes.model.RuneEffect.Type;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;

/**
 * Loads runes from runes.yml into memory on plugin startup.
 */
public class RuneLoader {
    private final Plugin plugin;
    private final Map<String, Rune> runes = new HashMap<>();

    public RuneLoader(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Call during your plugin's onEnable to load (and generate if missing) the runes.yml file.
     */
    @SuppressWarnings("unchecked")
    public void loadRunes() {
        File file = new File(plugin.getDataFolder(), "runes.yml");
        if (!file.exists()) {
            plugin.saveResource("runes.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> list = config.getMapList("runes");

        for (Map<?, ?> data : list) {
            String id          = (String) data.get("id");
            String displayName = (String) data.get("name");
            Rarity rarity      = Rarity.valueOf(((String) data.get("rarity")).toUpperCase());
            String targetClass = (String) data.get("targetClass");
            String targetSpell = (String) data.get("targetSpell");
            boolean unique     = Boolean.TRUE.equals(data.get("unique"));

            // **NEW**: parse description block (list of strings)
            List<String> description = new ArrayList<>();
            Object rawDesc = data.get("description");
            if (rawDesc instanceof List<?>) {
                for (Object line : (List<?>) rawDesc) {
                    if (line != null) description.add(line.toString());
                }
            }

            // parse effects
            List<RuneEffect> effectsList = new ArrayList<>();
            List<Map<?, ?>> effectsData = (List<Map<?, ?>>) data.get("effects");
            if (effectsData != null) {
                for (Map<?, ?> edata : effectsData) {
                    Type type = Type.valueOf(((String) edata.get("type")).toUpperCase());
                    double bonusDamage = edata.get("bonusDamagePercent") instanceof Number
                        ? ((Number) edata.get("bonusDamagePercent")).doubleValue()
                        : 0.0;
                    double cooldownRed = edata.get("cooldownReductionPercent") instanceof Number
                        ? ((Number) edata.get("cooldownReductionPercent")).doubleValue()
                        : 0.0;
                    String newEffectKey = edata.get("newEffectKey") != null
                        ? edata.get("newEffectKey").toString()
                        : null;

                    // extraParams may be null or a map
                    Map<String, Object> extraParams = new HashMap<>();
                    Object rawParams = edata.get("extraParams");
                    if (rawParams instanceof Map<?, ?>) {
                        ((Map<?, ?>) rawParams).forEach((k, v) -> extraParams.put(k.toString(), v));
                    }

                    effectsList.add(new RuneEffect(
                        type, bonusDamage, cooldownRed, newEffectKey, extraParams
                    ));
                }
            }

            // construct and store the rune
            Rune rune = new Rune(
                id,
                displayName,
                description,
                rarity,
                targetClass,
                targetSpell,
                unique,
                effectsList
            );
            runes.put(id, rune);
        }

        plugin.getLogger().info("Loaded " + runes.size() + " runes from runes.yml");
    }

    /**
     * Retrieve a rune by its unique ID.
     */
    public Rune getRune(String id) {
        return runes.get(id);
    }

    /**
     * Get all loaded runes as an unmodifiable list.
     * Used by RuneBrowser to display every template.
     */
    public List<Rune> getAllRunes() {
        return Collections.unmodifiableList(new ArrayList<>(runes.values()));
    }
}
