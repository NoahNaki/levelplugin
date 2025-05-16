package me.nakilex.levelplugin.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Utility to send styled toggle feedback messages to players.
 */
public final class ToggleFeedbackUtil {
    private ToggleFeedbackUtil() {}

    /**
     * Sends a standardized ON/OFF message.
     *
     * @param player  the recipient
     * @param feature the feature name (e.g. "Damage chat")
     * @param enabled true if the feature is now enabled
     */
    public static void sendToggle(Player player, String feature, boolean enabled) {
        String status = enabled
            ? ChatColor.GREEN + "" + ChatColor.BOLD + "ON"
            : ChatColor.RED   + "" + ChatColor.BOLD + "OFF";
        player.sendMessage(
            ChatColor.GRAY + feature + ": " + status
        );
    }
}
