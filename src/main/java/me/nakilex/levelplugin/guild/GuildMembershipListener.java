package me.nakilex.levelplugin.guild;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.guild.events.GuildMembershipEvent;
import me.nakilex.levelplugin.guild.siege.GuildSiegeManager;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.npc.system.NpcTagUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Updates town holograms when players join or leave guilds.
 */
public class GuildMembershipListener implements Listener {
    @EventHandler
    public void onGuildChange(GuildMembershipEvent event) {
        if (NpcTagUtil.isNpc(event.getPlayer())) {
            return;
        }
        Main.getInstance().getLogger().info("[GuildDebug] Event " + event.getAction() + " for " + event.getPlayer().getName());
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            Main.getInstance().getLogger().info("[GuildDebug] Refreshing visibility for " + event.getPlayer().getName());
            GuildSiegeManager.getInstance().refreshTownVisibility(event.getPlayer());
        }, 40L);
        if (event.getAction() == GuildMembershipEvent.Action.LEAVE) {
            if (GuildSiegeManager.getInstance().leave(event.getPlayer().getUniqueId())) {
                ChatFormatter.sendCenteredMessage(event.getPlayer(),
                        ChatColor.RED + "Your siege sign-up was cancelled because you left your guild.");
            }
        }
    }
}
