package me.nakilex.levelplugin.hud.assets;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HudAssetRegistry {
    private final Map<String, HudImageDefinition> definitions = new HashMap<>();
    private final Map<String, HudGlyph> baseGlyphs = new HashMap<>();
    private final Map<String, List<HudGlyph>> baseBarFrames = new HashMap<>();
    private final Map<String, Map<String, HudGlyph>> scaledGlyphs = new HashMap<>();
    private final Map<String, Map<String, List<HudGlyph>>> scaledBarFrames = new HashMap<>();
    private final Map<Character, Integer> glyphWidths = new HashMap<>();

    public void registerDefinition(HudImageDefinition definition) {
        if (definition == null || definition.getId() == null) {
            return;
        }
        definitions.put(definition.getId().toLowerCase(), definition);
    }

    public Map<String, HudImageDefinition> getDefinitions() {
        return Collections.unmodifiableMap(definitions);
    }

    public void registerBaseGlyph(String id, HudGlyph glyph) {
        if (id == null || glyph == null) {
            return;
        }
        baseGlyphs.put(id.toLowerCase(), glyph);
    }

    public void registerBaseBarFrames(String id, List<HudGlyph> frames) {
        if (id == null || frames == null) {
            return;
        }
        baseBarFrames.put(id.toLowerCase(), List.copyOf(frames));
    }

    public void registerGlyph(String id, double scale, HudGlyph glyph) {
        if (id == null || glyph == null) {
            return;
        }
        String normalized = id.toLowerCase();
        String scaleKey = normalizeScaleKey(scale);
        scaledGlyphs.computeIfAbsent(normalized, key -> new HashMap<>()).put(scaleKey, glyph);
        registerGlyphWidth(glyph);
    }

    public void registerBarFrames(String id, double scale, List<HudGlyph> frames) {
        if (id == null || frames == null) {
            return;
        }
        String normalized = id.toLowerCase();
        String scaleKey = normalizeScaleKey(scale);
        scaledBarFrames.computeIfAbsent(normalized, key -> new HashMap<>()).put(scaleKey, List.copyOf(frames));
        for (HudGlyph glyph : frames) {
            registerGlyphWidth(glyph);
        }
    }

    public HudGlyph getBaseGlyph(String id) {
        if (id == null) {
            return null;
        }
        return baseGlyphs.get(id.toLowerCase());
    }

    public List<HudGlyph> getBaseBarFrames(String id) {
        if (id == null) {
            return List.of();
        }
        return baseBarFrames.getOrDefault(id.toLowerCase(), List.of());
    }

    public HudGlyph getGlyph(String id, double scale) {
        if (id == null) {
            return null;
        }
        String normalized = id.toLowerCase();
        String scaleKey = normalizeScaleKey(scale);
        HudGlyph glyph = scaledGlyphs.getOrDefault(normalized, Map.of()).get(scaleKey);
        if (glyph != null) {
            return glyph;
        }
        return baseGlyphs.get(normalized);
    }

    public List<HudGlyph> getBarFrames(String id, double scale) {
        if (id == null) {
            return List.of();
        }
        String normalized = id.toLowerCase();
        String scaleKey = normalizeScaleKey(scale);
        List<HudGlyph> frames = scaledBarFrames.getOrDefault(normalized, Map.of()).get(scaleKey);
        if (frames != null) {
            return frames;
        }
        return baseBarFrames.getOrDefault(normalized, List.of());
    }

    public Map<String, HudGlyph> getGlyphVariants(String id) {
        if (id == null) {
            return Map.of();
        }
        return Collections.unmodifiableMap(scaledGlyphs.getOrDefault(id.toLowerCase(), Map.of()));
    }

    public Map<String, List<HudGlyph>> getBarFrameVariants(String id) {
        if (id == null) {
            return Map.of();
        }
        return Collections.unmodifiableMap(scaledBarFrames.getOrDefault(id.toLowerCase(), Map.of()));
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
        HudGlyph glyph = getBaseGlyph(id);
        if (glyph != null && glyph.width() > 0) {
            return glyph.width();
        }
        List<HudGlyph> frames = getBaseBarFrames(id);
        if (!frames.isEmpty() && frames.get(0).width() > 0) {
            return frames.get(0).width();
        }
        return 0;
    }

    public int getBaseAssetHeight(String id) {
        HudGlyph glyph = getBaseGlyph(id);
        if (glyph != null && glyph.height() > 0) {
            return glyph.height();
        }
        List<HudGlyph> frames = getBaseBarFrames(id);
        if (!frames.isEmpty() && frames.get(0).height() > 0) {
            return frames.get(0).height();
        }
        return 0;
    }

    public Map<Character, Integer> getGlyphWidths() {
        return Collections.unmodifiableMap(glyphWidths);
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

    private String normalizeScaleKey(double scale) {
        double normalized = scale <= 0 ? 1.0 : scale;
        return Double.toString(normalized);
    }
}
