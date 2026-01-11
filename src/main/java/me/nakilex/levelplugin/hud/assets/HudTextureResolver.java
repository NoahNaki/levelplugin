package me.nakilex.levelplugin.hud.assets;

public class HudTextureResolver {
    public String resolveSingle(String texturePath) {
        if (texturePath == null || texturePath.isBlank()) {
            return "";
        }
        return normalizePath(texturePath);
    }

    public String resolveBarFrame(HudImageDefinition definition, int frameIndex) {
        if (definition == null || frameIndex <= 0) {
            return "";
        }
        String base = normalizePath(definition.getTexture());
        if (base.isEmpty()) {
            return "";
        }
        base = base.substring(0, base.length() - 4);
        String splitType = definition.getSplitType().name().toLowerCase();
        int split = Math.max(1, definition.getSplit());
        return base + "_" + splitType + "_" + split + "_" + frameIndex + ".png";
    }

    private String normalizePath(String texturePath) {
        String normalized = texturePath.replace('\\', '/');
        String trimmed = normalized.trim();
        if (trimmed.toLowerCase().startsWith("textures/")) {
            trimmed = trimmed.substring("textures/".length());
        }
        if (!trimmed.toLowerCase().endsWith(".png")) {
            trimmed = trimmed + ".png";
        }
        return trimmed;
    }
}
