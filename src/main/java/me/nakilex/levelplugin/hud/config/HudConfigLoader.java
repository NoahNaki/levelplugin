package me.nakilex.levelplugin.hud.config;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.hud.conditions.HudConditionParser;
import me.nakilex.levelplugin.hud.core.HudElement;
import me.nakilex.levelplugin.hud.core.HudElementType;
import me.nakilex.levelplugin.hud.core.HudLayout;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public class HudConfigLoader {
    private final Main plugin;
    private final Logger logger;

    public HudConfigLoader(Main plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public HudConfig load() {
        File file = new File(plugin.getDataFolder(), "hud.yml");
        if (!file.exists()) {
            plugin.saveResource("hud.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        int updateTicks = Math.max(1, config.getInt("hud.update-interval-ticks", 2));
        int cacheTtl = Math.max(0, config.getInt("hud.placeholder-cache-ttl-ms", 150));
        String defaultLayout = config.getString("hud.default-layout", "default");

        Map<String, HudLayout> layouts = new HashMap<>();
        ConfigurationSection layoutsSection = config.getConfigurationSection("layouts");
        if (layoutsSection == null) {
            logger.warning("No HUD layouts found in hud.yml.");
            return new HudConfig(updateTicks, cacheTtl, defaultLayout, layouts);
        }

        HudConditionParser conditionParser = new HudConditionParser(logger);
        for (String layoutKey : layoutsSection.getKeys(false)) {
            List<Map<?, ?>> elementsRaw = config.getMapList("layouts." + layoutKey + ".elements");
            List<HudElement> elements = new ArrayList<>();
            for (int index = 0; index < elementsRaw.size(); index++) {
                Map<?, ?> map = elementsRaw.get(index);
                String id = getString(map, "id", layoutKey + "_element_" + index);
                String typeRaw = getString(map, "type", "text").toUpperCase(Locale.ROOT);
                HudElementType type;
                try {
                    type = HudElementType.valueOf(typeRaw);
                } catch (IllegalArgumentException ex) {
                    logger.warning("Unknown HUD element type '" + typeRaw + "' in layout " + layoutKey);
                    type = HudElementType.TEXT;
                }
                int x = getInt(map, "x", 0);
                int y = getInt(map, "y", 0);
                int layer = getInt(map, "layer", 0);
                String text = getString(map, "text", "");
                elements.add(new HudElement(id, type, x, y, layer, text, conditionParser.parse(map.get("conditions"))));
            }
            layouts.put(layoutKey, new HudLayout(layoutKey, elements));
        }

        return new HudConfig(updateTicks, cacheTtl, defaultLayout, layouts);
    }

    private static String getString(Map<?, ?> map, String key, String fallback) {
        if (map == null) {
            return fallback;
        }
        Object value = map.get(key);
        return value == null ? fallback : value.toString();
    }

    private static int getInt(Map<?, ?> map, String key, int fallback) {
        if (map == null) {
            return fallback;
        }
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
