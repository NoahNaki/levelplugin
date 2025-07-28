package me.nakilex.levelplugin.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Utility for displaying a simple loading progress title.
 */
public class LoadingScreen {
    private static final int SEGMENTS = 6;

    /**
     * Display a loading progress bar using a title.
     *
     * @param player   target player
     * @param progress value between 0 and 1
     */
    public static void show(Player player, double progress) {
        if (player == null) return;
        if (progress < 0) progress = 0;
        if (progress > 1) progress = 1;
        int filled = (int) Math.round(progress * SEGMENTS);
        if (filled > SEGMENTS) filled = SEGMENTS;
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < SEGMENTS; i++) {
            if (i < filled) {
                bar.append(ChatColor.GREEN).append('■');
            } else {
                bar.append(ChatColor.GRAY).append('■');
            }
        }
        String title = ChatColor.WHITE + "LOADING";
        player.sendTitle(title, bar.toString(), 0, 20, 10);
    }
}
