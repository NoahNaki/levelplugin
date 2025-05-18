package me.nakilex.levelplugin.player.listener;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.runes.manager.RunesManager;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.UUID;

public class PlayerQuitListener implements Listener {

    private final PlayerConfig  playerConfig;
    private final RunesManager  runesManager;

    public PlayerQuitListener(PlayerConfig playerConfig) {
        this.playerConfig = playerConfig;
        this.runesManager = SpellManager.getInstance().getRunesManager();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID pid = player.getUniqueId();

        // Get currently equipped runes and save them
        List<String> equipped = runesManager.getEquippedRuneIds(player);
        Main.getInstance()
            .getPlayerConfig()
            .setEquippedRunes(pid, equipped);
        Main.getInstance()
            .getPlayerConfig()
            .savePlayer(pid);
    }
}
