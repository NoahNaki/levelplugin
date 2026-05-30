package me.nakilex.levelplugin.npc.dialog.display;

import org.bukkit.ChatColor;

/** Stored dialogue frame with composition helpers for clean displays and future chat-history renderers. */
public record DialogueRenderedMessage(DialogueFrame frame) {
    public String composeMessage() {
        return ChatColor.DARK_GRAY + "[" + ChatColor.GRAY + (frame.index() + 1) + "/" + frame.total()
                + ChatColor.DARK_GRAY + "] " + ChatColor.YELLOW + frame.speaker()
                + ChatColor.WHITE + ": " + frame.text();
    }

    public String composeDarkMessage() {
        return ChatColor.DARK_GRAY + frame.speaker() + ": " + ChatColor.GRAY + frame.text();
    }
}
