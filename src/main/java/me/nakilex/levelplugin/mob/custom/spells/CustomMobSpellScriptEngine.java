package me.nakilex.levelplugin.mob.custom.spells;

import me.nakilex.levelplugin.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads custom-mob spell scripts from plugin data files.
 */
public class CustomMobSpellScriptEngine {
    private static final String LEGACY_FILE = "custom_mob_spells.yml";
    private static final String SCRIPT_FOLDER = "custom_mob_spells";

    private final Main plugin;
    private final Map<String, SpellScript> scripts = new HashMap<>();

    public CustomMobSpellScriptEngine(Main plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        scripts.clear();
        loadLegacyFile();
        loadScriptFolder();
    }

    public SpellScript getScript(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return scripts.get(key.trim().toLowerCase(Locale.ROOT));
    }

    private void loadLegacyFile() {
        File legacy = new File(plugin.getDataFolder(), LEGACY_FILE);
        if (!legacy.exists()) {
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(legacy);
        if (cfg.getKeys(false).isEmpty()) {
            return;
        }
        for (String key : cfg.getKeys(false)) {
            ConfigurationSection section = cfg.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            SpellScript script = parseScript(section, key);
            registerScript(script, key);
        }
    }

    private void loadScriptFolder() {
        File folder = new File(plugin.getDataFolder(), SCRIPT_FOLDER);
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Failed to create custom_mob_spells folder.");
            return;
        }
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String stem = file.getName().substring(0, file.getName().length() - 4);
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            SpellScript script = parseScript(cfg, stem);
            registerScript(script, stem);
        }
    }

    private SpellScript parseScript(ConfigurationSection section, String fallbackId) {
        String id = section.getString("id", fallbackId);
        List<SpellAction> actions = new ArrayList<>();

        List<Map<?, ?>> rawActionList = section.getMapList("actions");
        for (Map<?, ?> raw : rawActionList) {
            Object rawType = raw.containsKey("type") ? raw.get("type") : "";
            String type = String.valueOf(rawType).trim().toLowerCase(Locale.ROOT);
            if (type.isEmpty()) {
                continue;
            }
            Map<String, Object> args = new HashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if ("type".equalsIgnoreCase(key)) {
                    continue;
                }
                args.put(key, entry.getValue());
            }
            actions.add(new SpellAction(type, args));
        }

        return new SpellScript(id, List.copyOf(actions));
    }

    private void registerScript(SpellScript script, String stem) {
        String normalizedStem = normalizeKey(stem);
        String normalizedId = normalizeKey(script.id());
        if (normalizedId != null) {
            scripts.put(normalizedId, script);
        }
        if (normalizedStem != null) {
            scripts.put(normalizedStem, script);
        }
    }

    private String normalizeKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public record SpellScript(String id, List<SpellAction> actions) {
    }

    public record SpellAction(String type, Map<String, Object> args) {
    }
}
