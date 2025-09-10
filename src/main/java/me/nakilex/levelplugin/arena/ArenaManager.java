package me.nakilex.levelplugin.arena;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType.*;

/**
 * Handles ranked arena matchmaking and Elo based rating updates.
 * This manager stores each player's rating (MMR and visible rank points),
 * allows them to join a simple queue and updates ratings when a duel concludes.
 */
public class ArenaManager implements Listener {
    private static final ArenaManager INSTANCE = new ArenaManager();
    public static ArenaManager getInstance() { return INSTANCE; }

    /** Player rating data. */
    public static class Rating {
        public int mmr;
        public int rankPoints;
        Rating(int mmr, int rp) { this.mmr = mmr; this.rankPoints = rp; }
    }

    private final Map<UUID, Rating> ratings = new HashMap<>();
    private final Map<UUID, Long> queue = new HashMap<>();
    private final Map<String, ArenaInstance> matches = new HashMap<>();
    private final Map<UUID, String> playerArena = new HashMap<>();

    private String pairKey(UUID a, UUID b) {
        return a.compareTo(b) < 0 ? a + ":" + b : b + ":" + a;
    }

    private static class ArenaInstance {
        final World world;
        final Map<UUID, Location> returns = new HashMap<>();
        final UUID p1;
        final UUID p2;
        ArenaInstance(World world, UUID p1, UUID p2, Location l1, Location l2) {
            this.world = world;
            this.p1 = p1;
            this.p2 = p2;
            returns.put(p1, l1);
            returns.put(p2, l2);
        }
        UUID other(UUID id) { return id.equals(p1) ? p2 : p1; }
    }

    /** Retrieve or create rating for a player. */
    public Rating getRating(UUID id) {
        return ratings.computeIfAbsent(id, k -> new Rating(1000, 1000));
    }

    /** Set rating values (used during data load). */
    public void setRating(UUID id, int mmr, int rp) {
        ratings.put(id, new Rating(mmr, rp));
    }

    /** Attempt to join the arena queue. */
    public void joinQueue(Player p) {
        if (queue.containsKey(p.getUniqueId())) {
            ChatMessageUtil.send(p, ERROR, "You are already in the arena queue.");
            return;
        }
        queue.put(p.getUniqueId(), System.currentTimeMillis());
        ChatMessageUtil.send(p, SUCCESS, "Joined the arena queue.");
        matchmake();
    }

    /** Leave the arena queue if queued. */
    public void leaveQueue(Player p) {
        if (queue.remove(p.getUniqueId()) != null) {
            ChatMessageUtil.send(p, WARNING, "You left the arena queue.");
        }
    }

    /** Check if a player is currently queued. */
    public boolean queueContains(UUID id) {
        return queue.containsKey(id);
    }

    private void matchmake() {
        if (queue.size() < 2) return;
        List<UUID> ids = new ArrayList<>(queue.keySet());
        ids.sort(Comparator.comparingInt(id -> getRating(id).mmr));
        for (int i = 0; i < ids.size() - 1; i++) {
            UUID a = ids.get(i);
            for (int j = i + 1; j < ids.size(); j++) {
                UUID b = ids.get(j);
                int diff = Math.abs(getRating(a).mmr - getRating(b).mmr);
                if (diff <= 300) {
                    Player pa = Bukkit.getPlayer(a);
                    Player pb = Bukkit.getPlayer(b);
                    if (pa != null && pb != null) {
                        queue.remove(a);
                        queue.remove(b);
                        ChatMessageUtil.send(pa, INFO, "Match found against " + pb.getName() + "!");
                        ChatMessageUtil.send(pb, INFO, "Match found against " + pa.getName() + "!");

                        String key = pairKey(a, b);
                        World world = Main.getInstance().getWorldManager()
                                .createVoidWorld("arena_" + System.currentTimeMillis());
                        if (world == null) return;
                        Location l1 = pa.getLocation();
                        Location l2 = pb.getLocation();
                        ArenaInstance inst = new ArenaInstance(world, a, b, l1, l2);
                        matches.put(key, inst);
                        playerArena.put(a, key);
                        playerArena.put(b, key);

                        pa.teleport(new Location(world, 0, 64, -5));
                        pb.teleport(new Location(world, 0, 64, 5));

                        DuelManager.getInstance().startDuel(a, b);
                    }
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        String key = playerArena.get(id);
        if (key == null) return;
        ArenaInstance inst = matches.get(key);
        if (inst == null) return;
        UUID opp = inst.other(id);
        DuelManager.getInstance().endDuel(id, opp);
        handleDuelResult(opp, id);
    }

    /** Handle duel result; returns true if it was an arena duel. */
    public boolean handleDuelResult(UUID winner, UUID loser) {
        String key = playerArena.remove(winner);
        if (key == null || !key.equals(playerArena.remove(loser))) {
            if (key != null) playerArena.put(winner, key); // put back if mismatch
            return false;
        }
        ArenaInstance inst = matches.remove(key);
        if (inst == null) return false;

        for (var e : inst.returns.entrySet()) {
            UUID id = e.getKey();
            Location back = e.getValue();
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                p.teleport(back);
            } else {
                var pm = me.nakilex.levelplugin.player.profile.ProfileManager.getInstance();
                var cfg = Main.getInstance().getPlayerConfig();
                Integer slot = pm.getActiveSlot(id);
                if (slot != null) {
                    cfg.setProfileLocation(id, slot, back);
                    cfg.savePlayer(id);
                }
            }
        }
        Main.getInstance().getWorldManager().deleteWorld(inst.world.getName());

        Rating win = getRating(winner);
        Rating lose = getRating(loser);
        int newWin = EloUtil.update(win.mmr, lose.mmr, true, false, 1.0);
        int newLose = EloUtil.update(lose.mmr, win.mmr, false, false, 1.0);
        win.mmr = newWin;
        lose.mmr = newLose;
        win.rankPoints = clampRank(win.rankPoints, newWin);
        lose.rankPoints = clampRank(lose.rankPoints, newLose);
        Main.getInstance().getPlayerConfig().savePlayerData(winner);
        Main.getInstance().getPlayerConfig().savePlayerData(loser);
        ChatMessageUtil.send(Bukkit.getPlayer(winner), SUCCESS, "MMR: " + win.mmr);
        ChatMessageUtil.send(Bukkit.getPlayer(loser), ERROR, "MMR: " + lose.mmr);
        if (Main.getInstance().getLeaderboardManager() != null) {
            Main.getInstance().getLeaderboardManager().updateType(me.nakilex.levelplugin.leaderboards.LeaderboardType.ARENA);
        }
        return true;
    }

    private int clampRank(int currentRankPoints, int newMMR) {
        int floor = getTierFloor(currentRankPoints);
        return newMMR < floor ? floor : newMMR;
    }

    /** Determine tier floor based on rank points. */
    public int getTierFloor(int mmr) {
        if (mmr >= 2000) return 2000;
        if (mmr >= 1800) return 1800;
        if (mmr >= 1600) return 1600;
        if (mmr >= 1400) return 1400;
        if (mmr >= 1200) return 1200;
        if (mmr >= 1000) return 1000;
        return 0;
    }

    /** Return formatted tier/division name for the given mmr. */
    public String getTierName(int mmr) {
        if (mmr >= 2000) return "Grandmaster";
        if (mmr >= 1800) return "Master " + roman(division(mmr, 1800, 2000, 3));
        if (mmr >= 1600) return "Diamond " + roman(division(mmr, 1600, 1800, 5));
        if (mmr >= 1400) return "Platinum " + roman(division(mmr, 1400, 1600, 5));
        if (mmr >= 1200) return "Gold " + roman(division(mmr, 1200, 1400, 5));
        if (mmr >= 1000) return "Silver " + roman(division(mmr, 1000, 1200, 5));
        return "Bronze " + roman(division(mmr, 0, 1000, 5));
    }

    private int division(int mmr, int min, int max, int divs) {
        int size = (max - min) / divs;
        int idx = (mmr - min) / size;
        return divs - idx; // 1..divs, higher index => lower division
    }

    private String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> "V";
        };
    }
}
