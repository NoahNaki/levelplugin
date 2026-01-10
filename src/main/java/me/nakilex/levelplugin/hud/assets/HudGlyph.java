package me.nakilex.levelplugin.hud.assets;

public record HudGlyph(char codepoint, String texturePath, int width, int height) {
    public HudGlyph(char codepoint, String texturePath) {
        this(codepoint, texturePath, 0, 0);
    }
}
