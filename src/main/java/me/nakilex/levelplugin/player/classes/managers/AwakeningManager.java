package me.nakilex.levelplugin.player.classes.managers;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.gui.AwakeningGUI;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import org.bukkit.entity.Player;

public class AwakeningManager {
    public static void check(Player player) {
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        int level = LevelManager.getInstance().getLevel(player);
        int stage = ps.awakeningStage;
        PlayerClass base = ps.playerClass;

        if (base == PlayerClass.WARRIOR) {
            if (stage < 1 && level >= 25) {
                AwakeningGUI.open(player, 1, PlayerClass.BARBARIAN, PlayerClass.DRAGONIAN);
            } else if (stage < 2 && level >= 50) {
                AwakeningGUI.open(player, 2, null, PlayerClass.GALEGLAIVE);
            } else if (stage < 3 && level >= 75) {
                AwakeningGUI.open(player, 3, PlayerClass.DEATHKNIGHT, PlayerClass.ARCTICKNIGHT);
            } else if (stage < 4 && level >= 100) {
                AwakeningGUI.open(player, 4, null, PlayerClass.DRAGONWARRIOR);
            }
        }
    }
}
