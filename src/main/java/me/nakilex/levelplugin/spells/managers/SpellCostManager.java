package me.nakilex.levelplugin.spells.managers;

import me.nakilex.levelplugin.Main;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages dynamic mana costs per player, per spell. Costs increase by a fixed
 * percent of the base cost on consecutive casts, and reset after a delay.
 */
public class SpellCostManager {
    private static SpellCostManager instance;

    public static SpellCostManager getInstance() {
        if (instance == null) {
            instance = new SpellCostManager();
        }
        return instance;
    }

    // Tracks current cost (might be > baseCost) per player/spellKey
    private final Map<UUID, Map<String, Double>> currentCost = new ConcurrentHashMap<>();
    // Tracks last cast timestamp per player/spellKey
    private final Map<UUID, Map<String, Long>> lastCastTime = new ConcurrentHashMap<>();

    // Settings
    private static final double INCREMENT_PERCENT = 0.5;  // +50% of base each time
    private static final long RESET_DELAY_MS    = 5000;  // 5 seconds

    /**
     * Returns the mana cost for this player's next cast of spellKey, resetting
     * to baseCost if they haven't cast within RESET_DELAY_MS.
     */
    public double getManaCost(Player player, String spellKey, double baseCost) {
        UUID pid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Ensure maps exist
        currentCost.computeIfAbsent(pid, k -> new ConcurrentHashMap<>());
        lastCastTime.computeIfAbsent(pid, k -> new ConcurrentHashMap<>());

        Map<String, Double> costMap = currentCost.get(pid);
        Map<String, Long> timeMap = lastCastTime.get(pid);

        Long last = timeMap.get(spellKey);
        if (last == null || now - last > RESET_DELAY_MS) {
            // Reset
            costMap.put(spellKey, baseCost);
            timeMap.put(spellKey, now);
            return baseCost;
        }

        // Return the current escalated cost
        return costMap.getOrDefault(spellKey, baseCost);
    }

    /**
     * Call this after a successful cast to increment the stored cost and update timestamp.
     */
    public void recordCast(Player player, String spellKey, double baseCost) {
        UUID pid = player.getUniqueId();
        Map<String, Double> costMap = currentCost.computeIfAbsent(pid, k -> new ConcurrentHashMap<>());
        Map<String, Long> timeMap = lastCastTime.computeIfAbsent(pid, k -> new ConcurrentHashMap<>());

        double oldCost = costMap.getOrDefault(spellKey, baseCost);
        double newCost = oldCost + (baseCost * INCREMENT_PERCENT);
        costMap.put(spellKey, newCost);
        timeMap.put(spellKey, System.currentTimeMillis());

        // Optional: schedule auto-reset after RESET_DELAY_MS to clean maps
        new BukkitRunnable() {
            @Override public void run() {
                long t = System.currentTimeMillis();
                Long lastTime = timeMap.get(spellKey);
                if (lastTime != null && t - lastTime >= RESET_DELAY_MS) {
                    costMap.remove(spellKey);
                    timeMap.remove(spellKey);
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), RESET_DELAY_MS / 50, RESET_DELAY_MS / 50);
    }

    /**
     * Clears all stored costs for a player (e.g. on logout).
     */
    public void clearPlayer(Player player) {
        UUID pid = player.getUniqueId();
        currentCost.remove(pid);
        lastCastTime.remove(pid);
    }
}
