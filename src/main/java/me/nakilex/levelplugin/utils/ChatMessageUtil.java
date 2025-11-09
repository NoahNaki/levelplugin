package me.nakilex.levelplugin.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Utility for standardized chat message styling.
 * <p>
 * Message types map to colors and optional prefixes to keep
 * chat output consistent across the plugin.
 * </p>
 */
public final class ChatMessageUtil {

    private ChatMessageUtil() {
    }

    /**
     * Categories of messages with associated color and prefix.
     */
    public enum MessageType {
        INFO(ChatColor.WHITE, ""),
        SUCCESS(ChatColor.GREEN, ""),
        WARNING(ChatColor.YELLOW, ""),
        ERROR(ChatColor.RED, ""),
        REWARD(ChatColor.GOLD, "");

        private final ChatColor color;
        private final String prefix;

        MessageType(ChatColor color, String prefix) {
            this.color = color;
            this.prefix = prefix;
        }

        String apply(String msg) {
            return color + (prefix.isEmpty() ? "" : prefix + " ") + msg;
        }
    }

    /**
     * Format a message using the given type's styling.
     *
     * @param type    the message category
     * @param message the message text
     * @return formatted message
     */
    public static String format(MessageType type, String message) {
        return type.apply(message);
    }

    /**
     * Send a formatted and indented message to a player.
     *
     * @param player  target player
     * @param type    message category
     * @param message message text
     */
    public static void send(Player player, MessageType type, String message) {
        ChatFormatter.sendIndentedMessage(player, format(type, message));
    }

    /**
     * Convenience for non-player command senders.
     */
    public static void send(CommandSender sender, MessageType type, String message) {
        sender.sendMessage(format(type, message));
    }

    /** Broadcast a formatted message to all online players. */
    public static void broadcast(MessageType type, String message) {
        if (message == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            send(player, type, message);
        }
    }
}
