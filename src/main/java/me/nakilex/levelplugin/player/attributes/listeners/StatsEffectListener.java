package me.nakilex.levelplugin.player.attributes.listeners;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.PlayerStats;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StatsEffectListener implements Listener {

    private final Random random = new Random();

    // Track whether each player's last hit was a crit
    private static final Map<UUID, Boolean> lastCritMap = new ConcurrentHashMap<>();

    /**
     * Returns whether the player's last outgoing hit was a crit,
     * and clears the flag so it won't be re‑used.
     */
    public static boolean consumeLastCrit(Player player) {
        Boolean wasCrit = lastCritMap.remove(player.getUniqueId());
        return wasCrit != null && wasCrit;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity target  = event.getEntity();

        // ── Outgoing damage (when the damager is a player) ──
        if (damager instanceof Player) {
            Player player = (Player) damager;
            PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());

            // 1) Strength bonus
            int totalStrength = ps.baseStrength + ps.bonusStrength;
            double finalDamage = event.getDamage() + (totalStrength * 0.5);

            // 2) Dex → crit (diminishing returns)
            int totalDexterity = ps.baseDexterity + ps.bonusDexterity;
            double critChance = (double) totalDexterity / (totalDexterity + 100.0);
            critChance = Math.max(0.0, Math.min(1.0, critChance));

            boolean isCrit = random.nextDouble() < critChance;
            if (isCrit) finalDamage *= 2;

            // Record for chat, etc.
            lastCritMap.put(player.getUniqueId(), isCrit);

            // Apply
            event.setDamage(finalDamage);
        }

        // ── Incoming damage (when the target is a player) ──
        if (target instanceof Player) {
            Player attacked = (Player) target;
            PlayerStats vs = StatsManager.getInstance().getPlayerStats(attacked.getUniqueId());

            // 1) Target’s raw AGI
            int totalAgility = vs.baseAgility + vs.bonusAgility;

            // 2) Attacker’s DEX for accuracy
            int attackerDex = 0;
            if (damager instanceof Player) {
                Player attacker = (Player) damager;
                PlayerStats aps = StatsManager.getInstance().getPlayerStats(attacker.getUniqueId());
                attackerDex = aps.baseDexterity + aps.bonusDexterity;
            }

            // 3) Subtract to get “effective” AGI
            int effectiveAgility = Math.max(0, totalAgility - attackerDex);

            // 4) Re‐compute dodge with diminishing returns
            double dodgeChance = (double) effectiveAgility / (effectiveAgility + 100.0);
            dodgeChance = Math.max(0.0, Math.min(1.0, dodgeChance));

            // 5) Dodge roll
            if (random.nextDouble() < dodgeChance) {
                event.setCancelled(true);
                return;
            }

            // 6) Defense reduction (unchanged)
            int totalDefence = vs.baseDefenceStat + vs.bonusDefenceStat;
            double percentReduction = (double) totalDefence / (totalDefence + 100.0);
            double reducedDamage = event.getDamage() * (1.0 - percentReduction);

            event.setDamage(reducedDamage);
        }
    }
}
