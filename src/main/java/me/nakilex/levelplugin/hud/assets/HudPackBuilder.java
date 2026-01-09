package me.nakilex.levelplugin.hud.assets;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class HudPackBuilder {
    private final Plugin plugin;
    private final Logger logger;

    public HudPackBuilder(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void build(String outputFolder, String namespace, HudAssetRegistry registry) {
        if (outputFolder == null || namespace == null || registry == null) {
            return;
        }
        logMissingTextures(outputFolder, namespace, registry);
        File base = new File(outputFolder, "assets/" + namespace + "/font");
        if (!base.exists() && !base.mkdirs()) {
            logger.warning("Failed to create HUD font output directory: " + base.getAbsolutePath());
            return;
        }
        File fontFile = new File(base, "hud_generated.json");
        String json = buildFontJson(namespace, registry);
        try (FileWriter writer = new FileWriter(fontFile)) {
            writer.write(json);
        } catch (IOException ex) {
            logger.warning("Failed to write HUD font JSON: " + ex.getMessage());
        }
    }

    private String buildFontJson(String namespace, HudAssetRegistry registry) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n  \"providers\": [\n");
        boolean first = true;
        for (Map.Entry<String, HudImageDefinition> entry : registry.getDefinitions().entrySet()) {
            String id = entry.getKey();
            HudImageDefinition definition = entry.getValue();
            if (definition.getType() == HudImageType.LISTENER) {
                List<HudGlyph> frames = registry.getBarFrames(id);
                for (int index = 0; index < frames.size(); index++) {
                    HudGlyph glyph = frames.get(index);
                    String texture = glyph.texturePath();
                    if (!first) {
                        builder.append(",\n");
                    }
                    first = false;
                    builder.append("    ").append(providerJson(namespace, texture, glyph.codepoint()));
                }
                continue;
            }
            HudGlyph glyph = registry.getGlyph(id);
            if (glyph == null) {
                continue;
            }
            if (!first) {
                builder.append(",\n");
            }
            first = false;
            builder.append("    ").append(providerJson(namespace, glyph.texturePath(), glyph.codepoint()));
        }
        builder.append("\n  ]\n}");
        return builder.toString();
    }

    private String providerJson(String namespace, String texture, char codepoint) {
        String texturePath = "textures/" + texture;
        return "{"
                + "\"type\":\"bitmap\","
                + "\"file\":\"" + namespace + ":" + texturePath + "\","
                + "\"ascent\":8,"
                + "\"height\":8,"
                + "\"chars\":[\"" + codepoint + "\"]"
                + "}";
    }

    public List<String> collectMissingTextures(String outputFolder, String namespace, HudAssetRegistry registry) {
        if (outputFolder == null || namespace == null || registry == null) {
            return List.of();
        }
        List<String> missing = new java.util.ArrayList<>();
        File textureRoot = new File(outputFolder, "assets/" + namespace + "/textures");
        for (HudImageDefinition definition : registry.getDefinitions().values()) {
            if (definition.getType() == HudImageType.LISTENER) {
                for (HudGlyph frame : registry.getBarFrames(definition.getId())) {
                    if (!textureExists(textureRoot, frame.texturePath())) {
                        missing.add(frame.texturePath());
                    }
                }
                continue;
            }
            HudGlyph glyph = registry.getGlyph(definition.getId());
            if (glyph != null && !textureExists(textureRoot, glyph.texturePath())) {
                missing.add(glyph.texturePath());
            }
        }
        return missing;
    }

    private void logMissingTextures(String outputFolder, String namespace, HudAssetRegistry registry) {
        List<String> missing = collectMissingTextures(outputFolder, namespace, registry);
        if (missing.isEmpty()) {
            return;
        }
        logger.warning("HUD textures missing from pack (" + missing.size() + "). Example: " + missing.get(0));
    }

    private boolean textureExists(File textureRoot, String texturePath) {
        if (texturePath == null || texturePath.isBlank()) {
            return false;
        }
        File file = new File(textureRoot, texturePath);
        return file.exists();
    }
}
