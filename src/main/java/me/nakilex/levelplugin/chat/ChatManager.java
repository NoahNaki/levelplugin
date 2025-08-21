package me.nakilex.levelplugin.chat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Handles global chat state such as muting and clearing.
 */
public class ChatManager implements Listener {

    private static boolean muted = false;

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

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (muted && !event.getPlayer().hasPermission("levelplugin.chat.bypass")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Chat is currently muted.");
        }
    }
}
