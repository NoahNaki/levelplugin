package me.nakilex.levelplugin.cooking.util;

import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.entity.Player;

/** Sends cooking chat messages with spacer lines so consecutive stage updates are easier to read. */
public final class CookingChatMessageUtil {
    private CookingChatMessageUtil() {
    }

    public static void send(Player player, ChatMessageUtil.MessageType type, String message) {
        if (player == null) {
            return;
        }
        player.sendMessage(" ");
        ChatMessageUtil.send(player, type, message);
        player.sendMessage(" ");
    }
}
