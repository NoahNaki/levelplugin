package me.nakilex.levelplugin.xprison;

import dev.drawethree.xprison.api.XPrisonAPI;
import dev.drawethree.xprison.api.enchants.XPrisonEnchantsAPI;
import dev.drawethree.xprison.api.enchants.model.XPrisonEnchantment;
import me.nakilex.levelplugin.Main;
import me.nakilex.xprisonenchants.enchant.AcidRainEnchant;
import me.nakilex.xprisonenchants.enchant.BlackHoleEnchant;
import me.nakilex.xprisonenchants.enchant.MeteorShowerEnchant;
import me.nakilex.xprisonenchants.enchant.TornadoEnchant;
import me.nakilex.xprisonenchants.fx.Effects;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Owns LevelPlugin's custom X-Prison enchantments.
 *
 * <p>This replaces the old separate XPrisonEnchants addon jar. X-Prison itself is still an
 * external server dependency, but Tornado, Black Hole, Meteor Shower and Acid Rain now live in
 * levelplugin.jar and are registered directly through the public X-Prison API.</p>
 */
public final class XPrisonEnchantsIntegration {

    private static final String[] ENCHANT_FILES = {
            "tornado.json", "blackhole.json", "meteors.json", "acidrain.json"
    };

    private final Main plugin;
    private final List<XPrisonEnchantment> registeredEnchants = new ArrayList<>();
    private XPrisonAPI api;
    private boolean enabled;

    public XPrisonEnchantsIntegration(Main plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        if (enabled) {
            return;
        }

        if (!Bukkit.getPluginManager().isPluginEnabled("X-Prison")) {
            plugin.getLogger().info("X-Prison is not enabled; integrated custom prison enchants are disabled.");
            return;
        }

        try {
            api = XPrisonAPI.getInstance();
        } catch (RuntimeException | LinkageError ex) {
            plugin.getLogger().warning("Could not access X-Prison API; custom prison enchants were not registered: "
                    + ex.getMessage());
            return;
        }

        if (api == null) {
            plugin.getLogger().warning("X-Prison API returned null; custom prison enchants were not registered.");
            return;
        }

        Effects.setPlugin(plugin);

        File dataFolder = new File(plugin.getDataFolder(), "xprison-enchants");
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create X-Prison enchant data folder: " + dataFolder);
        }

        migrateOldAddonConfigs(dataFolder);
        copyDefaultConfigs(dataFolder);
        migrateLegacyProcChances(dataFolder);

        List<XPrisonEnchantment> candidates = List.of(
                new TornadoEnchant(new File(dataFolder, "tornado.json")),
                new BlackHoleEnchant(new File(dataFolder, "blackhole.json")),
                new MeteorShowerEnchant(new File(dataFolder, "meteors.json")),
                new AcidRainEnchant(new File(dataFolder, "acidrain.json"))
        );

        XPrisonEnchantsAPI enchantsApi = api.getEnchantsApi();
        int registered = 0;
        int duplicates = 0;

        for (XPrisonEnchantment enchant : candidates) {
            try {
                enchant.load();

                XPrisonEnchantment existingById = enchantsApi.getById(enchant.getId());
                XPrisonEnchantment existingByName = enchantsApi.getByName(enchant.getRawName());
                if (existingById != null || existingByName != null) {
                    duplicates++;
                    plugin.getLogger().warning("Skipped integrated X-Prison enchant '" + enchant.getRawName()
                            + "' because an enchant with id " + enchant.getId()
                            + " or the same name is already registered. If the old XPrisonEnchants.jar is still in "
                            + "plugins/X-Prison/addons/, remove it and restart the server.");
                    continue;
                }

                if (enchantsApi.registerEnchant(enchant)) {
                    registeredEnchants.add(enchant);
                    registered++;
                } else {
                    plugin.getLogger().warning("X-Prison rejected custom enchant " + enchant.getRawName() + ".");
                }
            } catch (RuntimeException ex) {
                Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                plugin.getLogger().warning("Failed to register integrated X-Prison enchant " + enchant.getRawName()
                        + ": " + ex.getMessage() + " (cause: " + cause + ")");
            }
        }

        enabled = true;
        plugin.getLogger().info("Integrated X-Prison enchants: registered " + registered + "/"
                + candidates.size() + (duplicates > 0 ? " (" + duplicates + " already registered)" : "") + ".");
    }

    public void disable() {
        if (api != null) {
            XPrisonEnchantsAPI enchantsApi = api.getEnchantsApi();
            for (XPrisonEnchantment enchant : registeredEnchants) {
                try {
                    enchantsApi.unregisterEnchant(enchant);
                } catch (RuntimeException ex) {
                    plugin.getLogger().warning("Failed to unregister integrated X-Prison enchant "
                            + enchant.getRawName() + ": " + ex.getMessage());
                }
            }
        }

        registeredEnchants.clear();
        Effects.setPlugin(null);
        api = null;
        enabled = false;
    }

    /**
     * Moves existing addon config forward on first integrated run so server-side tuning is kept.
     * X-Prison's addon manager used plugins/X-Prison/addons/XPrisonEnchants as the old data folder.
     */
    private void migrateOldAddonConfigs(File newDataFolder) {
        File oldDataFolder = new File(plugin.getServer().getPluginsFolder(), "X-Prison/addons/XPrisonEnchants");
        if (!oldDataFolder.isDirectory()) {
            return;
        }

        int copied = 0;
        for (String name : ENCHANT_FILES) {
            File source = new File(oldDataFolder, name);
            File target = new File(newDataFolder, name);
            if (!source.isFile() || target.exists()) {
                continue;
            }
            try {
                Files.copy(source.toPath(), target.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
                copied++;
            } catch (IOException ex) {
                plugin.getLogger().warning("Could not migrate old X-Prison enchant config " + name + ": "
                        + ex.getMessage());
            }
        }

        if (copied > 0) {
            plugin.getLogger().info("Migrated " + copied + " X-Prison enchant config(s) from the old addon folder to "
                    + newDataFolder + ".");
        }
    }

    /** Writes bundled defaults only when a config does not already exist. */
    private void copyDefaultConfigs(File dataFolder) {
        for (String name : ENCHANT_FILES) {
            File target = new File(dataFolder, name);
            if (target.exists()) {
                continue;
            }

            try (InputStream in = plugin.getResource("xprison-enchants/" + name)) {
                if (in == null) {
                    plugin.getLogger().warning("Bundled X-Prison enchant config is missing: " + name);
                    continue;
                }
                Files.copy(in, target.toPath());
                plugin.getLogger().info("Wrote default X-Prison enchant config " + name + ".");
            } catch (IOException ex) {
                plugin.getLogger().warning("Could not write X-Prison enchant config " + name + ": "
                        + ex.getMessage());
            }
        }
    }

    /**
     * Fixes only the exact old defaults that were 100x too small. Custom server values are retained.
     */
    private void migrateLegacyProcChances(File dataFolder) {
        migrateLegacyChance(new File(dataFolder, "tornado.json"), "0.0015", "0.15");
        migrateLegacyChance(new File(dataFolder, "blackhole.json"), "0.001", "0.10");
        migrateLegacyChance(new File(dataFolder, "meteors.json"), "0.0009", "0.09");
        migrateLegacyChance(new File(dataFolder, "acidrain.json"), "0.0012", "0.12");
    }

    private void migrateLegacyChance(File file, String oldValue, String newValue) {
        if (!file.isFile()) {
            return;
        }

        try {
            String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            Pattern pattern = Pattern.compile("(\\\"chance\\\"\\s*:\\s*)"
                    + Pattern.quote(oldValue) + "(?=\\s*[,}])");
            Matcher matcher = pattern.matcher(json);
            if (!matcher.find()) {
                return;
            }

            String replacement = matcher.group(1) + newValue;
            String migrated = matcher.replaceFirst(Matcher.quoteReplacement(replacement));
            Files.writeString(file.toPath(), migrated, StandardCharsets.UTF_8);
            plugin.getLogger().info("Migrated legacy proc chance in " + file.getName()
                    + " from " + oldValue + "% to " + newValue + "%.");
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not migrate proc chance in " + file.getName() + ": " + ex.getMessage());
        }
    }
}
