package me.nakilex.levelplugin.hud.render;

import org.bukkit.entity.Player;

public interface HudDisplay {
    void update(Player player, HudRenderOutput output);
    void clear(Player player);
    void clearAll();
}
