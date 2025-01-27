package me.nakilex.levelplugin.duels.managers;

import me.nakilex.levelplugin.utils.ChatFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class DuelManager {
    private static final DuelManager instance = new DuelManager();
    public static DuelManager getInstance() { return instance; }

    private final Map<UUID, DuelRequest> duelRequests = new HashMap<>();

    // activeDuels: p1->p2 and p2->p1
    private final Map<UUID, UUID> activeDuels = new HashMap<>();

    public void createRequest(Player requester, Player target) {
        DuelRequest req = new DuelRequest(requester.getUniqueId(), target.getUniqueId(), System.currentTimeMillis());
        duelRequests.put(target.getUniqueId(), req);
        // (Do NOT send messages here, to avoid duplicates.)
    }

    public DuelRequest getRequest(UUID targetId) {
        DuelRequest request = duelRequests.get(targetId);
        if (request == null) return null;
        // check expiry
        long now = System.currentTimeMillis();
        if (now - request.getTimestamp() > 10000) {
            // expired
            duelRequests.remove(targetId);
            return null;
        }
        return request;
    }

    public boolean acceptRequest(Player target) {
        DuelRequest request = getRequest(target.getUniqueId());
        if (request == null) return false;

        // Remove the request so it can’t be reused
        duelRequests.remove(target.getUniqueId());

        Player requester = Bukkit.getPlayer(request.getRequester());
        if (requester == null || !requester.isOnline() || !target.isOnline()) {
            return false;
        }

        // 1) Immediately notify them that it was accepted (if desired)
        ChatFormatter.sendCenteredMessage(target,
            "§aYou accepted " + requester.getName() + "’s duel request!");
        ChatFormatter.sendCenteredMessage(requester,
            "§aYour duel request was accepted by " + target.getName() + "!");

        // 2) Begin a 5-second countdown, THEN call startDuel(...)
        new BukkitRunnable() {
            int countdown = 5;
            @Override
            public void run() {
                if (!requester.isOnline() || !target.isOnline()) {
                    cancel();
                    return;
                }
                if (countdown <= 0) {
                    startDuel(requester.getUniqueId(), target.getUniqueId());
                    cancel();
                    return;
                }
                ChatFormatter.sendCenteredMessage(requester,
                    "§eDuel starts in " + countdown + " seconds...");
                ChatFormatter.sendCenteredMessage(target,
                    "§eDuel starts in " + countdown + " seconds...");
                countdown--;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 20L);

        return true;
    }


    public boolean declineRequest(Player target) {
        DuelRequest req = getRequest(target.getUniqueId());
        if (req == null) return false;
        duelRequests.remove(target.getUniqueId());
        return true;
    }

    public void startDuel(UUID p1, UUID p2) {
        activeDuels.put(p1, p2);
        activeDuels.put(p2, p1);

        Player player1 = Bukkit.getPlayer(p1);
        Player player2 = Bukkit.getPlayer(p2);

        if (player1 != null && player2 != null) {
            ChatFormatter.sendCenteredMessage(player1, "§aDuel started with " + player2.getName() + "!");
            ChatFormatter.sendCenteredMessage(player2, "§aDuel started with " + player1.getName() + "!");
        }

        // Start a repeating task to check distance. If > 100 blocks, end the duel.
        new BukkitRunnable() {
            @Override
            public void run() {
                Player pA = Bukkit.getPlayer(p1);
                Player pB = Bukkit.getPlayer(p2);
                if (pA == null || pB == null || !pA.isOnline() || !pB.isOnline()) {
                    // One player left the server, so end duel
                    endDuel(p1, p2);
                    cancel();
                    return;
                }
                // measure distance
                double dist = pA.getLocation().distance(pB.getLocation());
                if (dist > 100) {
                    // End the duel
                    ChatFormatter.sendCenteredMessage(pA,
                        "§cDuel ended because you were too far apart!");
                    ChatFormatter.sendCenteredMessage(pB,
                        "§cDuel ended because you were too far apart!");
                    endDuel(p1, p2);
                    cancel();
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 40L);
        // checks every 2 seconds
    }

    public void endDuel(UUID p1, UUID p2) {
        activeDuels.remove(p1);
        activeDuels.remove(p2);

        Player player1 = Bukkit.getPlayer(p1);
        Player player2 = Bukkit.getPlayer(p2);
        if (player1 != null) {
            restorePlayer(player1);
            ChatFormatter.sendCenteredMessage(player1, "§cYour duel has ended!");
        }
        if (player2 != null) {
            restorePlayer(player2);
            ChatFormatter.sendCenteredMessage(player2, "§cYour duel has ended!");
        }
    }

    private void restorePlayer(Player p) {
        p.setHealth(p.getMaxHealth());
        // Restore mana, etc. if you have a system for that.
    }

    public boolean areInDuel(UUID p1, UUID p2) {
        UUID partner = activeDuels.get(p1);
        return partner != null && partner.equals(p2);
    }

    /**
     * Are they in a duel with ANYONE?
     */
    public boolean areInAnyDuel(Player p) {
        return activeDuels.containsKey(p.getUniqueId());
    }
}
