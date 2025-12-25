package me.nakilex.levelplugin.scoreboard;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.arena.ArenaMode;
import me.nakilex.levelplugin.arena.ArenaQueueManager;
import me.nakilex.levelplugin.arena.rating.ArenaRatingManager;
import me.nakilex.levelplugin.catacombs.CatacombsManager;
import me.nakilex.levelplugin.party.Party;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.quests.def.CultistCullingQuest;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.guild.Guild;
import me.nakilex.levelplugin.guild.GuildManager;
import me.nakilex.levelplugin.guild.quests.GuildQuest;
import me.nakilex.levelplugin.guild.quests.GuildQuestManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.time.Duration;
import java.util.*;

public class PlayerScoreboardManager implements org.bukkit.event.Listener {
    private final Main plugin;
    private final PartyManager partyManager;
    private final QuestManager questManager;
    private final LevelManager levelManager;
    private final ArenaQueueManager arenaQueueManager;
    private final ArenaRatingManager arenaRatingManager;
    private CatacombsManager catacombsManager;

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
        updateBoard(event.getPlayer());
        org.bukkit.entity.Player player = event.getPlayer();
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                updateBoard(player);
            }
        }, 20L);
    }

    @org.bukkit.event.EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        removeBoard(event.getPlayer());
    }

    public PlayerScoreboardManager(Main plugin,
                                   PartyManager partyManager,
                                   QuestManager questManager,
                                   ArenaQueueManager arenaQueueManager,
                                   ArenaRatingManager arenaRatingManager) {
        this.plugin = plugin;
        this.partyManager = partyManager;
        this.questManager = questManager;
        this.levelManager = plugin.getLevelManager();
        this.arenaQueueManager = arenaQueueManager;
        this.arenaRatingManager = arenaRatingManager;
    }

    public void setCatacombsManager(CatacombsManager catacombsManager) {
        this.catacombsManager = catacombsManager;
    }

    public void createBoard(Player player) {
        ScoreboardManager sm = Bukkit.getScoreboardManager();
        if (sm == null) return;
        Scoreboard board = sm.getNewScoreboard();
        Objective obj = board.registerNewObjective("stats", "dummy",
                ChatColor.YELLOW.toString() + ChatColor.BOLD + "Objectives");
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
        UUID id = player.getUniqueId();

        // Determine if we should show a board at all
        me.nakilex.levelplugin.guild.siege.GuildSiegeManager siege = plugin.getGuildSiegeManager();
        boolean siegeActive = siege != null && siege.isActive(id);

        PlayerQuestProgress progress = questManager.getProgress(id);
        Quest quest = progress != null ? progress.getQuest() : null;
        String trackedId = questManager.getTrackedQuest(id);
        if (trackedId != null && (quest == null || !quest.getId().equals(trackedId))) {
            Quest other = questManager.getQuest(trackedId);
            if (other != null) quest = other;
        }
        boolean hasQuest = quest != null;

        String gTrackedId = GuildQuestManager.getInstance().getTrackedQuest(id);
        boolean hasGuildQuest = false;
        if (gTrackedId != null) {
            Guild g = GuildManager.getInstance().getGuild(id);
            if (g != null) {
                for (GuildQuest q : g.getQuests().values()) {
                    if (q.getId().equals(gTrackedId) && q.isAccepted()) { hasGuildQuest = true; break; }
                }
            }
        }

        Party party = partyManager.getParty(id);
        boolean inParty = party != null;

        boolean queueing = arenaQueueManager != null && arenaQueueManager.isQueued(id);

        boolean catacombsActive = catacombsManager != null && catacombsManager.getStage(id) != null;

        boolean showBoard = siegeActive || hasQuest || hasGuildQuest || inParty || queueing || catacombsActive;
        Scoreboard board = boards.get(id);
        if (!showBoard) {
            if (board != null) {
                removeBoard(player);
            }
            return;
        }
        if (board == null) {
            createBoard(player);
            return;
        }

        player.setScoreboard(board);

        Objective obj = board.getObjective("stats");
        if (obj == null) return;
        obj.setDisplayName(ChatColor.YELLOW.toString() + ChatColor.BOLD + "Objectives");

        String[] prev = lastLines.computeIfAbsent(id, k -> new String[entries.length]);
        String[] current = new String[entries.length];

        int line = 15;
        int idx = 0;

        current[idx] = " ";
        if (!current[idx].equals(prev[idx])) {
            setLine(board, obj, idx, line, current[idx]);
        }
        idx++; line--;

        if (player.getWorld().hasStorm()) {
            current[idx] = ChatColor.AQUA + "Fishing Speed: " + ChatColor.WHITE + "+100%";
            if (!current[idx].equals(prev[idx])) {
                setLine(board, obj, idx, line, current[idx]);
            }
            idx++; line--;
        }

        // Siege status
        if (siegeActive) {
            String cap = siege.getCapturingGuild();
            int prog = siege.getProgress();
            String name = cap == null ? ChatColor.WHITE + "None" : ChatColor.WHITE + cap;
            String capText = name + " " + ChatColor.DARK_GRAY + "[" + ChatColor.GRAY + prog + "%" + ChatColor.DARK_GRAY + "]";
            current[idx] = ChatColor.RED + "      Siege: " + capText;
            if (!current[idx].equals(prev[idx])) {
                setLine(board, obj, idx, line, current[idx]);
            }
            idx++; line--;

            current[idx] = ChatColor.RED + "<glyph:flagleft_icon> " + ChatColor.WHITE + "Duration: " + ChatColor.GRAY + siege.getFormattedRemaining();
            if (!current[idx].equals(prev[idx])) {
                setLine(board, obj, idx, line, current[idx]);
            }
            idx++; line--;

            current[idx] = " ";
            if (!current[idx].equals(prev[idx])) {
                setLine(board, obj, idx, line, current[idx]);
            }
            idx++; line--;
        }

        CultistCullingQuest.RitualStatus ritualStatus = CultistCullingQuest.getRitualStatus(player);
        if (ritualStatus != null) {
            current[idx] = ChatColor.DARK_PURPLE + ritualStatus.title();
            if (!current[idx].equals(prev[idx])) {
                setLine(board, obj, idx, line, current[idx]);
            }
            idx++; line--;

            current[idx] = ChatColor.GRAY + "Cultists: " + ChatColor.WHITE + ritualStatus.remaining() + "/" + ritualStatus.target();
            if (!current[idx].equals(prev[idx])) {
                setLine(board, obj, idx, line, current[idx]);
            }
            idx++; line--;

            current[idx] = " ";
            if (!current[idx].equals(prev[idx])) {
                setLine(board, obj, idx, line, current[idx]);
            }
            idx++; line--;
        }

        if (catacombsActive) {
            CatacombsManager.StageStatus status = catacombsManager.getStage(id);
            current[idx] = ChatColor.DARK_PURPLE + "Catacombs";
            if (!current[idx].equals(prev[idx])) {
                setLine(board, obj, idx, line, current[idx]);
            }
            idx++; line--;

            if (status != null) {
                current[idx] = ChatColor.GRAY + "Stage: " + ChatColor.WHITE + status.stage();
                if (!current[idx].equals(prev[idx])) {
                    setLine(board, obj, idx, line, current[idx]);
                }
                idx++; line--;

                current[idx] = ChatColor.GRAY + "Mobs: " + ChatColor.WHITE + status.mobsRemaining();
                if (!current[idx].equals(prev[idx])) {
                    setLine(board, obj, idx, line, current[idx]);
                }
                idx++; line--;

                current[idx] = ChatColor.GRAY + "Time: " + ChatColor.WHITE + formatDuration(Duration.ofSeconds(status.secondsLeft()));
                if (!current[idx].equals(prev[idx])) {
                    setLine(board, obj, idx, line, current[idx]);
                }
                idx++; line--;
            }

            current[idx] = " ";
            if (!current[idx].equals(prev[idx])) {
                setLine(board, obj, idx, line, current[idx]);
            }
            idx++; line--;
        }

        if (queueing) {
            ArenaMode mode = arenaQueueManager.getMode(id).orElse(ArenaMode.ONE_VS_ONE);
            ArenaRatingManager.RatingCategory category = mode.ratingCategory();
            current[idx] = ChatColor.GOLD + mode.displayName() + ChatColor.GRAY + " Queue";
            if (!current[idx].equals(prev[idx])) {
                setLine(board, obj, idx, line, current[idx]);
            }
            idx++; line--;

            int size = arenaQueueManager.getQueuePopulation(mode);
            current[idx] = ChatColor.GRAY + "Players: " + ChatColor.WHITE + size;
            if (!current[idx].equals(prev[idx])) {
                setLine(board, obj, idx, line, current[idx]);
            }
            idx++; line--;

            Duration wait = arenaQueueManager.getWaitDuration(id);
            if (arenaRatingManager != null) {
                int rating = arenaRatingManager.getRating(id, category);
                current[idx] = ChatColor.GRAY + "ELO: " + ChatColor.WHITE + rating + ChatColor.GRAY + " ("
                        + arenaRatingManager.formatTier(rating) + ChatColor.GRAY + ")";
                if (!current[idx].equals(prev[idx])) {
                    setLine(board, obj, idx, line, current[idx]);
                }
                idx++; line--;

                int window = arenaRatingManager.computeMatchWindow(id, wait, category);
                current[idx] = ChatColor.GRAY + "Window: " + ChatColor.WHITE + "±" + window;
                if (!current[idx].equals(prev[idx])) {
                    setLine(board, obj, idx, line, current[idx]);
                }
                idx++; line--;
            }

            current[idx] = ChatColor.GRAY + "Waiting: " + ChatColor.WHITE + formatDuration(wait);
            if (!current[idx].equals(prev[idx])) {
                setLine(board, obj, idx, line, current[idx]);
            }
            idx++; line--;

            current[idx] = " ";
            if (!current[idx].equals(prev[idx])) {
                setLine(board, obj, idx, line, current[idx]);
            }
            idx++; line--;
        }

        if (hasQuest) {
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
                    if (progress.getProgress(i) < quest.getObjectives().get(i).getAmount()) { progIndex = i; break; }
                }
                progValue = progress.getProgress(progIndex);
            }
            QuestObjective currentObj = quest.getObjectives().get(progIndex);
            String desc = questManager.describeObjective(currentObj);
            current[idx] = ChatColor.GRAY + "- " + desc + ": " + ChatColor.WHITE + progValue + "/" + currentObj.getAmount();
            if (!current[idx].equals(prev[idx])) {
                setLine(board, obj, idx, line, current[idx]);
            }
            idx++; line--;
        }

        if (hasGuildQuest) {
            Guild g = GuildManager.getInstance().getGuild(id);
            if (g != null) {
                GuildQuest gq = null;
                for (GuildQuest q : g.getQuests().values()) {
                    if (q.getId().equals(gTrackedId) && q.isAccepted()) { gq = q; break; }
                }
                if (gq != null) {
                    current[idx] = ChatColor.RED + "Guild Quest: " + ChatColor.WHITE + gq.getName();
                    if (!current[idx].equals(prev[idx])) {
                        setLine(board, obj, idx, line, current[idx]);
                    }
                    idx++; line--;

                    current[idx] = ChatColor.RED + "Progress:";
                    if (!current[idx].equals(prev[idx])) {
                        setLine(board, obj, idx, line, current[idx]);
                    }
                    idx++; line--;

                    QuestObjective o = gq.getObjective();
                    String desc2 = questManager.describeObjective(o);
                    int total = gq.getTotalContribution();
                    current[idx] = ChatColor.GRAY + "- " + desc2 + ": " + ChatColor.WHITE + total + "/" + o.getAmount();
                    if (!current[idx].equals(prev[idx])) {
                        setLine(board, obj, idx, line, current[idx]);
                    }
                    idx++; line--;
                }
            }
        }
        if (inParty) {
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

        if (showTps.contains(id)) {
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
        lastLines.put(id, current);
    }

    private static String formatDuration(Duration duration) {
        long seconds = Math.max(0L, duration.getSeconds());
        long minutes = seconds / 60;
        long remSeconds = seconds % 60;
        return minutes + ":" + String.format("%02d", remSeconds);
    }
}
