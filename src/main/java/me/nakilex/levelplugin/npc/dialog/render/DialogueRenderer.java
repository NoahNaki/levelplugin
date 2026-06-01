package me.nakilex.levelplugin.npc.dialog.render;

import me.nakilex.levelplugin.npc.dialog.engine.DialogueSession;
import org.bukkit.entity.Player;

public interface DialogueRenderer {
    void render(Player player, DialogueSession session);
    void clear(Player player);
}
