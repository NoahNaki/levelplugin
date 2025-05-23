// File: src/main/java/me/nakilex/levelplugin/runes/loader/RuneLoader.java
package me.nakilex.levelplugin.runes.loader;

import me.nakilex.levelplugin.runes.model.Rune;
import me.nakilex.levelplugin.runes.model.Rune.Rarity;
import me.nakilex.levelplugin.runes.model.RuneEffect;
import me.nakilex.levelplugin.runes.model.RuneEffect.Type;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

@SuppressWarnings("unchecked")
public class RuneLoader {
    private final Plugin plugin;
    private final Map<String, Rune> runes = new HashMap<>();

    public RuneLoader(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Read runes.yml, parse every field (including description and extraParams) and cache in memory.
     * Supports both:
     *   runes: [ { id: "...", ... }, { id: "...", ... } ]
     * and
     *   runes:
     *     key1:
     *       id: "..."
     *       ...
     */
    public void loadRunes() {
        File file = new File(plugin.getDataFolder(), "runes.yml");
        if (!file.exists()) {
            plugin.saveResource("runes.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        runes.clear();

        // 1) If runes is defined as a List of Maps:
        if (config.isList("runes")) {
            @NotNull List<Map<?, ?>> list = config.getMapList("runes");
            for (Map<?, ?> map : list) {
                parseAndAddRune((Map<String, Object>) map);
            }
        }
        // 2) Else if runes is a section of named entries:
        else if (config.isConfigurationSection("runes")) {
            ConfigurationSection root = config.getConfigurationSection("runes");
            for (String key : root.getKeys(false)) {
                ConfigurationSection rs = root.getConfigurationSection(key);
                Map<String, Object> map = rs.getValues(true);
                parseAndAddRune(map);
            }
        }
        // else: nothing to load
    }

    private void parseAndAddRune(Map<String, Object> map) {
        String id          = Objects.toString(map.get("id"), UUID.randomUUID().toString());
        String displayName = Objects.toString(map.get("name"), "Unnamed Rune");
        List<String> description = (List<String>) map.getOrDefault("description", Collections.emptyList());
        Rarity rarity      = Rarity.valueOf(
            Objects.toString(map.get("rarity"), "COMMON").toUpperCase()
        );
        String cls         = Objects.toString(map.get("targetClass"), "");
        String spell       = Objects.toString(map.get("targetSpell"), "");
        boolean unique     = Boolean.parseBoolean(Objects.toString(map.get("unique"), "false"));

        // Effects
        List<RuneEffect> effects = new ArrayList<>();
        List<Map<String, Object>> effList =
            (List<Map<String, Object>>) map.getOrDefault("effects", Collections.emptyList());
        for (Map<String, Object> em : effList) {
            Type type = Type.valueOf(
                Objects.toString(em.get("type"), "MODIFIER").toUpperCase()
            );
            double bonusDmg  = ((Number) em.getOrDefault("bonusDamagePercent", 0)).doubleValue();
            double cdReduct  = ((Number) em.getOrDefault("cooldownReductionPercent", 0)).doubleValue();
            String newKey    = Objects.toString(em.get("newEffectKey"), null);
            int priority = ((Number) em.getOrDefault("priority", 0)).intValue();

            // extraParams may be null or a Map
            Map<String, Object> extraParams = Collections.emptyMap();
            Object xp = em.get("extraParams");
            if (xp instanceof Map<?, ?>) {
                extraParams = (Map<String, Object>) xp;
            }

            effects.add(new RuneEffect(type, bonusDmg, cdReduct, newKey, extraParams, priority));
        }

        runes.put(id, new Rune(
            id,
            displayName,
            description,
            rarity,
            cls,
            spell,
            unique,
            effects
        ));
    }

    public Rune getRune(String id) {
        return runes.get(id);
    }

    public List<Rune> getAllRunes() {
        return Collections.unmodifiableList(new ArrayList<>(runes.values()));
    }
}
