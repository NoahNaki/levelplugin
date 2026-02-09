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
        applyGlowWithColor(entity, color, null);
    }

    public static void applyGlowWithColor(Entity entity, ChatColor color, Scoreboard board) {
        if (entity == null || color == null) {
            return;
        }
        Scoreboard resolved = board;
        if (resolved == null) {
            ScoreboardManager manager = entity.getServer().getScoreboardManager();
            if (manager == null) {
                return;
            }
            resolved = manager.getMainScoreboard();
        }
        String teamName = "glow_" + color.name().toLowerCase();
        Team team = resolved.getTeam(teamName);
        if (team == null) {
            team = resolved.registerNewTeam(teamName);
            team.setColor(color);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }
        team.addEntry(entity.getUniqueId().toString());
        entity.setGlowing(true);
    }
}
