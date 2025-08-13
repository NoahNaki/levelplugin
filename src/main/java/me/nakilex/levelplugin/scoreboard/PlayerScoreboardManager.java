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
    private final Map<UUID, String[]> lastLines = new HashMap<>();
    /** Players that enabled TPS display. */
    private final java.util.Set<UUID> showTps = new java.util.HashSet<>();

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
        Objective obj = board.registerNewObjective("stats", "dummy", ChatColor.GREEN + "Profile Stats");
        try {
            // Paper 1.20+ includes an overload allowing sidebar numbers to be hidden
            java.lang.reflect.Method m = obj.getClass().getMethod("setDisplaySlot", DisplaySlot.class, boolean.class);
            m.invoke(obj, DisplaySlot.SIDEBAR, false);
        } catch (Throwable ignore) {
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        boards.put(player.getUniqueId(), board);
        player.setScoreboard(board);
        updateBoard(player);
    }

    public void removeBoard(Player player) {
        boards.remove(player.getUniqueId());
        lastLines.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        showTps.remove(player.getUniqueId());
    }

    /** Toggle TPS display for this player. */
    public boolean toggleTps(Player player) {
        UUID id = player.getUniqueId();
        boolean enabled;
        if (showTps.contains(id)) {
            showTps.remove(id);
            enabled = false;
        } else {
            showTps.add(id);
            enabled = true;
        }
        updateBoard(player);
        return enabled;
    }

    /** Check if TPS display is enabled for the player. */
    public boolean isTpsEnabled(Player player) {
        return showTps.contains(player.getUniqueId());
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
        String[] prev = lastLines.computeIfAbsent(player.getUniqueId(), k -> new String[entries.length]);
        String[] current = new String[entries.length];

        int line = 15;
        int idx = 0;

        // spacer above currency lines
        current[idx] = " ";
        if (!current[idx].equals(prev[idx])) {
            setLine(board, obj, idx, line, current[idx]);
        }
        idx++; line--;

        String coinStr = java.text.NumberFormat.getIntegerInstance().format(economyManager.getBalance(player));
        current[idx] = ChatColor.GOLD + "<glyph:coins_icon> " + ChatColor.WHITE + "Coins: " + ChatColor.GOLD + coinStr;
        if (!current[idx].equals(prev[idx])) {
            setLine(board, obj, idx, line, current[idx]);
        }
        idx++; line--;

        String gemStr = java.text.NumberFormat.getIntegerInstance().format(gemsManager.getTotalUnits(player));
        current[idx] = ChatColor.LIGHT_PURPLE + "<glyph:purple_orb_icon> " + ChatColor.WHITE + "Gems: " + ChatColor.LIGHT_PURPLE + gemStr;
        if (!current[idx].equals(prev[idx])) {
            setLine(board, obj, idx, line, current[idx]);
        }
        idx++; line--;

        // spacer above calendar
        current[idx] = " ";
        if (!current[idx].equals(prev[idx])) {
            setLine(board, obj, idx, line, current[idx]);
        }
        idx++; line--;

        String date = plugin.getCalendarManager().getSeasonDate();
        current[idx] = ChatColor.WHITE + date;
        if (!current[idx].equals(prev[idx])) {
            setLine(board, obj, idx, line, current[idx]);
        }
        idx++; line--;

        String time = plugin.getCalendarManager().getTimeString() + " " + ChatColor.YELLOW + plugin.getCalendarManager().getWeatherGlyph();
        current[idx] = ChatColor.GRAY + time;
        if (!current[idx].equals(prev[idx])) {
            setLine(board, obj, idx, line, current[idx]);
        }
        idx++; line--;

        // spacer below calendar
        current[idx] = " ";
        if (!current[idx].equals(prev[idx])) {
            setLine(board, obj, idx, line, current[idx]);
        }
        idx++; line--;

        // Siege status
        me.nakilex.levelplugin.guild.siege.GuildSiegeManager siege = plugin.getGuildSiegeManager();
        if (siege != null && siege.isActive(player.getUniqueId())) {
            String cap = siege.getCapturingGuild();
            int prog = siege.getProgress();
            String capText = cap == null ? ChatColor.GRAY + "None" : ChatColor.YELLOW + cap + ChatColor.WHITE + " " + prog + "%";
            current[idx] = ChatColor.RED + "<glyph:flagleft_icon> Siege: " + capText;
            if (!current[idx].equals(prev[idx])) {
                setLine(board, obj, idx, line, current[idx]);
            }
            idx++; line--;

            // spacer below siege line
            current[idx] = " ";
            if (!current[idx].equals(prev[idx])) {
                setLine(board, obj, idx, line, current[idx]);
            }
            idx++; line--;
        }

        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId());
        Quest quest = progress != null ? progress.getQuest() : null;
        String trackedId = questManager.getTrackedQuest(player.getUniqueId());
        if (trackedId != null && (quest == null || !quest.getId().equals(trackedId))) {
            Quest other = questManager.getQuest(trackedId);
            if (other != null) quest = other;
        }
        if (quest != null) {
            current[idx] = ChatColor.GREEN + "Quest: " + ChatColor.WHITE + quest.getName();
            if (!current[idx].equals(prev[idx])) {
                setLine(board, obj, idx, line, current[idx]);
            }
            idx++; line--;

            current[idx] = ChatColor.GREEN + "Progress:";
            if (!current[idx].equals(prev[idx])) {
                setLine(board, obj, idx, line, current[idx]);
            }
            idx++; line--;
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
            current[idx] = ChatColor.GRAY + "- " + desc + ": " + progValue + "/" + currentObj.getAmount();
            if (!current[idx].equals(prev[idx])) {
                setLine(board, obj, idx, line, current[idx]);
            }
            idx++; line--;
        }

        Party party = partyManager.getParty(player.getUniqueId());
        if (party != null) {
            current[idx] = ChatColor.AQUA + "Party:";
            if (!current[idx].equals(prev[idx])) {
                setLine(board, obj, idx, line, current[idx]);
            }
            idx++; line--;
            for (UUID memberId : party.getMembers()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline()) {
                    int lvl = levelManager.getLevel(member);
                    String hp = (int) member.getHealth() + "/" + (int) member.getMaxHealth();
                    current[idx] = ChatColor.GRAY + "[" + lvl + "] " + ChatColor.WHITE + member.getName() + " " + ChatColor.GRAY + hp + " " + ChatColor.RED + "\u2764";
                    if (!current[idx].equals(prev[idx])) {
                        setLine(board, obj, idx, line, current[idx]);
                    }
                    idx++; line--;
                } else {
                    String name = Bukkit.getOfflinePlayer(memberId).getName();
                    if (name == null) name = "Unknown";
                    current[idx] = ChatColor.GRAY + "- " + name;
                    if (!current[idx].equals(prev[idx])) {
                        setLine(board, obj, idx, line, current[idx]);
                    }
                    idx++; line--;
                }
                if (line <= 1) break;
            }
        }

        if (showTps.contains(player.getUniqueId())) {
            double tps = Bukkit.getTPS()[0];
            current[idx] = ChatColor.DARK_AQUA + "TPS: " + String.format("%.1f", tps);
            if (!current[idx].equals(prev[idx])) {
                setLine(board, obj, idx, line, current[idx]);
            }
            idx++; line--;
        }

        // Apply party and friend glow scoreboard entries if enabled
        plugin.getPartyGlowManager().applyGlowScoreboard(player);
        plugin.getFriendGlowManager().applyGlowScoreboard(player);

        for (int i = idx; i < prev.length; i++) {
            if (prev[i] != null) {
                board.resetScores(entries[i]);
                Team t = board.getTeam("line" + i);
                if (t != null) t.setPrefix("");
            }
        }
        lastLines.put(player.getUniqueId(), current);
    }
}
