package me.nakilex.levelplugin.utils;

import me.nakilex.levelplugin.utils.DefaultFontInfo;
import org.bukkit.entity.Player;

public class ChatFormatter {

    private static final int CENTER_PX = 154; // This is approximately the center of the Minecraft chat window

    public static void sendCenteredMessage(Player player, String message) {
        if (message == null || message.equals("")) return;

        centerMessage(player, message);
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

}
