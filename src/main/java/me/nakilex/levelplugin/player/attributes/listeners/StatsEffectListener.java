package me.nakilex.levelplugin.player.attributes.listeners;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.PlayerStats;
import me.nakilex.levelplugin.mob.utils.SweepAttack;
import me.nakilex.levelplugin.spells.managers.SpellContextManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StatsEffectListener implements Listener {

    private final Random random = new Random();

    // Basic attacks still trail spells but shouldn't feel useless
    // 0.20 ~= half of the original 0.4 scaling
    public static final double BASIC_ATTACK_MULTIPLIER = 0.60;

    // Slightly boost spell damage so they retain the edge over basics
    public static final double SPELL_DAMAGE_MULTIPLIER = 1.10;

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

    /**
     * Records whether the player's last hit should be treated as a critical
     * strike so other systems (chat logs, damage popups, etc.) can query it.
     * Used by spells that determine crit chance outside of this listener.
     */
    public static void recordCrit(Player player, boolean isCrit) {
        if (player != null) {
            lastCritMap.put(player.getUniqueId(), isCrit);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity target  = event.getEntity();

        // Determine if a player is responsible for the damage
        Player player = null;
        SpellContextManager.Context ctx = null; // may hold basic-attack flag
        if (damager instanceof Player p) {
            boolean sweeping = p.hasMetadata(SweepAttack.SWEEP_META);
            ctx = SpellContextManager.peek(p.getUniqueId());
            boolean basic = ctx != null && ctx.basicAttack;
            if (!sweeping && !basic && p.getAttackCooldown() < 1.0f) {
                event.setCancelled(true);
                return;
            }
            player = p;
        } else if (damager instanceof org.bukkit.entity.Projectile proj && proj.getShooter() instanceof Player shooter) {
            // Skip scaling for our own custom projectiles which already embed stats
            if (proj.hasMetadata("ArcherSpell") || proj.hasMetadata("BasicAttack") || proj.hasMetadata("Meteor") || proj.hasMetadata("Shockwave")) {
                player = null;
            } else {
                player = shooter;
                ctx = SpellContextManager.peek(shooter.getUniqueId());
            }
        }

        // ── Outgoing damage (when the damager is a player or their projectile) ──
        if (player != null && !player.hasMetadata(SweepAttack.SWEEP_META)) {
            PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());

            // 1) Strength bonus
            int totalStrength = ps.baseStrength + ps.bonusStrength;
            double finalDamage = event.getDamage() + (totalStrength * 0.5);

            // 2) Technique scaling (overall damage)
            int totalTec = ps.baseTechnique + ps.bonusTechnique;
            finalDamage *= (1.0 + totalTec * 0.003);

            // 3) Dex → crit (diminishing returns)
            int totalDexterity = ps.baseDexterity + ps.bonusDexterity;
            double critChance = (double) totalDexterity / (totalDexterity + 100.0);
            critChance = Math.max(0.0, Math.min(1.0, critChance));

            if (ctx == null) {
                ctx = SpellContextManager.peek(player.getUniqueId());
            }
            boolean isCrit = (ctx != null) ? ctx.isCrit : random.nextDouble() < critChance;
            if (isCrit) finalDamage *= 2;

            // Apply type-based multiplier to keep spells ahead of basics
            double scale = (ctx == null || ctx.basicAttack)
                ? BASIC_ATTACK_MULTIPLIER
                : SPELL_DAMAGE_MULTIPLIER;
            finalDamage *= scale;

            me.nakilex.levelplugin.Main.getPlugin().getLogger().info(
                "[StatsEffect] dmg=" + event.getDamage() + "->" + finalDamage +
                " crit=" + isCrit + " player=" + player.getName());

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

            // 6) Vitality-based damage reduction
            int totalVitality = vs.baseVitality + vs.bonusVitality;
            double percentReduction = (double) totalVitality / (totalVitality + 200.0);
            double reducedDamage = event.getDamage() * (1.0 - percentReduction);

            event.setDamage(reducedDamage);
        }
    }
}
