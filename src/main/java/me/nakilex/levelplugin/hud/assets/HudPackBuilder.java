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
}
