package me.nakilex.levelplugin.player.listener;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.player.fishing.managers.FishingManager;
import me.nakilex.levelplugin.player.mining.managers.MiningManager;
import me.nakilex.levelplugin.environment.EnvironmentManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import me.nakilex.levelplugin.player.profile.ProfileEntryUtil;
import me.nakilex.levelplugin.server.ServerSelectionManager;

import java.util.UUID;

public class PlayerJoinListener implements Listener {

    private final LevelManager levelManager;
    private final MiningManager miningManager;
    private final me.nakilex.levelplugin.player.farming.managers.FarmingManager farmingManager;
    private final FishingManager fishingManager;
    private final EnvironmentManager environmentManager;
    private final me.nakilex.levelplugin.environment.stage.TownStageManager stageManager;
    private final ServerSelectionManager serverSelectionManager;

    public PlayerJoinListener(LevelManager levelManager, MiningManager miningManager,
                              me.nakilex.levelplugin.player.farming.managers.FarmingManager farmingManager,
                              FishingManager fishingManager,
                              EnvironmentManager envManager,
                              ServerSelectionManager serverSelectionManager) {
        this.levelManager  = levelManager;
        this.miningManager = miningManager;
        this.farmingManager = farmingManager;
        this.fishingManager = fishingManager;
        this.environmentManager = envManager;
        this.stageManager = envManager.getStageManager();
        this.serverSelectionManager = serverSelectionManager;
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
            farmingManager.initializePlayer(player);
            fishingManager.initializePlayer(player);
            environmentManager.loadPlayerState(player);
            stageManager.hideNPCsFrom(player);
            player.setHealthScaled(true);
            player.setHealthScale(20.0);

            if (serverSelectionManager != null) {
                serverSelectionManager.handleJoin(player);
            }

            me.nakilex.levelplugin.quests.managers.QuestManager qm = Main.getInstance().getQuestManager();

            me.nakilex.levelplugin.quests.data.Quest nb1 = qm.getQuest("newbeginning");

            // Repeatedly hide NPC 547 until quest1 is completed, only after the
            // player has entered the "flatland" world where that NPC resides.
            new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) { cancel(); return; }

                    me.nakilex.levelplugin.npc.system.NPC moved =
                            me.nakilex.levelplugin.npc.system.NpcApi.getRegistry().getById(547);
                    me.nakilex.levelplugin.quests.gui.QuestState state =
                            qm.getQuestState(player, nb1);

                    if (state == me.nakilex.levelplugin.quests.gui.QuestState.COMPLETED) {
                        if (moved != null && moved.isSpawned()
                                && player.getWorld().equals(moved.getEntity().getWorld())) {
                            player.showEntity(Main.getInstance(), moved.getEntity());
                        }
                        cancel();
                        return;
                    }

                    // Wait until the player is actually in the flatland world so
                    // the NPC can be hidden client-side.
                    if (moved != null && moved.isSpawned()
                            && "flatland".equals(player.getWorld().getName())) {
                        player.hideEntity(Main.getInstance(), moved.getEntity());
                    }
                }
            }.runTaskTimer(Main.getInstance(), 0L, 40L);

            // Spawn the town immediately if the player logs in nearby
            org.bukkit.Location origin = environmentManager.getOrigin(pid);
            if (origin != null && origin.getWorld().equals(player.getWorld())
                    && player.getLocation().distanceSquared(origin) <= 350 * 350) {
                if (!environmentManager.isTownLoaded(player)) {
                    if (!environmentManager.hasPlayedInitAnimation(player)) {
                        environmentManager.initializePlayerAnimated(player, 20);
                        environmentManager.markAnimationPlayed(player);
                    } else {
                        environmentManager.initializePlayer(player);
                    }
                    environmentManager.markTownLoaded(player, true);
                }
            }

            // Additional per-player loading can happen here
        }, 2L);  // 2 ticks

        // Delay profile menu so gravity settles the player
        if (serverSelectionManager == null) {
            ProfileEntryUtil.handleProfileEntry(player);
        }
    }
}
