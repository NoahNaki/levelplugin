package me.nakilex.levelplugin.mail;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class MailJoinNotifier implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            int unread = MailManager.getInstance().getUnreadCount(event.getPlayer().getUniqueId());
            if (unread > 0) {
                ChatMessageUtil.send(event.getPlayer(), ChatMessageUtil.MessageType.INFO,
                        "You have " + unread + " unread mail. Use /mail.");
            }
        }, 30L);
    }
}
