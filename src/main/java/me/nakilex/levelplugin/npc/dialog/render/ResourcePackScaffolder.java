package me.nakilex.levelplugin.npc.dialog.render;

import me.nakilex.levelplugin.Main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Level;

/** Creates the editable resource-pack layout without generating or depending on LuxDialogues assets. */
public final class ResourcePackScaffolder {
    private static final String PACK_META = "{\"pack\":{\"pack_format\":42,\"description\":\"LevelPlugin dialogue HUD scaffold\"}}\n";
    private static final String EMPTY_JSON = "{}\n";
    /** Files intentionally supplied by resource-pack authors; scaffolding creates their parent folders only. */
    public static final List<String> EXPECTED_ASSETS = List.of(
            "pack.png",
            "assets/levelplugin/textures/gui/dialogue/dialogue.png",
            "assets/levelplugin/textures/gui/dialogue/answer.png",
            "assets/levelplugin/textures/gui/dialogue/hand.png",
            "assets/levelplugin/textures/gui/dialogue/fog.png",
            "assets/levelplugin/textures/gui/dialogue/character.png",
            "assets/levelplugin/textures/gui/dialogue/name_start.png",
            "assets/levelplugin/textures/gui/dialogue/name_mid.png",
            "assets/levelplugin/textures/gui/dialogue/name_end.png",
            "assets/levelplugin/textures/gui/dialogue/portraits/default.png",
            "assets/levelplugin/sounds/dialogue/typing.ogg",
            "assets/levelplugin/sounds/dialogue/selection.ogg",
            "assets/levelplugin/sounds/dialogue/confirm.ogg",
            "assets/levelplugin/sounds/dialogue/ding.ogg");
    private final Main plugin;

    public ResourcePackScaffolder(Main plugin) {
        this.plugin = plugin;
    }

    public void ensureDirectories() {
        File root = new File(plugin.getDataFolder(), "resourcepack");
        List<String> directories = List.of(
                "assets/levelplugin/font",
                "assets/levelplugin/textures/gui/dialogue/portraits",
                "assets/levelplugin/sounds/dialogue");
        for (String path : directories) ensureDirectory(new File(root, path));
        for (String path : EXPECTED_ASSETS) {
            File parent = new File(root, path).getParentFile();
            if (parent != null) ensureDirectory(parent);
        }
        writeIfMissing(new File(root, "pack.mcmeta"), PACK_META);
        writeIfMissing(new File(root, "assets/levelplugin/font/dialogue.json"), EMPTY_JSON);
        writeIfMissing(new File(root, "assets/levelplugin/sounds.json"), EMPTY_JSON);
    }

    private void ensureDirectory(File directory) {
        if (!directory.exists() && !directory.mkdirs()) plugin.getLogger().warning("Could not create resource-pack directory: " + directory);
    }

    private void writeIfMissing(File file, String content) {
        if (file.exists()) return;
        ensureDirectory(file.getParentFile());
        try {
            Files.writeString(file.toPath(), content);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not create resource-pack scaffold file " + file, exception);
        }
    }
}
