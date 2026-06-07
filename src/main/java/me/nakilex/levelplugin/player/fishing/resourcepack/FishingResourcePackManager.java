package me.nakilex.levelplugin.player.fishing.resourcepack;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.resourcepack.ResourcePackFragmentInstaller;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Installs and verifies the Nexo-managed fishing resource-pack fragment used by the glyph renderer. */
public final class FishingResourcePackManager {
    private static final String BUNDLED_FRAGMENT = "resourcepack/fishing_games";
    private static final String FALLBACK_MESSAGE = "Could not install fishing mini-game resource-pack fragment into Nexo. Falling back to text UI.";
    private static final String INSTALLED_MESSAGE = "Installed fishing mini-game resource-pack fragment into Nexo external_packs. Regenerate/reload Nexo pack to apply changes.";
    private static FishingResourcePackManager instance;

    private final Main plugin;
    private final Path nexoExternalPacks;
    private final Path installedPack;

    private FishingResourcePackManager(Main plugin) {
        this.plugin = plugin;
        Path pluginsDirectory = plugin.getDataFolder().toPath().getParent();
        if (pluginsDirectory == null) pluginsDirectory = Path.of("plugins");
        this.nexoExternalPacks = pluginsDirectory.resolve("Nexo/pack/external_packs");
        this.installedPack = nexoExternalPacks.resolve("levelplugin-fishing-games");
    }

    public static FishingResourcePackManager initialize(Main plugin) {
        FishingResourcePackManager manager = new FishingResourcePackManager(plugin);
        instance = manager;
        manager.installBundledFragment();
        manager.logAvailability();
        return manager;
    }

    public static FishingResourcePackManager getInstance() { return instance; }

    public static boolean canRenderGlyphUi(Main plugin) {
        return instance != null && instance.status().glyphUiEnabled();
    }

    public FishingPackStatus status() {
        boolean externalPacksExists = Files.isDirectory(nexoExternalPacks);
        boolean installedDirectoryExists = Files.isDirectory(installedPack);
        boolean packMetadataExists = Files.isRegularFile(installedPack.resolve("pack.mcmeta"));
        boolean defaultFontExists = Files.isRegularFile(installedPack.resolve("assets/customfishing/font/default.json"));
        boolean iconsFontExists = Files.isRegularFile(installedPack.resolve("assets/customfishing/font/icons.json"));
        boolean offsetFontExists = Files.isRegularFile(installedPack.resolve("assets/customfishing/font/offset_chars.json"));
        boolean glyphUiEnabled = plugin.getConfig().getBoolean("fishing-mini-games.resource-pack.enabled", true)
                && Bukkit.getPluginManager().getPlugin("Nexo") != null
                && externalPacksExists
                && installedDirectoryExists
                && packMetadataExists
                && defaultFontExists
                && iconsFontExists
                && offsetFontExists;
        return new FishingPackStatus(externalPacksExists, installedDirectoryExists,
                packMetadataExists, defaultFontExists, iconsFontExists, offsetFontExists,
                glyphUiEnabled,
                plugin.getConfig().getBoolean("fishing-mini-games.resource-pack.fallback-text-ui", true));
    }

    /** Copies a bundled fragment when present; Nexo remains responsible for generating and sending the final pack. */
    private void installBundledFragment() {
        if (Bukkit.getPluginManager().getPlugin("Nexo") == null) return;
        try {
            new ResourcePackFragmentInstaller(plugin, BUNDLED_FRAGMENT, installedPack).install();
            plugin.getLogger().info(INSTALLED_MESSAGE);
        } catch (IOException | URISyntaxException exception) {
            plugin.getLogger().warning(FALLBACK_MESSAGE);
            plugin.getLogger().warning("Fishing pack fragment installation failed: " + exception.getMessage());
        }
    }

    private void logAvailability() {
        if (!plugin.getConfig().getBoolean("fishing-mini-games.resource-pack.enabled", true)) return;
        if (!status().glyphUiEnabled()) plugin.getLogger().warning(FALLBACK_MESSAGE);
    }

    public record FishingPackStatus(boolean nexoExternalPacksExists, boolean installed,
                                    boolean packMetadataExists, boolean defaultFontExists,
                                    boolean iconsFontExists, boolean offsetFontExists,
                                    boolean glyphUiEnabled, boolean textFallbackEnabled) { }
}
