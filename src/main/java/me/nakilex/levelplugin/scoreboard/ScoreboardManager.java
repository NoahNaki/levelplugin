// File: me/nakilex/levelplugin/scoreboard/ScoreboardManager.java
package me.nakilex.levelplugin.scoreboard;

import me.nakilex.levelplugin.party.Party;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

public class ScoreboardManager {

    public void updateScoreboard(Player player, int money, Party party) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("status", "dummy", ChatColor.GOLD + "Status");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Money display
        Score moneyScore = obj.getScore(ChatColor.GREEN + "Money: $" + money);
        moneyScore.setScore(3);

        // If in a party, display party members and their HP
        if (party != null) {
            Score header = obj.getScore(ChatColor.YELLOW + "Party Members:");
            header.setScore(2);
            int score = 1;
            for (var memberId : party.getMembers()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline()) {
                    double health = member.getHealth();
                    Score memberScore = obj.getScore(ChatColor.AQUA + member.getName() + ": " + health + " HP");
                    memberScore.setScore(score);
                    score--;
                }
            }
        }

        // Finally, set the player's scoreboard
        player.setScoreboard(board);
    }
}
