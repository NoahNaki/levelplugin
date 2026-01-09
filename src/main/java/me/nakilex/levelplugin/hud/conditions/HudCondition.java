package me.nakilex.levelplugin.hud.conditions;

import org.bukkit.entity.Player;

public interface HudCondition {
    boolean matches(Player player);
}
