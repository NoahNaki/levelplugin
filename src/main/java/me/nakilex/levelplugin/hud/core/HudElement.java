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
    private final String text;
    private final List<HudCondition> conditions;

    public HudElement(String id,
                      HudElementType type,
                      int x,
                      int y,
                      int layer,
                      String text,
                      List<HudCondition> conditions) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
        this.layer = layer;
        this.text = text;
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

    public String getText() {
        return text;
    }

    public List<HudCondition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }

    public boolean shouldRender(Player player) {
        for (HudCondition condition : conditions) {
            if (!condition.matches(player)) {
                return false;
            }
        }
        return true;
    }
}
