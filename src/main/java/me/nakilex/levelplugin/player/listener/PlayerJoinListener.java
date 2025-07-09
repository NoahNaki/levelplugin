package me.nakilex.levelplugin.player.listener;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.player.mining.managers.MiningManager;
import me.nakilex.levelplugin.environment.EnvironmentManager;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import me.nakilex.levelplugin.player.profile.ProfileSelectionGUI;
import me.nakilex.levelplugin.player.profile.ProfileManager;

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
        // Early initialization and teleport
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

            // Teleport to profile lobby in world2
            org.bukkit.World lobbyWorld = org.bukkit.Bukkit.getWorld("world2");
            if (lobbyWorld != null) {
                org.bukkit.Location lobby = new org.bukkit.Location(lobbyWorld, 217, 6, 80);
                player.teleport(lobby);
            }

            // Additional per-player loading can happen here
        }, 2L);  // 2 ticks

        // Delay profile menu so gravity settles the player
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if (!player.isOnline()) return;
            ProfileManager.getInstance().clearActiveSlot(pid);
            ProfileSelectionGUI.startSelection(player);
        }, 30L);  // ~1.5 seconds
    }
}
