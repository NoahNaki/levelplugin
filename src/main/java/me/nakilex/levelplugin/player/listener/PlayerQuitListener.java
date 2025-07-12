package me.nakilex.levelplugin.player.listener;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.environment.EnvironmentManager;
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
        UUID pid = player.getUniqueId();

        // Persist player data
        me.nakilex.levelplugin.player.config.PlayerConfig cfg = Main.getInstance().getPlayerConfig();
        Integer slot = me.nakilex.levelplugin.player.profile.ProfileManager.getInstance().getActiveSlot(pid);
        if (slot != null) {
            cfg.setProfileInventory(pid, slot, player.getInventory().getContents());
            cfg.setProfileArmor(pid, slot, player.getInventory().getArmorContents());
        }
        cfg.savePlayer(pid);
        me.nakilex.levelplugin.player.profile.ProfileManager.getInstance()
            .saveActiveLocation(player);
        me.nakilex.levelplugin.player.profile.ProfileSelectionGUI.handleQuit(player);
        EnvironmentManager.EnvironmentState st = environmentManager.getState(pid);
        String town = st != null ? environmentManager.getTown(pid) : null;
        environmentManager.saveState(pid);
        if (st != null && town != null) {
            stageManager.despawnForStage(pid, town, st.level, st.stage);
        }



    }
}
