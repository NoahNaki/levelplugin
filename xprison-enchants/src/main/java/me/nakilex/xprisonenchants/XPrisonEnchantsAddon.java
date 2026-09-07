package me.nakilex.xprisonenchants;

import dev.drawethree.xprison.api.XPrisonAPI;
import dev.drawethree.xprison.api.addons.XPrisonAddon;
import dev.drawethree.xprison.api.addons.XPrisonAddonContext;
import dev.drawethree.xprison.api.enchants.model.XPrisonEnchantment;
import me.nakilex.xprisonenchants.enchant.AcidRainEnchant;
import me.nakilex.xprisonenchants.enchant.BlackHoleEnchant;
import me.nakilex.xprisonenchants.enchant.MeteorShowerEnchant;
import me.nakilex.xprisonenchants.enchant.TornadoEnchant;
import me.nakilex.xprisonenchants.fx.Effects;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Registers the four cinematic area enchants with X-Prison.
 *
 * <p>Ported from the standalone PrisonEnchants plugin, which drove EdPrison and the Prison plugin's
 * explosive event directly. Under X-Prison these are {@code AreaBreakEnchant}s, so drops, auto-sell,
 * Fortune, prestige scaling, mine reset counters, pickaxe experience and quest progress are all
 * handled by the core — including on X-PrivateMines packet mines, where the blocks never exist
 * server-side.
 *
 * <p>The jar belongs in {@code plugins/X-Prison/addons/}, not {@code plugins/}.
 */
public final class XPrisonEnchantsAddon implements XPrisonAddon {

    private static final String[] ENCHANT_FILES = {
            "tornado.json", "blackhole.json", "meteors.json", "acidrain.json"
    };

    private final List<XPrisonEnchantment> enchants = new ArrayList<>();
    private XPrisonAPI api;
    private Logger logger;

    @Override
    public void onEnable(XPrisonAddonContext context) {
        this.api = context.getAPI();
        this.logger = context.getLogger();
        Effects.setPlugin(context.getPlugin());

        File dataFolder = context.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            logger.warning("Could not create data folder " + dataFolder + "; using defaults.");
        }
        copyDefaultConfigs(dataFolder);

        enchants.add(new TornadoEnchant(new File(dataFolder, "tornado.json")));
        enchants.add(new BlackHoleEnchant(new File(dataFolder, "blackhole.json")));
        enchants.add(new MeteorShowerEnchant(new File(dataFolder, "meteors.json")));
        enchants.add(new AcidRainEnchant(new File(dataFolder, "acidrain.json")));

        int registered = 0;
        for (XPrisonEnchantment enchant : enchants) {
            try {
                enchant.load();
                api.getEnchantsApi().registerEnchant(enchant);
                registered++;
            } catch (RuntimeException ex) {
                // The loader wraps parse failures, so the wrapped cause is the useful half.
                Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                logger.warning("Failed to register enchant " + enchant.getRawName()
                        + ": " + ex.getMessage() + " (cause: " + cause + ")");
            }
        }
        logger.info("Registered " + registered + "/" + enchants.size() + " cinematic enchants.");
    }

    @Override
    public void onDisable() {
        for (XPrisonEnchantment enchant : enchants) {
            try {
                api.getEnchantsApi().unregisterEnchant(enchant);
            } catch (RuntimeException ex) {
                logger.warning("Failed to unregister " + enchant.getRawName() + ": " + ex);
            }
        }
        enchants.clear();
        Effects.setPlugin(null);
    }

    /** Writes the bundled defaults on first run, never overwriting a config the server owner edited. */
    private void copyDefaultConfigs(File dataFolder) {
        for (String name : ENCHANT_FILES) {
            File target = new File(dataFolder, name);
            if (target.exists()) {
                continue;
            }
            try (InputStream in = getClass().getResourceAsStream("/" + name)) {
                if (in == null) {
                    logger.warning("Bundled config " + name + " is missing from the addon jar.");
                    continue;
                }
                Files.copy(in, target.toPath());
                logger.info("Wrote default config " + name);
            } catch (IOException ex) {
                logger.warning("Could not write " + name + ": " + ex.getMessage());
            }
        }
    }
}
