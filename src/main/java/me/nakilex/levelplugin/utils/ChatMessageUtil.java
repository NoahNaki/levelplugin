package me.nakilex.levelplugin.utils;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.nakilex.levelplugin.utils.NumberUtil;

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

    public static void send(CommandSender sender, MessageType type, String message) {
        sender.sendMessage(format(type, message));
    }

    /**
     * Send a standardized milestone reached message with coin rewards.
     */
    public static void sendMilestoneMessage(Player player, String mobName, int killAmount, int coins) {
        if (player == null) {
            return;
        }
        String safeName = (mobName == null || mobName.isBlank()) ? "Unknown" : mobName;
        String killsText = NumberUtil.formatCommas(killAmount);
        String coinText = NumberUtil.formatCommas(coins);
        send((CommandSender) player, MessageType.REWARD,
                ChatColor.GOLD + "" + ChatColor.BOLD + "MILESTONE REACHED! "
                        + ChatColor.WHITE + killsText + "x "
                        + ChatColor.YELLOW + safeName + ChatColor.GOLD + " "
                        + ChatColor.GRAY + "You received "
                        + ChatColor.WHITE + coinText + " <glyph:coins_icon>"
                        + ChatColor.GRAY + ".");
    }
    /**
     * Send a standardized purchase confirmation to a player.
     */
    public static void sendPurchaseMessage(Player player, String itemName, int coinCost) {
        sendPurchaseMessage(player, itemName, coinCost, 0);
    }

    /**
     * Send a standardized purchase confirmation to a player, supporting both coins and gems.
     */
    public static void sendPurchaseMessage(Player player, String itemName, int coinCost, int gemCost) {
        send(player, MessageType.SUCCESS, buildPurchaseMessage(itemName, coinCost, gemCost));
    }

    /**
     * Build a consistent purchase message for reuse across systems.
     */
    public static String buildPurchaseMessage(String itemName, int coinCost, int gemCost) {
        StringBuilder message = new StringBuilder();
        message.append("You purchased ").append(itemName);

        boolean hasCoins = coinCost > 0;
        boolean hasGems = gemCost > 0;
        if (hasCoins) {
            message.append(ChatColor.GREEN).append(" for ")
                    .append(ChatColor.YELLOW).append(coinCost).append(" <glyph:coins_icon> coins");
        }
        if (hasGems) {
            message.append(hasCoins ? ChatColor.GRAY + " and " : ChatColor.GREEN + " for ")
                    .append(ChatColor.LIGHT_PURPLE).append(gemCost).append("<glyph:purple_orb_icon>");
        }
        message.append(ChatColor.GREEN).append('.');
        return message.toString();
    }

    /** Broadcast a formatted message to all online players. */
    public static void broadcast(MessageType type, String message) {
        if (message == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            send(player, type, message);
        }
    }
}
