package me.nakilex.levelplugin.scoreboard;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.economy.managers.GemsManager;
import me.nakilex.levelplugin.party.Party;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.*;

public class PlayerScoreboardManager implements org.bukkit.event.Listener {
    private final Main plugin;
    private final EconomyManager economyManager;
    private final GemsManager gemsManager;
    private final PartyManager partyManager;
    private final QuestManager questManager;
    private final LevelManager levelManager;

    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    @org.bukkit.event.EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        createBoard(event.getPlayer());
    }

    @org.bukkit.event.EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        removeBoard(event.getPlayer());
    }

    public PlayerScoreboardManager(Main plugin,
                                   EconomyManager economyManager,
                                   GemsManager gemsManager,
                                   PartyManager partyManager,
                                   QuestManager questManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.gemsManager = gemsManager;
        this.partyManager = partyManager;
        this.questManager = questManager;
        this.levelManager = plugin.getLevelManager();
    }

    public void createBoard(Player player) {
        ScoreboardManager sm = Bukkit.getScoreboardManager();
        if (sm == null) return;
        Scoreboard board = sm.getNewScoreboard();
        Objective obj = board.registerNewObjective("stats", "dummy", ChatColor.GREEN + "Player Stats");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        boards.put(player.getUniqueId(), board);
        player.setScoreboard(board);
        updateBoard(player);
    }

    public void removeBoard(Player player) {
        boards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void updateAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            updateBoard(p);
        }
    }

    public void updateBoard(Player player) {
        Scoreboard board = boards.get(player.getUniqueId());
        if (board == null) return;
        Objective obj = board.getObjective("stats");
        if (obj == null) return;
        board.getEntries().forEach(board::resetScores);

        int line = 15;
        Score coins = obj.getScore(ChatColor.YELLOW + "⛃ " + ChatColor.WHITE + "Coins: " + ChatColor.YELLOW + economyManager.getBalance(player));
        coins.setScore(line--);
        Score gems = obj.getScore(ChatColor.LIGHT_PURPLE + "✦ " + ChatColor.WHITE + "Gems: " + ChatColor.LIGHT_PURPLE + gemsManager.getTotalUnits(player));
        gems.setScore(line--);

        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId());
        Quest quest = progress != null ? progress.getQuest() : null;
        String trackedId = questManager.getTrackedQuest(player.getUniqueId());
        if (trackedId != null && (quest == null || !quest.getId().equals(trackedId))) {
            Quest other = questManager.getQuest(trackedId);
            if (other != null) quest = other;
        }
        if (quest != null) {
            Score qTitle = obj.getScore(ChatColor.GREEN + "Quest Progress:");
            qTitle.setScore(line--);
            int progIndex = 0;
            int progValue = 0;
            if (progress != null && progress.getQuest().getId().equals(quest.getId())) {
                for (int i = 0; i < quest.getObjectives().size(); i++) {
                    if (progress.getProgress(i) < quest.getObjectives().get(i).getAmount()) {
                        progIndex = i;
                        break;
                    }
                }
                progValue = progress.getProgress(progIndex);
            }
            Score qProg = obj.getScore(ChatColor.GRAY + "- " + progValue + "/" + quest.getObjectives().get(progIndex).getAmount());
            qProg.setScore(line--);
        }

        Party party = partyManager.getParty(player.getUniqueId());
        if (party != null) {
            Score partyTitle = obj.getScore(ChatColor.AQUA + "Party:");
            partyTitle.setScore(line--);
            for (UUID memberId : party.getMembers()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline()) {
                    int lvl = levelManager.getLevel(member);
                    String hp = (int) member.getHealth() + "/" + (int) member.getMaxHealth();
                    Score s = obj.getScore(ChatColor.GRAY + "[" + lvl + "]" + " " + ChatColor.WHITE + member.getName() + " " + ChatColor.GRAY + hp + " " + ChatColor.RED + "\u2764");
                    s.setScore(line--);
                } else {
                    String name = Bukkit.getOfflinePlayer(memberId).getName();
                    if (name == null) name = "Unknown";
                    Score s = obj.getScore(ChatColor.GRAY + "- " + name);
                    s.setScore(line--);
                }
                if (line <= 1) break;
            }
        }
    }
}
