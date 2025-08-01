package me.nakilex.levelplugin.utils;

import me.nakilex.levelplugin.utils.DefaultFontInfo;
import org.bukkit.entity.Player;

public class ChatFormatter {

    private static final int CENTER_PX = 154; // This is approximately the center of the Minecraft chat window

    public static void sendCenteredMessage(Player player, String message) {
        if (message == null || message.equals("")) return;

        player.sendMessage(getCenteredText(message));
    }

    /**
     * Return the provided message padded so that it appears centered
     * in chat. This does not send anything to the player.
     */
    public static String getCenteredText(String message) {
        if (message == null || message.isEmpty()) return "";

        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;

        for (char c : message.toCharArray()) {
            if (c == '§') {
                previousCode = true;
                continue;
            } else if (previousCode) {
                previousCode = false;
                isBold = c == 'l' || c == 'L';
            } else {
                DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
                messagePxSize += isBold ? DefaultFontInfo.getBoldLength() : dFI.getLength();
                messagePxSize++;
            }
        }

        int toCompensate = CENTER_PX - (messagePxSize / 2);
        int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
        int compensated = 0;
        StringBuilder sb = new StringBuilder();
        while (compensated < toCompensate) {
            sb.append(' ');
            compensated += spaceLength;
        }
        return sb + message;
    }

    private static void centerMessage(Player player, String message) {
        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;

        for (char c : message.toCharArray()) {
            if (c == '§') {
                previousCode = true;
                continue;
            } else if (previousCode) {
                previousCode = false;
                isBold = c == 'l' || c == 'L';
            } else {
                DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
                messagePxSize += isBold ? DefaultFontInfo.getBoldLength() : dFI.getLength();
                messagePxSize++;
            }
        }

        int toCompensate = CENTER_PX - (messagePxSize / 2);
        int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
        int compensated = 0;
        StringBuilder sb = new StringBuilder();
        while (compensated < toCompensate) {
            sb.append(" ");
            compensated += spaceLength;
        }
        player.sendMessage(sb.toString() + message);
    }

    public static void constructDivider(Player player, String dividerChar, int length) {
        StringBuilder divider = new StringBuilder();
        for (int i = 0; i < length; i++) {
            divider.append(dividerChar);
        }
        player.sendMessage(divider.toString());
    }

    /**
     * Send a line of chat with a small indentation.
     */
    public static void sendIndentedMessage(Player player, String message) {
        if (message == null) return;
        player.sendMessage("        " + message);
    }

    /** Return the standard colored label used for experience amounts. */
    public static String experienceLabel() {
        return net.md_5.bungee.api.ChatColor.of("#47b587") + "EXP";
    }

    /**
     * Send multiple centered lines wrapped in a colored divider.
     *
     * @param player the target player
     * @param dividerColor the color code for the divider (e.g. "§a")
     * @param lines the messages to center between the dividers
     */
    public static void sendBoxedCenteredMessages(Player player, String dividerColor, String... lines) {
        constructDivider(player, dividerColor + "§l-", 45);
        for (String line : lines) {
            sendCenteredMessage(player, line);
        }
        constructDivider(player, dividerColor + "§l-", 45);
    }

}
