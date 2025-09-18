package me.nakilex.levelplugin.chat;

import me.nakilex.levelplugin.utils.ChatUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/** Routes player chat to the appropriate channel. */
public class ChatChannelListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (ChatManager.isMuted() && !player.hasPermission("levelplugin.chat.bypass")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Chat is currently muted.");
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        Component base = ChatUtil.buildMessage(player, event.getMessage());
        event.setCancelled(true);
        ChatManager.sendChannelMessage(player, base);
    }
}
