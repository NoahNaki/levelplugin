package me.nakilex.levelplugin.settings.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.settings.environment.PlayerEnvironmentService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PersonalEnvironmentJoinListener implements Listener {

    private final Main plugin;
    private final PlayerEnvironmentService environmentService;

    public PersonalEnvironmentJoinListener(Main plugin, PlayerEnvironmentService environmentService) {
        this.plugin = plugin;
        this.environmentService = environmentService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                environmentService.restorePreferences(player);
            }
        }, 20L);
    }
}
