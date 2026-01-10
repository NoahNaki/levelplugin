package me.nakilex.levelplugin.hud.core;

public class HudResolvedElement {
    private final String id;
    private final String text;
    private final int x;
    private final int y;
    private final int row;
    private final int layer;
    private final double scale;
    private final HudTextAlign align;

    public HudResolvedElement(String id,
                              String text,
                              int x,
                              int y,
                              int row,
                              int layer,
                              double scale,
                              HudTextAlign align) {
        this.id = id;
        this.text = text;
        this.x = x;
        this.y = y;
        this.row = row;
        this.layer = layer;
        this.scale = scale;
        this.align = align;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getRow() {
        return row;
    }

    public int getLayer() {
        return layer;
    }

    public double getScale() {
        return scale;
    }

    public HudTextAlign getAlign() {
        return align;
    }
}
