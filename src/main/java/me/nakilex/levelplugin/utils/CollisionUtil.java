package me.nakilex.levelplugin.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Utility for toggling player collisions using a scoreboard team.
 * Players added to the team will never collide with others.
 */
public final class CollisionUtil {
    private static final String TEAM_NAME = "noCollide";
    private static Team team;

    private CollisionUtil() {}

    private static Team getTeam() {
        if (team == null) {
            Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
            team = board.getTeam(TEAM_NAME);
            if (team == null) {
                team = board.registerNewTeam(TEAM_NAME);
                team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            }
        }
        return team;
    }

    /**
     * Toggle whether the player should collide with others.
     */
    public static void setCollidable(Player player, boolean collidable) {
        Team t = getTeam();
        if (collidable) {
            t.removeEntry(player.getName());
        } else {
            t.addEntry(player.getName());
        }
        player.setCollidable(collidable);
    }

    /**
     * Remove the player from the no-collision team entirely.
     */
    public static void remove(Player player) {
        if (team != null) {
            team.removeEntry(player.getName());
        }
    }
}
