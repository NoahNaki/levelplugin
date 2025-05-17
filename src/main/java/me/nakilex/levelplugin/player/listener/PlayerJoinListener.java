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

        // Initial debug log
        Main.getInstance().getLogger().info("[PlayerJoinListener] onPlayerJoin fired for player: " + player.getName());
        player.sendMessage("[Debug] onPlayerJoin called");

        // Delay setting the gamemode to ensure no other plugin overrides it
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            player.setGameMode(GameMode.ADVENTURE);
            Main.getInstance().getLogger().info("[PlayerJoinListener] Delayed Set game mode to: " + player.getGameMode());
            player.sendMessage("[Debug] Delayed GameMode is now: " + player.getGameMode());

            // Recalculate stats to apply the proper health scaling
            StatsManager.getInstance().recalcDerivedStats(player);
            levelManager.initializePlayer(player);

            // Apply health scaling and update scoreboard
            player.setHealthScaled(true);
            player.setHealthScale(20.0);
            Main.getInstance().getLogger().info("[PlayerJoinListener] Health scaled and set to 20.0 for player: " + player.getName());
            player.sendMessage("[Debug] Health scaled: " + player.getHealthScale());
        }, 2L); // delay of 2 ticks (~0.1 seconds)
    }
}
