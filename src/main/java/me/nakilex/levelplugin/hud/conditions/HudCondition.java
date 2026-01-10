package me.nakilex.levelplugin.hud.conditions;

import org.bukkit.entity.Player;

public interface HudCondition {
    boolean matches(Player player, HudConditionContext context);

    default String describe(Player player, HudConditionContext context) {
        return "";
    }
}
