package me.nakilex.levelplugin.pet.utils;

import me.nakilex.levelplugin.utils.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class PetChatUtil {
    private static final String PREFIX = ChatColor.DARK_GRAY + "[" + ChatColor.LIGHT_PURPLE + "Pet" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;

    private PetChatUtil() {
    }

    public static void send(Player player, String message) {
        if (player == null || message == null) {
            return;
        }
        player.sendMessage(PREFIX + ChatUtil.applyEmojis(message));
    }
}
