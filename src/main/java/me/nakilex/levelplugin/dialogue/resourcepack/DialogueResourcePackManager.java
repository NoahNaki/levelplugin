package me.nakilex.levelplugin.dialogue.resourcepack;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.resourcepack.ResourcePackFragmentInstaller;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Installs and verifies the Nexo-managed Lux-style dialogue HUD resource-pack fragment. */
public final class DialogueResourcePackManager {
    public static final String PACK_DIRECTORY = "levelplugin-dialogue-hud";
    public static final String NAMESPACE = "levelplugin_dialogue";
    private static final String BUNDLED_FRAGMENT = "resourcepack/levelplugin_dialogue_hud";
    private static final String FALLBACK_MESSAGE = "Could not install dialogue HUD resource-pack fragment into Nexo. Dialogue debug glyphs may render as boxes.";
    private static final String INSTALLED_MESSAGE = "Installed dialogue HUD resource-pack fragment into Nexo external_packs. Regenerate/reload Nexo pack to apply changes.";

    public static final List<String> EXPECTED_ASSET_FILES = List.of(
            "font/dialogue.json",
            "font/offset_chars.json",
            "font/levelplugin_dialogue_default.json",
            "font/levelplugin_dialogue_line_1.json",
            "font/levelplugin_dialogue_line_2.json",
            "font/levelplugin_dialogue_line_3.json",
            "font/levelplugin_dialogue_line_4.json",
            "font/levelplugin_dialogue_line_5.json",
            "font/levelplugin_dialogue_answer_1.json",
            "font/levelplugin_dialogue_answer_2.json",
            "font/levelplugin_dialogue_answer_3.json",
            "font/levelplugin_dialogue_character_name.json",
            "font/levelplugin_dialogue_info.json",
            "textures/dialogue/dialogue.png",
            "textures/dialogue/answer.png",
            "textures/dialogue/character.png",
            "textures/dialogue/hand.png",
            "textures/dialogue/fog.png",
            "textures/dialogue/name_start.png",
            "textures/dialogue/name_mid.png",
            "textures/dialogue/name_end.png"
    );

    private static DialogueResourcePackManager instance;

    private final Main plugin;
    private final Path nexoExternalPacks;
    private final Path installedPack;
    private final Path assetsRoot;

    private DialogueResourcePackManager(Main plugin) {
        this.plugin = plugin;
        Path pluginsDirectory = plugin.getDataFolder().toPath().getParent();
        if (pluginsDirectory == null) pluginsDirectory = Path.of("plugins");
        this.nexoExternalPacks = pluginsDirectory.resolve("Nexo/pack/external_packs");
        this.installedPack = nexoExternalPacks.resolve(PACK_DIRECTORY);
        this.assetsRoot = installedPack.resolve("assets").resolve(NAMESPACE);
    }

    public static DialogueResourcePackManager initialize(Main plugin) {
        DialogueResourcePackManager manager = new DialogueResourcePackManager(plugin);
        instance = manager;
        manager.installBundledFragment();
        manager.logAvailability();
        return manager;
    }

    public static DialogueResourcePackManager getInstance() { return instance; }

    public static Path defaultAssetsRoot() {
        return Path.of("plugins", "Nexo", "pack", "external_packs", PACK_DIRECTORY, "assets", NAMESPACE);
    }

    public Path assetsRoot() { return assetsRoot; }

    public DialoguePackStatus status() {
        boolean externalPacksExists = Files.isDirectory(nexoExternalPacks);
        boolean installedDirectoryExists = Files.isDirectory(installedPack);
        boolean packMetadataExists = Files.isRegularFile(installedPack.resolve("pack.mcmeta"));
        List<String> missingAssets = EXPECTED_ASSET_FILES.stream()
                .filter(relativePath -> !Files.isRegularFile(assetsRoot.resolve(relativePath)))
                .toList();
        boolean enabled = plugin.getConfig().getBoolean("dialogue.resource-pack.enabled", true)
                && Bukkit.getPluginManager().getPlugin("Nexo") != null
                && externalPacksExists
                && installedDirectoryExists
                && packMetadataExists
                && missingAssets.isEmpty();
        return new DialoguePackStatus(externalPacksExists, installedDirectoryExists, packMetadataExists,
                missingAssets, enabled);
    }

    private void installBundledFragment() {
        if (Bukkit.getPluginManager().getPlugin("Nexo") == null) return;
        if (!plugin.getConfig().getBoolean("dialogue.resource-pack.enabled", true)) return;
        try {
            new ResourcePackFragmentInstaller(plugin, BUNDLED_FRAGMENT, installedPack).install();
            plugin.getLogger().info(INSTALLED_MESSAGE);
        } catch (IOException | URISyntaxException exception) {
            plugin.getLogger().warning(FALLBACK_MESSAGE);
            plugin.getLogger().warning("Dialogue pack fragment installation failed: " + exception.getMessage());
        }
    }

    private void logAvailability() {
        if (!plugin.getConfig().getBoolean("dialogue.resource-pack.enabled", true)) return;
        DialoguePackStatus status = status();
        if (!status.glyphUiEnabled()) {
            plugin.getLogger().warning(FALLBACK_MESSAGE);
            if (!status.missingAssets().isEmpty()) {
                plugin.getLogger().warning("Missing dialogue HUD pack assets: " + String.join(", ", status.missingAssets()));
            }
        }
    }

    public record DialoguePackStatus(boolean nexoExternalPacksExists, boolean installed,
                                     boolean packMetadataExists, List<String> missingAssets,
                                     boolean glyphUiEnabled) { }
}
