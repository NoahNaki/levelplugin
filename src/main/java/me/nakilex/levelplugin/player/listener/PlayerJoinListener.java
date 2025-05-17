package me.nakilex.levelplugin.player.listener;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final LevelManager levelManager;

    public PlayerJoinListener(LevelManager levelManager) {
        this.levelManager = levelManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        // Delay setting the gamemode to ensure no other plugin overrides it
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            player.setGameMode(GameMode.ADVENTURE);

            // Recalculate stats to apply the proper health scaling
            StatsManager.getInstance().recalcDerivedStats(player);
            levelManager.initializePlayer(player);

            // Apply health scaling and update scoreboard
            player.setHealthScaled(true);
            player.setHealthScale(20.0);
        }, 2L); // delay of 2 ticks (~0.1 seconds)
    }
}