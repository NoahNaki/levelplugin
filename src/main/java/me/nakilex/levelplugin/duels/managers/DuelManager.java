package me.nakilex.levelplugin.duels.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Singleton to manage all active duels, incoming requests, etc.
 */
public class DuelManager {

    private static final DuelManager instance = new DuelManager();
    public static DuelManager getInstance() { return instance; }

    /**
     * Map of requestTarget -> DuelRequest
     * (i.e., you store requests keyed by the "target" for quick lookup.)
     */
    private final Map<UUID, DuelRequest> duelRequests = new HashMap<>();

    /**
     * Active duels: If two players are dueling, store them in a map
     * or store a separate Duel object. This example uses a Map of p1->p2
     * but you could store a more complex structure if you want.
     */
    private final Map<UUID, UUID> activeDuels = new HashMap<>();

    /**
     * Create a new request from 'requester' to 'target'.
     * Overwrites old request if one already exists for 'target'.
     */
    public void createRequest(Player requester, Player target) {
        DuelRequest request = new DuelRequest(requester.getUniqueId(), target.getUniqueId(), System.currentTimeMillis());
        duelRequests.put(target.getUniqueId(), request);
    }

    /**
     * Checks if a valid request exists for the given target.
     */
    public DuelRequest getRequest(UUID targetId) {
        DuelRequest request = duelRequests.get(targetId);
        if (request == null) return null;

        // Check if it hasn't expired (10 seconds = 10000 ms).
        long now = System.currentTimeMillis();
        if (now - request.getTimestamp() > 10000) {
            // expired
            duelRequests.remove(targetId);
            return null;
        }
        return request;
    }

    /**
     * Accept the request if it exists and isn't expired.
     */
    public boolean acceptRequest(Player target) {
        DuelRequest request = getRequest(target.getUniqueId());
        if (request == null) {
            return false; // No valid request found
        }

        // Start the duel
        startDuel(request.getRequester(), request.getTarget());
        // Remove the request from memory
        duelRequests.remove(target.getUniqueId());
        return true;
    }

    /**
     * Decline the request if it exists.
     */
    public boolean declineRequest(Player target) {
        DuelRequest request = getRequest(target.getUniqueId());
        if (request == null) {
            return false;
        }
        // Just remove from memory
        duelRequests.remove(target.getUniqueId());
        return true;
    }

    /**
     * Start the duel between two player UUIDs.
     */
    public void startDuel(UUID p1, UUID p2) {
        activeDuels.put(p1, p2);
        activeDuels.put(p2, p1);

        Player player1 = Bukkit.getPlayer(p1);
        Player player2 = Bukkit.getPlayer(p2);

        if (player1 != null && player2 != null) {
            player1.sendMessage("§aDuel started with " + player2.getName() + "!");
            player2.sendMessage("§aDuel started with " + player1.getName() + "!");
        }
    }

    /**
     * End a duel between these two players, restore HP/mana, etc.
     */
    public void endDuel(UUID p1, UUID p2) {
        activeDuels.remove(p1);
        activeDuels.remove(p2);

        Player player1 = Bukkit.getPlayer(p1);
        Player player2 = Bukkit.getPlayer(p2);

        if (player1 != null) {
            restorePlayer(player1);
            player1.sendMessage("§cYour duel has ended!");
        }
        if (player2 != null) {
            restorePlayer(player2);
            player2.sendMessage("§cYour duel has ended!");
        }
    }

    /**
     * Restore HP, Mana, etc. Adjust to your plugin’s actual mechanics.
     */
    private void restorePlayer(Player player) {
        player.setHealth(player.getMaxHealth());
        // Pseudocode for your own mana system:
        // MyManaSystem.setMana(player, MyManaSystem.getMaxMana(player));
        // or something similar
    }

    /**
     * Check if these two players are currently in a duel with each other.
     */
    public boolean areInDuel(UUID p1, UUID p2) {
        UUID partner = activeDuels.get(p1);
        return (partner != null && partner.equals(p2));
    }
}
