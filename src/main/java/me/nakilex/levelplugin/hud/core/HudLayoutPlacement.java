package me.nakilex.levelplugin.hud.core;

public class HudLayoutPlacement {
    private final String layoutId;
    private final int offsetX;
    private final int offsetY;

    public HudLayoutPlacement(String layoutId, int offsetX, int offsetY) {
        this.layoutId = layoutId;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public String getLayoutId() {
        return layoutId;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }
}
