package me.nakilex.levelplugin.player.listener;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.player.mining.managers.MiningManager;
import me.nakilex.levelplugin.environment.EnvironmentManager;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.listener.ClassSelectionListener;
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
    private final MiningManager miningManager;
    private final PlayerConfig playerConfig;
    private final EnvironmentManager environmentManager;
    private final me.nakilex.levelplugin.environment.stage.TownStageManager stageManager;

    public PlayerJoinListener(LevelManager levelManager, MiningManager miningManager, PlayerConfig playerConfig, EnvironmentManager envManager) {
        this.levelManager  = levelManager;
        this.miningManager = miningManager;
        this.playerConfig  = playerConfig;
        this.environmentManager = envManager;
        this.stageManager = envManager.getStageManager();
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
            miningManager.initializePlayer(player);
            environmentManager.initializePlayer(player);
            stageManager.hideNPCsFrom(player);
            player.setHealthScaled(true);
            player.setHealthScale(20.0);

            EconomyManager eco = Main.getInstance().getEconomyManager();
            if (eco.getBalance(player) == 0) {
                eco.addCoins(player, 20);
                player.sendMessage(org.bukkit.ChatColor.YELLOW + "You received 20 coins to get started!");
            }

            // 2) If they haven't chosen a class, freeze and show menu
            StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(pid);
            if (ps.playerClass == PlayerClass.VILLAGER) {
                ClassSelectionListener.addPending(player);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "dm open mmocore_class_warrior -p:" + player.getName());
            }

            // 3) Additional per-player loading can happen here
        }, 2L);  // 2 ticks
    }
}
