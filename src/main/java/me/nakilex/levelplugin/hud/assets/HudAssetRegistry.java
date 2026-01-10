package me.nakilex.levelplugin.hud.assets;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HudAssetRegistry {
    public static final List<Double> SCALE_BUCKETS = List.of(0.5, 0.75, 0.9, 1.0, 1.25, 1.5, 2.0);
    private final Map<String, HudImageDefinition> definitions = new HashMap<>();
    private final Map<String, HudGlyph> glyphs = new HashMap<>();
    private final Map<String, List<HudGlyph>> barFrames = new HashMap<>();
    private final Map<Character, Integer> glyphWidths = new HashMap<>();
    private final Map<String, AssetBaseInfo> baseInfo = new HashMap<>();
    private final Map<String, VariantTexture> variantTextures = new HashMap<>();

    public void registerDefinition(HudImageDefinition definition) {
        if (definition == null || definition.getId() == null) {
            return;
        }
        definitions.put(definition.getId().toLowerCase(), definition);
    }

    public Map<String, HudImageDefinition> getDefinitions() {
        return Collections.unmodifiableMap(definitions);
    }

    public Map<String, HudGlyph> getAllGlyphs() {
        return Collections.unmodifiableMap(glyphs);
    }

    public Map<String, List<HudGlyph>> getAllBarFrames() {
        return Collections.unmodifiableMap(barFrames);
    }

    public void registerGlyph(String id, HudGlyph glyph) {
        registerGlyphVariant(id, 1.0, glyph, glyph == null ? null : glyph.texturePath());
    }

    public void registerBarFrames(String id, List<HudGlyph> frames) {
        registerBarFramesVariant(id, 1.0, frames, frames);
    }

    public void registerGlyphVariant(String id, double scale, HudGlyph glyph, String baseTexturePath) {
        if (id == null || glyph == null) {
            return;
        }
        String key = variantKey(id, scale);
        glyphs.put(key, glyph);
        registerGlyphWidth(glyph);
        registerBaseInfoIfMissing(id, glyph);
        registerVariantTexture(glyph.texturePath(), baseTexturePath, scale);
    }

    public void registerBarFramesVariant(String id, double scale, List<HudGlyph> frames, List<HudGlyph> baseFrames) {
        if (id == null || frames == null) {
            return;
        }
        String key = variantKey(id, scale);
        barFrames.put(key, frames);
        for (HudGlyph glyph : frames) {
            registerGlyphWidth(glyph);
        }
        if (baseFrames != null && !baseFrames.isEmpty()) {
            registerBaseInfoIfMissing(id, baseFrames.get(0));
        }
        if (scale != 1.0 && baseFrames != null) {
            int limit = Math.min(frames.size(), baseFrames.size());
            for (int i = 0; i < limit; i++) {
                HudGlyph variant = frames.get(i);
                HudGlyph base = baseFrames.get(i);
                registerVariantTexture(variant.texturePath(), base.texturePath(), scale);
            }
        }
    }

    public HudGlyph getGlyph(String id) {
        return getGlyph(id, 1.0);
    }

    public HudGlyph getGlyph(String id, double scale) {
        if (id == null) {
            return null;
        }
        return glyphs.get(variantKey(id, scale));
    }

    public List<HudGlyph> getBarFrames(String id) {
        return getBarFrames(id, 1.0);
    }

    public List<HudGlyph> getBarFrames(String id, double scale) {
        if (id == null) {
            return List.of();
        }
        return barFrames.getOrDefault(variantKey(id, scale), List.of());
    }

    public int getAssetWidth(String id) {
        return getAssetWidth(id, 1.0);
    }

    public int getAssetWidth(String id, double scale) {
        HudGlyph glyph = getGlyph(id, scale);
        if (glyph != null && glyph.width() > 0) {
            return glyph.width();
        }
        List<HudGlyph> frames = getBarFrames(id, scale);
        if (!frames.isEmpty() && frames.get(0).width() > 0) {
            return frames.get(0).width();
        }
        return 0;
    }

    public int getAssetHeight(String id) {
        return getAssetHeight(id, 1.0);
    }

    public int getAssetHeight(String id, double scale) {
        HudGlyph glyph = getGlyph(id, scale);
        if (glyph != null && glyph.height() > 0) {
            return glyph.height();
        }
        List<HudGlyph> frames = getBarFrames(id, scale);
        if (!frames.isEmpty() && frames.get(0).height() > 0) {
            return frames.get(0).height();
        }
        return 0;
    }

    public int getBaseAssetWidth(String id) {
        AssetBaseInfo info = baseInfo.get(normalizedId(id));
        return info == null ? 0 : info.width();
    }

    public int getBaseAssetHeight(String id) {
        AssetBaseInfo info = baseInfo.get(normalizedId(id));
        return info == null ? 0 : info.height();
    }

    public Map<Character, Integer> getGlyphWidths() {
        return Collections.unmodifiableMap(glyphWidths);
    }

    public Map<String, VariantTexture> getVariantTextures() {
        return Collections.unmodifiableMap(variantTextures);
    }

    public static double bucketScale(double requested) {
        double resolved = 1.0;
        double bestDelta = Double.MAX_VALUE;
        for (double bucket : SCALE_BUCKETS) {
            double delta = Math.abs(bucket - requested);
            if (delta < bestDelta) {
                bestDelta = delta;
                resolved = bucket;
            }
        }
        return resolved;
    }

    public String formatScale(double scale) {
        java.text.DecimalFormat format = new java.text.DecimalFormat("0.##");
        format.setDecimalSeparatorAlwaysShown(false);
        String value = format.format(scale);
        if (!value.contains(".")) {
            value = value + ".0";
        }
        return value;
    }

    private String variantKey(String id, double scale) {
        return normalizedId(id) + "@" + formatScale(scale);
    }

    private String normalizedId(String id) {
        return id == null ? "" : id.toLowerCase();
    }

    private void registerBaseInfoIfMissing(String id, HudGlyph glyph) {
        if (id == null || glyph == null) {
            return;
        }
        baseInfo.putIfAbsent(normalizedId(id), new AssetBaseInfo(glyph.width(), glyph.height(), glyph.texturePath()));
    }

    private void registerVariantTexture(String variantTexture, String baseTexture, double scale) {
        if (variantTexture == null || baseTexture == null || variantTexture.equals(baseTexture)) {
            return;
        }
        variantTextures.put(variantTexture, new VariantTexture(baseTexture, scale));
    }

    private void registerGlyphWidth(HudGlyph glyph) {
        if (glyph == null) {
            return;
        }
        int width = Math.max(0, glyph.width());
        if (width > 0) {
            glyphWidths.put(glyph.codepoint(), width);
        }
    }

    public record VariantTexture(String baseTexturePath, double scale) {
    }

    private record AssetBaseInfo(int width, int height, String baseTexturePath) {
    }
}
