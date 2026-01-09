package me.nakilex.levelplugin.hud.assets;

public class HudTextureResolver {
    private static final String SINGLE_PREFIX = "image_fantasy_assets_";
    private static final String BAR_PREFIX = "image_image_fantasy_assets_";

    public String resolveSingle(String texturePath) {
        if (texturePath == null || texturePath.isBlank()) {
            return "";
        }
        String base = sanitize(texturePath);
        return SINGLE_PREFIX + base + ".png";
    }

    public String resolveBarFrame(HudImageDefinition definition, int frameIndex) {
        if (definition == null || frameIndex <= 0) {
            return "";
        }
        String base = sanitize(definition.getTexture());
        String splitType = definition.getSplitType().name().toLowerCase();
        int split = Math.max(1, definition.getSplit());
        return BAR_PREFIX + base + "_" + splitType + "_" + split + "_" + frameIndex + ".png";
    }

    private String sanitize(String texturePath) {
        String normalized = texturePath.replace('\\', '/');
        String fileName = normalized;
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0 && slash < normalized.length() - 1) {
            fileName = normalized.substring(slash + 1);
        }
        String trimmed = fileName;
        if (trimmed.toLowerCase().endsWith(".png")) {
            trimmed = trimmed.substring(0, trimmed.length() - 4);
        }
        return trimmed.toLowerCase();
    }
}
