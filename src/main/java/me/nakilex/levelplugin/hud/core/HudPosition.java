package me.nakilex.levelplugin.hud.core;

public record HudPosition(HudAnchor anchor, double guiX, double guiY, int pixelX, int pixelY, int row) {
    public HudPosition {
        if (anchor == null) {
            anchor = HudAnchor.TOP_LEFT;
        }
    }

    public static HudPosition defaultPosition() {
        return new HudPosition(HudAnchor.TOP_LEFT, 0.0, 0.0, 0, 0, 0);
    }
}
