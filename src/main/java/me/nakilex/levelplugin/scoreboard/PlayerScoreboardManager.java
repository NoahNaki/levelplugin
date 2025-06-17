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
import me.nakilex.levelplugin.quests.data.QuestObjective;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class PlayerScoreboardManager implements org.bukkit.event.Listener {
    private final Main plugin;
    private final EconomyManager economyManager;
    private final GemsManager gemsManager;
    private final PartyManager partyManager;
    private final QuestManager questManager;
    private final LevelManager levelManager;

    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private final String[] entries = new String[15];

    /**
     * Exposes the internal scoreboard instance for other managers.
     */
    public Scoreboard getBoard(Player player) {
        return boards.get(player.getUniqueId());
    }

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

    private void setLine(Scoreboard board, Objective obj, int index, int score, String text) {
        if (index < 0 || index >= entries.length) return;

        String entry = entries[index];
        if (entry == null) {
            entry = ChatColor.values()[index].toString();
            entries[index] = entry;
        }

        Team team = board.getTeam("line" + index);
        if (team == null) {
            team = board.registerNewTeam("line" + index);
            team.addEntry(entry);
        }
        team.setPrefix(text);
        obj.getScore(entry).setScore(score);
    }

    public void updateBoard(Player player) {
        Scoreboard board = boards.get(player.getUniqueId());
        if (board == null) return;
        Objective obj = board.getObjective("stats");
        if (obj == null) return;
        board.getEntries().forEach(board::resetScores);

        int line = 15;
        int idx = 0;
        String coinStr = java.text.NumberFormat.getIntegerInstance().format(economyManager.getBalance(player));
        setLine(board, obj, idx++, line--, ChatColor.YELLOW + "⛃ " + ChatColor.WHITE + "Coins: " + ChatColor.YELLOW + coinStr);

        String gemStr = java.text.NumberFormat.getIntegerInstance().format(gemsManager.getTotalUnits(player));
        setLine(board, obj, idx++, line--, ChatColor.LIGHT_PURPLE + "✦ " + ChatColor.WHITE + "Gems: " + ChatColor.LIGHT_PURPLE + gemStr);

        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId());
        Quest quest = progress != null ? progress.getQuest() : null;
        String trackedId = questManager.getTrackedQuest(player.getUniqueId());
        if (trackedId != null && (quest == null || !quest.getId().equals(trackedId))) {
            Quest other = questManager.getQuest(trackedId);
            if (other != null) quest = other;
        }
        if (quest != null) {
            setLine(board, obj, idx++, line--,
                    ChatColor.GREEN + "Quest: " + ChatColor.WHITE + quest.getName());
            setLine(board, obj, idx++, line--, ChatColor.GREEN + "Progress:");
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
            QuestObjective currentObj = quest.getObjectives().get(progIndex);
            String desc = questManager.describeObjective(currentObj);
            setLine(board, obj, idx++, line--,
                    ChatColor.GRAY + "- " + desc + ": " + progValue + "/" + currentObj.getAmount());
        }

        Party party = partyManager.getParty(player.getUniqueId());
        if (party != null) {
            setLine(board, obj, idx++, line--, ChatColor.AQUA + "Party:");
            for (UUID memberId : party.getMembers()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline()) {
                    int lvl = levelManager.getLevel(member);
                    String hp = (int) member.getHealth() + "/" + (int) member.getMaxHealth();
                    setLine(board, obj, idx++, line--, ChatColor.GRAY + "[" + lvl + "] " + ChatColor.WHITE + member.getName() + " " + ChatColor.GRAY + hp + " " + ChatColor.RED + "\u2764");
                } else {
                    String name = Bukkit.getOfflinePlayer(memberId).getName();
                    if (name == null) name = "Unknown";
                    setLine(board, obj, idx++, line--, ChatColor.GRAY + "- " + name);
                }
                if (line <= 1) break;
            }
        }

        // Apply party and friend glow scoreboard entries if enabled
        plugin.getPartyGlowManager().applyGlowScoreboard(player);
        plugin.getFriendGlowManager().applyGlowScoreboard(player);
    }
}
