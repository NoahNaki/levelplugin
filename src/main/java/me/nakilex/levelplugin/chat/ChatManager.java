package me.nakilex.levelplugin.chat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles global chat state such as muting, clearing and per-player channels.
 */
public class ChatManager {

    private static boolean muted = false;
    private static final Map<UUID, ChatChannel> channels = new HashMap<>();

    /** Mute all chat messages. */
    public static void muteAll() {
        muted = true;
    }

    /** Unmute chat messages. */
    public static void unmuteAll() {
        muted = false;
    }

    /** @return whether chat is currently muted. */
    public static boolean isMuted() {
        return muted;
    }

    /** Clear the chat for all online players. */
    public static void clearChat() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < 100; i++) {
                p.sendMessage("");
            }
        }
    }

    /** Set a player's active chat channel. */
    public static void setChannel(UUID player, ChatChannel channel) {
        channels.put(player, channel);
    }

    /** Get a player's current chat channel. Defaults to region chat. */
    public static ChatChannel getChannel(UUID player) {
        return channels.getOrDefault(player, ChatChannel.REGION);
    }
}
