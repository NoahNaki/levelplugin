package me.nakilex.levelplugin.spells.managers;

import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import me.nakilex.levelplugin.Main;

public class ManaCostTracker {
    private static class CostData {
        double currentCost;
        BukkitRunnable resetTask;
    }

    private final Map<UUID, Map<String, CostData>> data = new HashMap<>();
    private final double multiplier;
    private final long resetDelayMs;

    public ManaCostTracker(double multiplier, long resetDelayMs) {
        this.multiplier = multiplier;
        this.resetDelayMs = resetDelayMs;
    }

    /** Call this *before* each cast to get the cost to charge. */
    public double getCost(UUID playerId, String spellId, double baseCost) {
        Map<String, CostData> spells = data.computeIfAbsent(playerId, k -> new HashMap<>());
        CostData cd = spells.computeIfAbsent(spellId, k -> {
            CostData n = new CostData();
            n.currentCost = baseCost;
            return n;
        });
        return cd.currentCost;
    }

    /** Call this *after* a successful cast to bump cost and schedule a reset. */
    public void recordCast(UUID playerId, String spellId, double baseCost) {
        Map<String, CostData> spells = data.get(playerId);
        if (spells == null) return;

        CostData cd = spells.get(spellId);
        if (cd == null) return;

        // cancel existing reset (so resetDelayMs is counted from the *last* cast)
        if (cd.resetTask != null) {
            cd.resetTask.cancel();
        }

        // bump cost
        cd.currentCost *= multiplier;

        // schedule reset
        cd.resetTask = new BukkitRunnable() {
            @Override public void run() {
                cd.currentCost = baseCost;
                spells.remove(spellId);
                if (spells.isEmpty()) {
                    data.remove(playerId);
                }
            }
        };
        cd.resetTask.runTaskLater(
            Main.getInstance(),
            resetDelayMs / 50L  // ticks (1 tick = 50ms)
        );
    }
}
