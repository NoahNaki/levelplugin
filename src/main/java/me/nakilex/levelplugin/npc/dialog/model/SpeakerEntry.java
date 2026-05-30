package me.nakilex.levelplugin.npc.dialog.model;

import org.bukkit.ChatColor;
import org.bukkit.Sound;

/** Optional speaker presentation metadata for dialogue entries. */
public record SpeakerEntry(String id, String displayName, Sound sound, ChatColor color) {
    public SpeakerEntry(String id, String displayName) {
        this(id, displayName, null, ChatColor.YELLOW);
    }

    public SpeakerEntry {
        color = color == null ? ChatColor.YELLOW : color;
    }
}
