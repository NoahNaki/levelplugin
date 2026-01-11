package me.nakilex.levelplugin.hud.assets;

import java.util.ArrayList;
import java.util.List;

public final class HudTextureCandidates {
    private HudTextureCandidates() {
    }

    public static List<String> buildCandidates(String texturePath) {
        if (texturePath == null || texturePath.isBlank()) {
            return List.of();
        }
        String normalized = texturePath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("textures/")) {
            normalized = normalized.substring("textures/".length());
        }
        String base = normalized;
        String noExt = base.toLowerCase().endsWith(".png")
                ? base.substring(0, base.length() - 4)
                : base;
        String flattened = noExt.replace("/", "_");
        List<String> candidates = new ArrayList<>();
        candidates.add(base);
        candidates.add("image_fantasy_assets_" + flattened + ".png");
        candidates.add("image_image_fantasy_assets_" + flattened + ".png");
        if (flattened.indexOf('/') < 0) {
            candidates.add("fantasy/assets/" + base);
        }
        return candidates;
    }
}
