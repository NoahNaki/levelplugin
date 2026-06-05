package me.nakilex.levelplugin.quests.dialogue.hud;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.resourcepack.ResourcePackFragmentInstaller;
import me.nakilex.levelplugin.resourcepack.ResourcePackFragmentStatus;

import java.util.LinkedHashSet;
import java.util.List;

/** Installs and verifies the optional Nexo external pack fragment for the future dialogue HUD. */
public final class DialogueHudResourcePackManager {
    private static final String BUNDLED_FRAGMENT = "resourcepack/dialogue_hud";
    private static final String EXTERNAL_PACK_FOLDER = "levelplugin-dialogue-hud";
    private static final List<String> DEFAULT_REQUIRED_FILES = List.of(
            "pack.mcmeta",
            "assets/levelplugin_dialogue/font/dialogue.json",
            "assets/levelplugin_dialogue/font/offset_chars.json"
    );
    private static DialogueHudResourcePackManager instance;

    private final Main plugin;
    private final ResourcePackFragmentInstaller installer;
    private final DialogueHudPackStatusListener packStatusListener;

    private DialogueHudResourcePackManager(Main plugin) {
        this.plugin = plugin;
        this.installer = new ResourcePackFragmentInstaller(plugin, "dialogue HUD", BUNDLED_FRAGMENT,
                EXTERNAL_PACK_FOLDER, configuredRequiredFiles(plugin));
        this.packStatusListener = new DialogueHudPackStatusListener(plugin);
        plugin.getServer().getPluginManager().registerEvents(packStatusListener, plugin);
    }

    public static DialogueHudResourcePackManager initialize(Main plugin) {
        DialogueHudResourcePackManager manager = new DialogueHudResourcePackManager(plugin);
        instance = manager;
        if (manager.resourcePackEnabled()) {
            manager.installer.installBundledFragment();
            manager.logAvailability();
        }
        return manager;
    }

    public static DialogueHudResourcePackManager getInstance() { return instance; }

    public ResourcePackFragmentStatus status() {
        return installer.status(resourcePackEnabled(), fallbackChatRendererEnabled());
    }

    public DialogueHudPackStatusListener packStatusListener() {
        return packStatusListener;
    }

    public boolean rendererEnabled() {
        return plugin.getConfig().getBoolean("dialogue-hud.renderer.enabled", true);
    }

    public String rendererMode() {
        String mode = plugin.getConfig().getString("dialogue-hud.renderer.mode", "actionbar");
        return mode == null || mode.isBlank() ? "actionbar" : mode.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public boolean actionBarMode() {
        return !"chat".equals(rendererMode());
    }

    public boolean useResourcePackGlyphs() {
        return plugin.getConfig().getBoolean("dialogue-hud.renderer.use-resource-pack-glyphs", true);
    }

    public boolean requireClientPackLoaded() {
        return plugin.getConfig().getBoolean("dialogue-hud.renderer.require-client-pack-loaded", true);
    }

    public boolean debugForceGlyphs() {
        return plugin.getConfig().getBoolean("dialogue-hud.renderer.debug-force-glyphs", false);
    }

    public boolean debugLogging() {
        return plugin.getConfig().getBoolean("dialogue-hud.debug.log-pack-status", true);
    }

    public boolean canRenderGlyphUi(org.bukkit.entity.Player player) {
        return rendererEnabled()
                && actionBarMode()
                && serverGlyphFilesReady()
                && useResourcePackGlyphs()
                && (debugForceGlyphs() || !requireClientPackLoaded() || packStatusListener.hasLoadedPack(player));
    }

    public boolean playerCanUseGlyphs(org.bukkit.entity.Player player) {
        return canRenderGlyphUi(player);
    }

    public boolean serverGlyphFilesReady() {
        return status().glyphUiEnabled();
    }

    public String glyphDebugReason(org.bukkit.entity.Player player) {
        if (!rendererEnabled()) return "renderer disabled in config";
        if (!actionBarMode()) return "renderer mode is chat";
        if (!useResourcePackGlyphs()) return "renderer glyphs disabled in config";
        if (!serverGlyphFilesReady()) return "server-side dialogue HUD pack files are incomplete or Nexo is unavailable";
        if (debugForceGlyphs()) return "debug-force-glyphs enabled";
        if (requireClientPackLoaded() && !packStatusListener.hasLoadedPack(player)) {
            return "player has not reported SUCCESSFULLY_LOADED for the resource pack";
        }
        return requireClientPackLoaded() ? "glyphs enabled" : "glyphs enabled without client load requirement";
    }

    public boolean fallbackChatRendererEnabled() {
        return plugin.getConfig().getBoolean("dialogue-hud.resource-pack.fallback-chat-renderer", true);
    }

    private boolean resourcePackEnabled() {
        return plugin.getConfig().getBoolean("dialogue-hud.resource-pack.enabled", true);
    }

    private static List<String> configuredRequiredFiles(Main plugin) {
        LinkedHashSet<String> files = new LinkedHashSet<>(DEFAULT_REQUIRED_FILES);
        for (String configuredFile : plugin.getConfig().getStringList("dialogue-hud.resource-pack.required-files")) {
            if (configuredFile != null && !configuredFile.isBlank()) {
                files.add(configuredFile.replace('\\', '/').replaceFirst("^/+", ""));
            }
        }
        return List.copyOf(files);
    }

    private void logAvailability() {
        ResourcePackFragmentStatus status = status();
        if (!status.bundledResourceExists()) {
            plugin.getLogger().warning("Bundled dialogue HUD resource-pack folder '" + BUNDLED_FRAGMENT
                    + "' is missing; dialogue glyph HUD will stay unavailable until assets are added.");
        }
        if (!status.glyphUiEnabled()) {
            plugin.getLogger().warning("Dialogue HUD resource-pack glyph UI is unavailable. Action-bar rendering will use plain text.");
            status.requiredFiles().forEach((file, exists) -> {
                if (!exists) {
                    plugin.getLogger().warning("Missing dialogue HUD resource-pack file in " + EXTERNAL_PACK_FOLDER
                            + ": " + file);
                }
            });
        } else {
            plugin.getLogger().info("Dialogue HUD resource-pack files verified at " + status.installedPackPath()
                    + ". Glyphs still require each player to successfully load the resource pack.");
        }
    }
}
