package me.nakilex.levelplugin.guild;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.guild.events.GuildMembershipEvent;
import me.nakilex.levelplugin.guild.siege.GuildSiegeManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Updates town holograms when players join or leave guilds.
 */
public class GuildMembershipListener implements Listener {
    @EventHandler
    public void onGuildChange(GuildMembershipEvent event) {
        Main.getInstance().getLogger().info("[GuildDebug] Event " + event.getAction() + " for " + event.getPlayer().getName());
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            Main.getInstance().getLogger().info("[GuildDebug] Refreshing visibility for " + event.getPlayer().getName());
            GuildSiegeManager.getInstance().refreshTownVisibility(event.getPlayer());
        }, 40L);
    }
}
