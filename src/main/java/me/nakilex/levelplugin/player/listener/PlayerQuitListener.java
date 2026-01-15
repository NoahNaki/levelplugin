package me.nakilex.levelplugin.player.listener;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.environment.EnvironmentManager;
import me.nakilex.levelplugin.player.profile.ProfileManager;
import me.nakilex.levelplugin.npc.system.NpcTagUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.UUID;

public class PlayerQuitListener implements Listener {

    private final PlayerConfig  playerConfig;
    private final EnvironmentManager environmentManager;
    private final me.nakilex.levelplugin.environment.stage.TownStageManager stageManager;

    public PlayerQuitListener(PlayerConfig playerConfig, EnvironmentManager envManager) {
        this.playerConfig = playerConfig;
        this.environmentManager = envManager;
        this.stageManager = envManager.getStageManager();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (NpcTagUtil.isNpc(player)) {
            return;
        }
        UUID pid = player.getUniqueId();

        if (Main.getInstance().getQuestManager().isDebug()) {
            Main.getInstance().getLogger().info("[QuestDebug] PlayerQuit " + player.getName());
        }

        // Persist player data
        me.nakilex.levelplugin.player.config.PlayerConfig cfg = Main.getInstance().getPlayerConfig();
        boolean profilesEnabled = Main.getInstance().getCustomConfig()
                .getBoolean("features.profiles", true);
        me.nakilex.levelplugin.player.profile.ProfileManager pm =
                me.nakilex.levelplugin.player.profile.ProfileManager.getInstance();
        if (profilesEnabled) {
            pm.saveActiveProfile(player);
            me.nakilex.levelplugin.player.profile.ProfileSelectionGUI.handleQuit(player);
        } else {
            pm.saveProfile(player, 0);
        }
        cfg.savePlayer(pid);
        pm.addPlayMinutes(pid, 0); // flush playtime to config
        EnvironmentManager.EnvironmentState st = environmentManager.getState(pid);
        String town = st != null ? environmentManager.getTown(pid) : null;
        environmentManager.saveState(pid);
        if (st != null && town != null) {
            stageManager.despawnForStage(pid, town, st.level, st.stage);
        }

        // Reset the intro quest only if the player hasn't finished it yet
        me.nakilex.levelplugin.quests.managers.QuestManager qm =
                Main.getInstance().getQuestManager();
        boolean reset = false;
        if (qm.getProgress(pid, "officeerrands") != null) {
            if (qm.isDebug()) {
                Main.getInstance().getLogger().info("[QuestDebug] resetting OfficeErrands for " + player.getName());
            }
            qm.cleanupQuest(player, "officeerrands");
            qm.resetQuest(pid, "officeerrands", true);
            reset = true;
        }

        // Only wipe the dialog session if we actually reset the quest so
        // conversations from other quests can resume on rejoin
        if (reset) {
            if (qm.isDebug()) {
                Main.getInstance().getLogger().info("[QuestDebug] clearing dialog session for " + player.getName());
            }
            Main.getInstance().getDialogManager().resetDialog(player);
        }

    }
}
