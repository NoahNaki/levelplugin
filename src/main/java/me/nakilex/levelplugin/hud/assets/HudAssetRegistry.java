package me.nakilex.levelplugin.hud.assets;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HudAssetRegistry {
    private final Map<String, HudImageDefinition> definitions = new HashMap<>();
    private final Map<String, HudGlyph> glyphs = new HashMap<>();
    private final Map<String, List<HudGlyph>> barFrames = new HashMap<>();
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

    public void registerGlyph(String id, HudGlyph glyph) {
        if (id == null || glyph == null) {
            return;
        }
        glyphs.put(id.toLowerCase(), glyph);
        registerGlyphWidth(glyph);
    }

    public void registerBarFrames(String id, List<HudGlyph> frames) {
        if (id == null || frames == null) {
            return;
        }
        barFrames.put(id.toLowerCase(), frames);
        for (HudGlyph glyph : frames) {
            registerGlyphWidth(glyph);
        }
    }

    public HudGlyph getGlyph(String id) {
        if (id == null) {
            return null;
        }
        return glyphs.get(id.toLowerCase());
    }

    public List<HudGlyph> getBarFrames(String id) {
        if (id == null) {
            return List.of();
        }
        return barFrames.getOrDefault(id.toLowerCase(), List.of());
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
}
