package me.nakilex.levelplugin.effects.listeners;

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
        Entity target = event.getEntity();

        // Outgoing damage modifications if a player is the damager
        if (damager instanceof Player) {
            Player player = (Player) damager;
            PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());

            // 1) Strength bonus
            int totalStrength = ps.baseStrength + ps.bonusStrength;
            double finalDamage = event.getDamage() + (totalStrength * 0.5);

            // 2) Dexterity → crit chance (1% per point)
            int totalDexterity = ps.baseDexterity + ps.bonusDexterity;
            double critChance = totalDexterity * 0.01;

            // 3) Roll for crit
            boolean isCrit = random.nextDouble() < critChance;
            if (isCrit) {
                finalDamage *= 1.5;
            }

            // Record crit status for chat listener
            lastCritMap.put(player.getUniqueId(), isCrit);

            // 4) Apply modified damage
            event.setDamage(finalDamage);
        }

        // Incoming damage modifications if the target is a player
        if (target instanceof Player) {
            Player player = (Player) target;
            PlayerStats vs = StatsManager.getInstance().getPlayerStats(player.getUniqueId());

            // Dodge chance (1% per AGI)
            int totalAgility = vs.baseAgility + vs.bonusAgility;
            double dodgeChance = totalAgility * 0.01;
            if (random.nextDouble() < dodgeChance) {
                event.setCancelled(true);
                return;
            }

            // Defense with diminishing returns using ratio-based formula
            int totalDefence = vs.baseDefenceStat + vs.bonusDefenceStat;
            double percentReduction = totalDefence / (totalDefence + 100.0); // Diminishing returns
            double reducedDamage = event.getDamage() * (1.0 - percentReduction);

            event.setDamage(reducedDamage);
        }
    }
}
