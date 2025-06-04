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
        Score coins = obj.getScore(ChatColor.GOLD + "Coins: " + economyManager.getBalance(player));
        coins.setScore(line--);
        Score gems = obj.getScore(ChatColor.LIGHT_PURPLE + "Gems: " + gemsManager.getTotalUnits(player));
        gems.setScore(line--);

        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId());
        Quest quest = progress != null ? progress.getQuest() : null;
        String trackedId = questManager.getTrackedQuest(player.getUniqueId());
        if (trackedId != null && (quest == null || !quest.getId().equals(trackedId))) {
            Quest other = questManager.getQuest(trackedId);
            if (other != null) quest = other;
        }
        if (quest != null) {
            Score qName = obj.getScore(ChatColor.AQUA + "Quest: " + quest.getName());
            qName.setScore(line--);
            int progIndex = 0;
            if (progress != null && progress.getQuest().getId().equals(quest.getId())) {
                for (int i = 0; i < quest.getObjectives().size(); i++) {
                    if (progress.getProgress(i) < quest.getObjectives().get(i).getAmount()) {
                        progIndex = i;
                        break;
                    }
                }
                Score qProg = obj.getScore(ChatColor.YELLOW + "Progress: " +
                        progress.getProgress(progIndex) + "/" + quest.getObjectives().get(progIndex).getAmount());
                qProg.setScore(line--);
            }
        }

        Party party = partyManager.getParty(player.getUniqueId());
        if (party != null) {
            Score partyTitle = obj.getScore(ChatColor.AQUA + "Party Members:");
            partyTitle.setScore(line--);
            for (UUID memberId : party.getMembers()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member == null) continue;
                int lvl = levelManager.getLevel(member);
                String hp = (int) member.getHealth() + "/" + (int) member.getMaxHealth();
                Score s = obj.getScore(ChatColor.GRAY + "- Lv" + lvl + " " + member.getName() + " " + hp);
                s.setScore(line--);
                if (line <= 1) break;
            }
        }
    }
}
