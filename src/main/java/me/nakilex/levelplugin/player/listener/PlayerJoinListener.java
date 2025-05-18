package me.nakilex.levelplugin.player.listener;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.runes.manager.RunesManager;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.UUID;

public class PlayerJoinListener implements Listener {

    private final LevelManager levelManager;
    private final PlayerConfig playerConfig;
    private final RunesManager runesManager;

    public PlayerJoinListener(LevelManager levelManager, PlayerConfig playerConfig) {
        this.levelManager  = levelManager;
        this.playerConfig  = playerConfig;
        this.runesManager  = SpellManager.getInstance().getRunesManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID pid = player.getUniqueId();

        // Delay to let other plugins finish their startup logic
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            // 1) Set up gamemode & stats
            player.setGameMode(GameMode.ADVENTURE);
            StatsManager.getInstance().recalcDerivedStats(player);
            levelManager.initializePlayer(player);
            player.setHealthScaled(true);
            player.setHealthScale(20.0);

            // 2) Load equipped runes from PlayerConfig
            List<String> storedRunes = Main.getInstance()
                .getPlayerConfig()
                .getEquippedRunes(pid);
            if (storedRunes != null && !storedRunes.isEmpty()) {
                runesManager.loadPlayerRunes(pid, storedRunes);
            }
        }, 2L);  // 2 ticks
    }
}
