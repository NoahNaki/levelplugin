package me.nakilex.levelplugin.friend;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;
import me.nakilex.levelplugin.npc.system.NpcTagUtil;

import java.util.UUID;

/**
 * Notifies players of pending friend requests when they join.
 */
public class FriendRequestListener implements Listener {
    private final FriendManager manager;

    public FriendRequestListener(FriendManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (NpcTagUtil.isNpc(player)) {
            return;
        }
        UUID inviter = manager.getRequest(player.getUniqueId());
        if (inviter != null) {
            String name = Bukkit.getOfflinePlayer(inviter).getName();
            if (name == null) name = inviter.toString();
            player.sendMessage(ChatColor.GREEN + name + " has sent you a friend request.");
            player.sendMessage(ChatColor.YELLOW + "Type /friend accept to add or /friend deny to decline.");
        }
    }
}
