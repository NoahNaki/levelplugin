package me.nakilex.levelplugin.core;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.GeoIpUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * Listener that attaches the player's country code on join.
 */
public class CountryListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getAddress() == null) {
            return;
        }
        String ip = player.getAddress().getAddress().getHostAddress();
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            String country = GeoIpUtil.getCountry(ip);
            if (country != null) {
                Bukkit.getLogger().info(player.getName() + " connected from " + country);
                Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                        player.setMetadata("country", new FixedMetadataValue(Main.getInstance(), country)));
            }
        });
    }
}
