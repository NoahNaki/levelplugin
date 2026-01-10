package me.nakilex.levelplugin.hud.core;

public class HudLayoutPlacement {
    private final String layoutId;
    private final HudPosition position;

    public HudLayoutPlacement(String layoutId, HudPosition position) {
        this.layoutId = layoutId;
        this.position = position == null ? HudPosition.defaultPosition() : position;
    }

    public String getLayoutId() {
        return layoutId;
    }

    public HudPosition getPosition() {
        return position;
    }
}
