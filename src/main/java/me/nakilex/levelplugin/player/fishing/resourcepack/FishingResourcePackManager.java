package me.nakilex.levelplugin.player.fishing.resourcepack;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.resourcepack.ResourcePackFragmentInstaller;
import me.nakilex.levelplugin.resourcepack.ResourcePackFragmentStatus;

import java.util.List;

/** Installs and verifies the Nexo-managed fishing resource-pack fragment used by the glyph renderer. */
public final class FishingResourcePackManager {
    private static final String BUNDLED_FRAGMENT = "resourcepack/fishing_games";
    private static final String EXTERNAL_PACK_FOLDER = "levelplugin-fishing-games";
    private static final String FALLBACK_MESSAGE = "Could not install fishing mini-game resource-pack fragment into Nexo. Falling back to text UI.";
    private static final List<String> REQUIRED_FILES = List.of(
            "pack.mcmeta",
            "assets/customfishing/font/default.json",
            "assets/customfishing/font/icons.json",
            "assets/customfishing/font/offset_chars.json"
    );
    private static FishingResourcePackManager instance;

    private final Main plugin;
    private final ResourcePackFragmentInstaller installer;

    private FishingResourcePackManager(Main plugin) {
        this.plugin = plugin;
        this.installer = new ResourcePackFragmentInstaller(plugin, "fishing mini-game", BUNDLED_FRAGMENT,
                EXTERNAL_PACK_FOLDER, REQUIRED_FILES);
    }

    public static FishingResourcePackManager initialize(Main plugin) {
        FishingResourcePackManager manager = new FishingResourcePackManager(plugin);
        instance = manager;
        manager.installer.installBundledFragment();
        manager.logAvailability();
        return manager;
    }

    public static FishingResourcePackManager getInstance() { return instance; }

    public static boolean canRenderGlyphUi(Main plugin) {
        return instance != null && instance.status().glyphUiEnabled();
    }

    public FishingPackStatus status() {
        ResourcePackFragmentStatus status = fragmentStatus();
        return new FishingPackStatus(status.nexoExternalPacksExists(), status.installed(),
                status.requiredFileExists("pack.mcmeta"),
                status.requiredFileExists("assets/customfishing/font/default.json"),
                status.requiredFileExists("assets/customfishing/font/icons.json"),
                status.requiredFileExists("assets/customfishing/font/offset_chars.json"),
                status.glyphUiEnabled(), status.fallbackEnabled());
    }

    public ResourcePackFragmentStatus fragmentStatus() {
        return installer.status(
                plugin.getConfig().getBoolean("fishing-mini-games.resource-pack.enabled", true),
                plugin.getConfig().getBoolean("fishing-mini-games.resource-pack.fallback-text-ui", true));
    }

    private void logAvailability() {
        if (!plugin.getConfig().getBoolean("fishing-mini-games.resource-pack.enabled", true)) return;
        ResourcePackFragmentStatus status = fragmentStatus();
        if (!status.bundledResourceExists()) {
            plugin.getLogger().warning("Bundled fishing resource-pack folder '" + BUNDLED_FRAGMENT
                    + "' is missing; glyph fishing UI cannot be installed until assets are added.");
        }
        if (!status.glyphUiEnabled()) plugin.getLogger().warning(FALLBACK_MESSAGE);
    }

    public record FishingPackStatus(boolean nexoExternalPacksExists, boolean installed,
                                    boolean packMetadataExists, boolean defaultFontExists,
                                    boolean iconsFontExists, boolean offsetFontExists,
                                    boolean glyphUiEnabled, boolean textFallbackEnabled) { }
}
