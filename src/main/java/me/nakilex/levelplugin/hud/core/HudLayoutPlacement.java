package me.nakilex.levelplugin.hud.core;

public class HudLayoutPlacement {
    private final String layoutId;
    private final HudPosition position;
    private final HudTextAlign align;

    public HudLayoutPlacement(String layoutId, HudPosition position, HudTextAlign align) {
        this.layoutId = layoutId;
        this.position = position == null ? HudPosition.defaultPosition() : position;
        this.align = align == null ? HudTextAlign.LEFT : align;
    }

    public String getLayoutId() {
        return layoutId;
    }

    public HudPosition getPosition() {
        return position;
    }

    public HudTextAlign getAlign() {
        return align;
    }
}
