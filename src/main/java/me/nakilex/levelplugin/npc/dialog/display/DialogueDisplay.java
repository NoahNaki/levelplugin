package me.nakilex.levelplugin.npc.dialog.display;

import org.bukkit.entity.Player;

/** Output strategy for dialogue-owned frames. */
public interface DialogueDisplay {
    void show(Player player, DialogueFrame frame);
    void clear(Player player);
    boolean isActive(Player player);
}
