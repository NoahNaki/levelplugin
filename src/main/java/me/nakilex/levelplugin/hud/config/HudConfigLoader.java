package me.nakilex.levelplugin.hud.config;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.hud.assets.HudImageDefinition;
import me.nakilex.levelplugin.hud.assets.HudImageType;
import me.nakilex.levelplugin.hud.assets.HudSplitType;
import me.nakilex.levelplugin.hud.conditions.HudConditionParser;
import me.nakilex.levelplugin.hud.core.HudElement;
import me.nakilex.levelplugin.hud.core.HudElementType;
import me.nakilex.levelplugin.hud.core.HudLayout;
import me.nakilex.levelplugin.hud.core.HudLayoutPlacement;
import me.nakilex.levelplugin.hud.core.HudModule;
import me.nakilex.levelplugin.hud.core.HudTextAlign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public class HudConfigLoader {
    private static final String BASE_FOLDER = "hud";
    private static final String CONFIG_FILE = "hud/config.yml";
    private static final String MODULES_FOLDER = "hud/modules";
    private static final String LAYOUTS_FOLDER = "hud/layouts";
    private static final String IMAGES_FOLDER = "hud/images";

    private final Main plugin;
    private final Logger logger;

    public HudConfigLoader(Main plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public HudConfig load() {
        ensureDefaults();
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), CONFIG_FILE));
        int updateTicks = Math.max(1, config.getInt("hud.update-interval-ticks", 1));
        int cacheTtl = Math.max(0, config.getInt("hud.placeholder-cache-ttl-ms", 150));
        int canvasWidth = Math.max(120, config.getInt("hud.canvas-width-px", 320));
        int lineHeight = Math.max(1, config.getInt("hud.line-height-px", 18));
        int bossbarLines = Math.max(1, config.getInt("hud.bossbar.lines", 1));
        boolean mergeBossBar = config.getBoolean("hud.bossbar.merge", true);
        String namespace = config.getString("hud.namespace", "betterhud");
        String outputFolder = config.getString("hud.output-folder", "Nexo/pack/external_packs/BetterHud");
        String sourceTexturesFolder = config.getString("hud.source-textures-folder",
                "plugins/Nexo/pack/external_packs/BetterHud/assets/betterhud/textures");
        List<String> defaultModules = config.getStringList("hud.default-modules");
        String imagesConfigPath = new File(plugin.getDataFolder(), IMAGES_FOLDER).getAbsolutePath();

        Map<String, HudModule> modules = loadModules();
        Map<String, HudLayout> layouts = loadLayouts();
        Map<String, HudImageDefinition> images = loadImages();

        return new HudConfig(updateTicks, cacheTtl, canvasWidth, lineHeight, bossbarLines, mergeBossBar,
                namespace, outputFolder, sourceTexturesFolder, imagesConfigPath, defaultModules, modules, layouts, images);
    }

    private void ensureDefaults() {
        File base = new File(plugin.getDataFolder(), BASE_FOLDER);
        File modules = new File(plugin.getDataFolder(), MODULES_FOLDER);
        File layouts = new File(plugin.getDataFolder(), LAYOUTS_FOLDER);
        File images = new File(plugin.getDataFolder(), IMAGES_FOLDER);
        createDir(base);
        createDir(modules);
        createDir(layouts);
        createDir(images);
        saveIfMissing(CONFIG_FILE);
        saveIfMissing("hud/modules/fantasy_hud.yml");
        saveIfMissing("hud/layouts/fantasy_layout.yml");
        saveIfMissing("hud/images/fantasy_images.yml");
        migrateFantasyImagesSplitTypes();
    }

    private void createDir(File dir) {
        if (!dir.exists() && !dir.mkdirs()) {
            logger.warning("Failed to create HUD directory: " + dir.getAbsolutePath());
        }
    }

    private void saveIfMissing(String resourcePath) {
        File target = new File(plugin.getDataFolder(), resourcePath);
        if (!target.exists()) {
            plugin.saveResource(resourcePath, false);
        }
    }

    private void migrateFantasyImagesSplitTypes() {
        File configFile = new File(plugin.getDataFolder(), "hud/images/fantasy_images.yml");
        if (!configFile.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        Map<String, String> expectedSplitTypes = Map.of(
                "air_bar_image", "left",
                "hp_bar_image", "left",
                "mana_bar_image", "left",
                "stamina_bar_image", "left",
                "armor_bar_image", "left",
                "level_bar_image", "up"
        );
        boolean changed = false;
        for (Map.Entry<String, String> entry : expectedSplitTypes.entrySet()) {
            String key = entry.getKey();
            String expected = entry.getValue();
            String current = config.getString(key + ".split-type", "");
            if (current == null) {
                current = "";
            }
            String normalized = current.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()
                    || (!expected.equalsIgnoreCase(normalized) && "left".equals(expected) && "up".equals(normalized))) {
                config.set(key + ".split-type", expected);
                changed = true;
            }
        }
        if (changed) {
            try {
                config.save(configFile);
            } catch (IOException e) {
                logger.warning("Failed to update fantasy images split types: " + e.getMessage());
            }
        }
    }

    private Map<String, HudModule> loadModules() {
        Map<String, HudModule> modules = new HashMap<>();
        File folder = new File(plugin.getDataFolder(), MODULES_FOLDER);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return modules;
        }
        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            for (String moduleId : config.getKeys(false)) {
                String normalizedId = moduleId.toLowerCase(Locale.ROOT);
                List<Map<?, ?>> layouts = config.getMapList(moduleId + ".layouts");
                List<HudLayoutPlacement> placements = new ArrayList<>();
                for (Map<?, ?> entry : layouts) {
                    String layoutName = getString(entry, "name", "");
                    int x = getInt(entry, "x", 0);
                    int y = getInt(entry, "y", 0);
                    if (!layoutName.isBlank()) {
                        placements.add(new HudLayoutPlacement(layoutName, x, y));
                    }
                }
                modules.put(normalizedId, new HudModule(moduleId, placements));
            }
        }
        return modules;
    }

    private Map<String, HudLayout> loadLayouts() {
        Map<String, HudLayout> layouts = new HashMap<>();
        File folder = new File(plugin.getDataFolder(), LAYOUTS_FOLDER);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return layouts;
        }
        HudConditionParser conditionParser = new HudConditionParser(logger);
        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            for (String layoutId : config.getKeys(false)) {
                String normalizedId = layoutId.toLowerCase(Locale.ROOT);
                List<Map<?, ?>> elementsRaw = config.getMapList(layoutId + ".elements");
                List<HudElement> elements = new ArrayList<>();
                for (int index = 0; index < elementsRaw.size(); index++) {
                    Map<?, ?> map = elementsRaw.get(index);
                    String id = getString(map, "id", layoutId + "_element_" + index);
                    HudElementType type = HudElementType.from(getString(map, "type", "text"));
                    int x = getInt(map, "x", 0);
                    int y = getInt(map, "y", 0);
                    int layer = getInt(map, "layer", 0);
                    double scale = getDouble(map, "scale", 1.0);
                    HudTextAlign align = HudTextAlign.from(getString(map, "align", "left"));
                    String text = getString(map, "text", "");
                    String asset = getString(map, "asset", "");
                    String anchor = getString(map, "anchor", "");
                    elements.add(new HudElement(id, type, x, y, layer, scale, align, text, asset, anchor,
                            conditionParser.parse(map.get("conditions"))));
                }
                layouts.put(normalizedId, new HudLayout(layoutId, elements));
            }
        }
        return layouts;
    }

    private Map<String, HudImageDefinition> loadImages() {
        Map<String, HudImageDefinition> images = new HashMap<>();
        File folder = new File(plugin.getDataFolder(), IMAGES_FOLDER);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return images;
        }
        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            for (String key : config.getKeys(false)) {
                String normalizedKey = key.toLowerCase(Locale.ROOT);
                String typeRaw = config.getString(key + ".type", "single");
                String texture = config.getString(key + ".texture", "");
                int split = config.getInt(key + ".split", 0);
                String splitTypeRaw = config.getString(key + ".split-type", "up");
                String current = config.getString(key + ".current", "");
                String max = config.getString(key + ".max", "");
                List<String> frames = config.getStringList(key + ".frames");
                HudImageDefinition def = new HudImageDefinition(key,
                        HudImageType.from(typeRaw),
                        texture,
                        split,
                        HudSplitType.from(splitTypeRaw),
                        current,
                        max,
                        frames);
                images.put(normalizedKey, def);
            }
        }
        return images;
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

    private static double getDouble(Map<?, ?> map, String key, double fallback) {
        if (map == null) {
            return fallback;
        }
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
