package me.nakilex.levelplugin.hud.core;

public final class HudPositionResolver {
    private HudPositionResolver() {
    }

    public static ResolvedPosition resolve(HudPosition position, int widthPx, int heightPx) {
        HudPosition pos = position == null ? HudPosition.defaultPosition() : position;
        ResolvedPosition base = resolveAnchorBase(pos.anchor(), widthPx, heightPx);
        int x = base.x() + pos.pixelX();
        int y = base.y() + pos.pixelY();
        return new ResolvedPosition(x, y);
    }

    public static ResolvedPosition resolveAnchorBase(HudAnchor anchor, int widthPx, int heightPx) {
        HudAnchor resolved = anchor == null ? HudAnchor.TOP_LEFT : anchor;
        int baseX = switch (resolved) {
            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> 0;
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> widthPx / 2;
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> widthPx;
        };
        int baseY = switch (resolved) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> 0;
            case CENTER_LEFT, CENTER, CENTER_RIGHT -> heightPx / 2;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> heightPx;
        };
        return new ResolvedPosition(baseX, baseY);
    }

    public record ResolvedPosition(int x, int y) {
    }
}
