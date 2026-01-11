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
        if (base.isBlank()) {
            return "";
        }
        if (base.toLowerCase().endsWith(".png")) {
            base = base.substring(0, base.length() - 4);
        }
        String splitType = definition.getSplitType().name().toLowerCase();
        int split = Math.max(1, definition.getSplit());
        return base + "_" + splitType + "_" + split + "_" + frameIndex + ".png";
    }

    private String normalizePath(String texturePath) {
        String normalized = texturePath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.toLowerCase().startsWith("textures/")) {
            normalized = normalized.substring("textures/".length());
        }
        if (!normalized.toLowerCase().endsWith(".png")) {
            normalized = normalized + ".png";
        }
        return normalized;
    }
}
