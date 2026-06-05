package me.nakilex.levelplugin.dialogue.render;

import org.bukkit.entity.Player;

/**
 * Renders one static dialogue page for a player.
 */
public interface DialogueRenderer {
    void render(Player player, DialogueRenderContext context);
}
