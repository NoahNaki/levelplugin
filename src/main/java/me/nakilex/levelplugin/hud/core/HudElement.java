package me.nakilex.levelplugin.hud.core;

import me.nakilex.levelplugin.hud.conditions.HudCondition;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class HudElement {
    private final String id;
    private final HudElementType type;
    private final int x;
    private final int y;
    private final int layer;
    private final double scale;
    private final HudTextAlign align;
    private final String text;
    private final String assetId;
    private final String anchorId;
    private final List<HudCondition> conditions;

    public HudElement(String id,
                      HudElementType type,
                      int x,
                      int y,
                      int layer,
                      double scale,
                      HudTextAlign align,
                      String text,
                      String assetId,
                      String anchorId,
                      List<HudCondition> conditions) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
        this.layer = layer;
        this.scale = scale;
        this.align = align == null ? HudTextAlign.LEFT : align;
        this.text = text;
        this.assetId = assetId;
        this.anchorId = anchorId;
        this.conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }

    public String getId() {
        return id;
    }

    public HudElementType getType() {
        return type;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
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

    public String getText() {
        return text;
    }

    public String getAssetId() {
        return assetId;
    }

    public String getAnchorId() {
        return anchorId;
    }

    public List<HudCondition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }

    public boolean shouldRender(Player player, me.nakilex.levelplugin.hud.conditions.HudConditionContext context) {
        for (HudCondition condition : conditions) {
            if (!condition.matches(player, context)) {
                return false;
            }
        }
        return true;
    }
}
