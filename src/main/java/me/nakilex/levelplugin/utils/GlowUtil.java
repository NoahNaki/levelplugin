package me.nakilex.levelplugin.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

public final class GlowUtil {
    private GlowUtil() {
    }

    public static void applyGlowWithColor(Entity entity, ChatColor color) {
        if (entity == null || color == null) {
            return;
        }
        ScoreboardManager manager = entity.getServer().getScoreboardManager();
        if (manager == null) {
            return;
        }
        Scoreboard board = manager.getMainScoreboard();
        String teamName = "glow_" + color.name().toLowerCase();
        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
            team.setColor(color);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }
        team.addEntry(entity.getUniqueId().toString());
        entity.setGlowing(true);
    }
}
