package me.nakilex.levelplugin.player.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class DeathBlindnessListener implements Listener {

    private final Plugin plugin;

    public DeathBlindnessListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Run one tick later so the player has actually respawned before giving the effect
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    // Blindness for 40 ticks = 2 seconds, amplifier 0
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, false));
                }
            }
        }.runTaskLater(plugin, 1L);
    }
}
