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

    public void build(String outputFolder, String namespace, HudAssetRegistry registry,
                      java.nio.file.Path sourceTextureRoot) {
        if (outputFolder == null || namespace == null || registry == null) {
            return;
        }
        File base = new File(outputFolder, "assets/" + namespace + "/font");
        if (!base.exists() && !base.mkdirs()) {
            logger.warning("Failed to create HUD font output directory: " + base.getAbsolutePath());
            return;
        }
        java.nio.file.Path textureRoot = java.nio.file.Paths.get(outputFolder)
                .resolve("assets")
                .resolve(namespace)
                .resolve("textures");
        generateScaledTextures(textureRoot, registry, sourceTextureRoot);
        File fontFile = new File(base, "hud_generated.json");
        String json = buildFontJson(namespace, registry, textureRoot);
        if (json.isBlank()) {
            logger.warning("HUD font build aborted due to missing textures.");
            return;
        }
        try (FileWriter writer = new FileWriter(fontFile)) {
            writer.write(json);
        } catch (IOException ex) {
            logger.warning("Failed to write HUD font JSON: " + ex.getMessage());
        }
    }

    private String buildFontJson(String namespace, HudAssetRegistry registry, java.nio.file.Path textureRoot) {
        if (textureRoot == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("{\n  \"providers\": [\n");
        boolean first = true;
        String advanceProvider = buildAdvanceProviderJson();
        if (!advanceProvider.isBlank()) {
            builder.append("    ").append(advanceProvider);
            first = false;
        }
        for (List<HudGlyph> frames : registry.getAllBarFrames().values()) {
            for (HudGlyph glyph : frames) {
                String texture = glyph.texturePath();
                if (!validateTexture(textureRoot, texture)) {
                    return "";
                }
                if (!first) {
                    builder.append(",\n");
                }
                first = false;
                builder.append("    ").append(providerJson(namespace, texture, glyph));
            }
        }
        for (HudGlyph glyph : registry.getAllGlyphs().values()) {
            if (glyph == null) {
                continue;
            }
            if (!validateTexture(textureRoot, glyph.texturePath())) {
                return "";
            }
            if (!first) {
                builder.append(",\n");
            }
            first = false;
            builder.append("    ").append(providerJson(namespace, glyph.texturePath(), glyph));
        }
        builder.append("\n  ]\n}");
        return builder.toString();
    }

    private String providerJson(String namespace, String texture, HudGlyph glyph) {
        int height = Math.max(8, glyph.height());
        int ascent = Math.max(1, height - 1);
        return "{"
                + "\"type\":\"bitmap\","
                + "\"file\":\"" + namespace + ":textures/" + texture + "\","
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

    private void generateScaledTextures(java.nio.file.Path textureRoot,
                                        HudAssetRegistry registry,
                                        java.nio.file.Path sourceTextureRoot) {
        if (textureRoot == null || registry == null || sourceTextureRoot == null) {
            return;
        }
        for (Map.Entry<String, HudAssetRegistry.VariantTexture> entry : registry.getVariantTextures().entrySet()) {
            String variantTexture = entry.getKey();
            HudAssetRegistry.VariantTexture info = entry.getValue();
            if (variantTexture == null || info == null) {
                continue;
            }
            java.nio.file.Path basePath = sourceTextureRoot.resolve(info.baseTexturePath()).normalize();
            java.nio.file.Path outputPath = textureRoot.resolve(variantTexture).normalize();
            if (java.nio.file.Files.exists(outputPath)) {
                continue;
            }
            try {
                java.nio.file.Files.createDirectories(outputPath.getParent());
                java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(basePath.toFile());
                if (image == null) {
                    continue;
                }
                java.awt.image.BufferedImage scaled = scaleImage(image, info.scale());
                javax.imageio.ImageIO.write(scaled, "png", outputPath.toFile());
            } catch (IOException ex) {
                logger.warning("Failed to generate HUD scaled texture '" + variantTexture + "': " + ex.getMessage());
            }
        }
    }

    private boolean validateTexture(java.nio.file.Path textureRoot, String texturePath) {
        if (texturePath == null || texturePath.isBlank()) {
            logger.warning("HUD font entry has blank texture path.");
            return false;
        }
        java.nio.file.Path file = textureRoot.resolve(texturePath).normalize();
        if (!java.nio.file.Files.exists(file)) {
            logger.warning("Missing HUD texture for font provider: " + file);
            return false;
        }
        return true;
    }

    private java.awt.image.BufferedImage scaleImage(java.awt.image.BufferedImage source, double scale) {
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        java.awt.image.BufferedImage scaled = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return scaled;
    }

    public List<String> collectMissingTextures(String sourceTexturesFolder, HudAssetRegistry registry) {
        if (sourceTexturesFolder == null || registry == null) {
            return List.of();
        }
        List<String> missing = new java.util.ArrayList<>();
        java.nio.file.Path textureRoot = java.nio.file.Paths.get(sourceTexturesFolder).toAbsolutePath().normalize();
        for (HudImageDefinition definition : registry.getDefinitions().values()) {
            if (definition.getType() == HudImageType.LISTENER) {
                List<HudGlyph> frames = registry.getBarFrames(definition.getId());
                for (HudGlyph frame : frames) {
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

    public List<String> collectMissingFontTextures(java.nio.file.Path outputFolder, String namespace) {
        if (outputFolder == null || namespace == null || namespace.isBlank()) {
            return List.of("Invalid output folder or namespace for HUD font validation.");
        }
        java.nio.file.Path fontFile = outputFolder.resolve("assets")
                .resolve(namespace)
                .resolve("font")
                .resolve("hud_generated.json");
        if (!java.nio.file.Files.exists(fontFile)) {
            return List.of("Missing HUD font file: " + fontFile);
        }
        List<String> missing = new java.util.ArrayList<>();
        java.nio.file.Path namespaceRoot = outputFolder.resolve("assets").resolve(namespace);
        try {
            String content = java.nio.file.Files.readString(fontFile);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"file\"\\s*:\\s*\"([^\"]+)\"")
                    .matcher(content);
            while (matcher.find()) {
                String fileToken = matcher.group(1);
                String relative = parseFontFileRelativePath(namespace, fileToken);
                if (relative == null || relative.isBlank()) {
                    missing.add("Invalid font file reference: " + fileToken);
                    continue;
                }
                java.nio.file.Path texturePath = namespaceRoot.resolve(relative).normalize();
                if (!java.nio.file.Files.exists(texturePath)) {
                    missing.add(texturePath.toString());
                }
            }
        } catch (IOException ex) {
            missing.add("Failed to read HUD font file: " + ex.getMessage());
        }
        return missing;
    }

    private String parseFontFileRelativePath(String namespace, String fileToken) {
        if (fileToken == null || fileToken.isBlank()) {
            return null;
        }
        String prefix = namespace + ":";
        String resolved = fileToken.startsWith(prefix) ? fileToken.substring(prefix.length()) : fileToken;
        while (resolved.startsWith("/")) {
            resolved = resolved.substring(1);
        }
        if (!resolved.startsWith("textures/")) {
            return null;
        }
        return resolved;
    }

    private boolean textureExists(java.nio.file.Path textureRoot, String texturePath) {
        if (texturePath == null || texturePath.isBlank()) {
            return false;
        }
        java.nio.file.Path file = textureRoot.resolve(texturePath).normalize();
        return java.nio.file.Files.exists(file);
    }
}
