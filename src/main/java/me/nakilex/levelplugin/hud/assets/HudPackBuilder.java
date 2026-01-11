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
        String advanceProvider = buildAdvanceProviderJson();
        if (!advanceProvider.isBlank()) {
            builder.append("    ").append(advanceProvider);
            first = false;
        }
        for (Map.Entry<String, HudImageDefinition> entry : registry.getDefinitions().entrySet()) {
            String id = entry.getKey();
            HudImageDefinition definition = entry.getValue();
            if (definition.getType() == HudImageType.LISTENER) {
                Map<String, List<HudGlyph>> frameVariants = registry.getBarFrameVariants(id);
                for (List<HudGlyph> frames : frameVariants.values()) {
                    for (int index = 0; index < frames.size(); index++) {
                        HudGlyph glyph = frames.get(index);
                        String texture = glyph.texturePath();
                        if (!first) {
                            builder.append(",\n");
                        }
                        first = false;
                        builder.append("    ").append(providerJson(namespace, texture, glyph));
                    }
                }
                continue;
            }
            Map<String, HudGlyph> variants = registry.getGlyphVariants(id);
            for (HudGlyph glyph : variants.values()) {
                if (glyph == null) {
                    continue;
                }
                if (!first) {
                    builder.append(",\n");
                }
                first = false;
                builder.append("    ").append(providerJson(namespace, glyph.texturePath(), glyph));
            }
        }
        builder.append("\n  ]\n}");
        return builder.toString();
    }

    private String providerJson(String namespace, String texture, HudGlyph glyph) {
        int height = Math.max(1, glyph.height());
        int ascent = Math.max(1, height - 1);
        return "{"
                + "\"type\":\"bitmap\","
                + "\"file\":\"" + namespace + ":" + texture + "\","
                + "\"ascent\":" + ascent + ","
                + "\"height\":" + height + ","
                + "\"chars\":[\"" + glyph.codepoint() + "\"]"
                + "}";
    }

    private String buildAdvanceProviderJson() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"type\":\"space\",\"advances\":{");
        boolean first = true;
        for (Map.Entry<Character, Integer> entry : HudAdvanceGlyphs.buildAdvanceMap().entrySet()) {
            if (!first) {
                builder.append(",");
            }
            first = false;
            builder.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
        }
        builder.append("}}");
        return builder.toString();
    }

    public List<String> collectMissingTextures(String sourceTexturesFolder, HudAssetRegistry registry) {
        if (sourceTexturesFolder == null || registry == null) {
            return List.of();
        }
        List<String> missing = new java.util.ArrayList<>();
        java.nio.file.Path textureRoot = java.nio.file.Paths.get(sourceTexturesFolder).toAbsolutePath().normalize();
        for (HudImageDefinition definition : registry.getDefinitions().values()) {
            if (definition.getType() == HudImageType.LISTENER) {
                List<HudGlyph> frames = registry.getBaseBarFrames(definition.getId());
                for (HudGlyph frame : frames) {
                    if (!textureExists(textureRoot, frame.texturePath())) {
                        missing.add(frame.texturePath());
                    }
                }
                continue;
            }
            HudGlyph glyph = registry.getBaseGlyph(definition.getId());
            if (glyph != null && !textureExists(textureRoot, glyph.texturePath())) {
                missing.add(glyph.texturePath());
            }
        }
        return missing;
    }

    private boolean textureExists(java.nio.file.Path textureRoot, String texturePath) {
        if (texturePath == null || texturePath.isBlank()) {
            return false;
        }
        java.nio.file.Path file = textureRoot.resolve(texturePath).normalize();
        return java.nio.file.Files.exists(file);
    }
}
