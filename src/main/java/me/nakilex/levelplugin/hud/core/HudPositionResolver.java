package me.nakilex.levelplugin.hud.core;

public final class HudPositionResolver {
    private HudPositionResolver() {
    }

    public static ResolvedPosition resolve(HudPosition position, int widthPx, int heightPx) {
        HudPosition pos = position == null ? HudPosition.defaultPosition() : position;
        int baseX = switch (pos.anchor()) {
            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> 0;
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> widthPx / 2;
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> widthPx;
        };
        int baseY = switch (pos.anchor()) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> 0;
            case CENTER_LEFT, CENTER, CENTER_RIGHT -> heightPx / 2;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> heightPx;
        };
        int guiX = (int) Math.round(widthPx * (pos.guiX() / 100.0));
        int guiY = (int) Math.round(heightPx * (pos.guiY() / 100.0));
        int x = baseX + guiX + pos.pixelX();
        int y = baseY + guiY + pos.pixelY();
        return new ResolvedPosition(x, y);
    }

    public record ResolvedPosition(int x, int y) {
    }
}
