package me.nakilex.levelplugin.player.attributes.listeners;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.PlayerStats;
import me.nakilex.levelplugin.mob.utils.SweepAttack;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.pet.PetEffectType;
import me.nakilex.levelplugin.pet.PetManager;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StatsEffectListener implements Listener {

    private final Random random = new Random();

    // Basic attacks scaling
    public static final double BASIC_ATTACK_MULTIPLIER = 0.60;

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
        if (damager instanceof Player p) {
            boolean sweeping = p.hasMetadata(SweepAttack.SWEEP_META);
            if (!sweeping && p.getAttackCooldown() < 1.0f) {
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
            }
        }

        // ── Outgoing damage (when the damager is a player or their projectile) ──
        if (player != null && !player.hasMetadata(SweepAttack.SWEEP_META)) {
            PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());

            double finalDamage = event.getDamage();
            int totalTec = ps.baseTechnique + ps.bonusTechnique;

            // Use Intelligence instead of Strength for all mage attacks
            boolean isMage = ClassUtil.isMageFamily(
                    PlayerClassManager.getInstance().getPlayerClass(player));
            if (isMage) {
                int totalInt = ps.baseIntelligence + ps.bonusIntelligence;
                finalDamage += totalInt * 0.5;
            } else {
                int totalStrength = ps.baseStrength + ps.bonusStrength;
                finalDamage += totalStrength * 0.5;
            }

            // Technique scaling (overall damage)
            finalDamage *= (1.0 + totalTec * 0.001);

            // Dex → crit (diminishing returns)
            int totalDexterity = ps.baseDexterity + ps.bonusDexterity;
            double critChance = (double) totalDexterity / (totalDexterity + 100.0);
            critChance = Math.max(0.0, Math.min(1.0, critChance));
            PetManager petManager = Main.getInstance().getPetManager();
            if (petManager != null) {
                double critBonus = petManager.getActiveEffectValue(player.getUniqueId(), PetEffectType.CRIT_CHANCE);
                critChance = Math.min(1.0, critChance + Math.max(0.0, critBonus));
            }

            boolean isCrit = random.nextDouble() < critChance;
            if (isCrit) finalDamage *= 2;

            finalDamage *= BASIC_ATTACK_MULTIPLIER;

            if (petManager != null) {
                double damageBoost = petManager.getActiveEffectValue(player.getUniqueId(), PetEffectType.DAMAGE_BOOST);
                if (damageBoost > 0.0) {
                    finalDamage *= (1.0 + damageBoost);
                }
                double coinCap = petManager.getActiveEffectValue(player.getUniqueId(), PetEffectType.COIN_DAMAGE);
                if (coinCap > 0.0) {
                    EconomyManager economyManager = Main.getInstance().getEconomyManager();
                    if (economyManager != null) {
                        int coins = economyManager.getBalance(player);
                        double coinBonus = (coins / 1000.0) * 0.01;
                        double cap = Math.min(2.0, coinCap);
                        finalDamage *= (1.0 + Math.min(coinBonus, cap));
                    }
                }
                double firstStrike = petManager.getActiveEffectValue(player.getUniqueId(), PetEffectType.FIRST_STRIKE);
                if (firstStrike > 0.0 && target instanceof LivingEntity livingTarget) {
                    double maxHealth = livingTarget.getMaxHealth();
                    if (maxHealth > 0.0 && livingTarget.getHealth() >= maxHealth - 0.01) {
                        finalDamage *= (1.0 + Math.min(0.3, firstStrike));
                    }
                }
                double executeBoost = petManager.getActiveEffectValue(player.getUniqueId(), PetEffectType.EXECUTE);
                if (executeBoost > 0.0 && target instanceof LivingEntity livingTarget) {
                    double maxHealth = livingTarget.getMaxHealth();
                    if (maxHealth > 0.0 && livingTarget.getHealth() / maxHealth <= PetEffectType.EXECUTE.executeThreshold()) {
                        finalDamage *= (1.0 + executeBoost);
                    }
                }
                double executeThreshold = petManager.getActiveEffectValue(player.getUniqueId(), PetEffectType.EXECUTE_NON_BOSS);
                if (executeThreshold > 0.0 && target instanceof LivingEntity livingTarget) {
                    if (!isBossEntity(livingTarget)) {
                        double maxHealth = livingTarget.getMaxHealth();
                        double threshold = Math.min(0.25, executeThreshold);
                        if (maxHealth > 0.0 && livingTarget.getHealth() / maxHealth <= threshold) {
                            finalDamage = Math.max(finalDamage, livingTarget.getHealth());
                        }
                    }
                }
                double lastStandBoost = petManager.getLastStandDamageBoost(player.getUniqueId());
                if (lastStandBoost > 0.0) {
                    finalDamage *= (1.0 + lastStandBoost);
                }
            }

//            me.nakilex.levelplugin.Main.getPlugin().getLogger().info(
//                "[StatsEffect] dmg=" + event.getDamage() + "->" + finalDamage +
//                " crit=" + isCrit + " player=" + player.getName());

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

            // 5) Dodge roll - a successful dodge now mitigates 90% of the damage
            double incoming = event.getDamage();
            if (random.nextDouble() < dodgeChance) {
                incoming *= 0.1; // take only 10% damage on dodge
            }

            // 6) Vitality-based damage reduction
            int totalVitality = vs.baseVitality + vs.bonusVitality;
            double percentReduction = (double) totalVitality / (totalVitality + 200.0);
            incoming *= (1.0 - percentReduction);

            PetManager petManager = Main.getInstance().getPetManager();
            if (petManager != null) {
                double petReduction = petManager.getActiveEffectValue(attacked.getUniqueId(), PetEffectType.DAMAGE_REDUCTION);
                if (petReduction > 0.0) {
                    incoming *= Math.max(0.0, 1.0 - petReduction);
                }
            }

            event.setDamage(incoming);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCombatTag(EntityDamageByEntityEvent event) {
        Player attacker = resolveCombatAttacker(event.getDamager());
        if (attacker != null) {
            StatsManager.getInstance().markCombat(attacker.getUniqueId());
        }

        if (event.getEntity() instanceof Player victim) {
            StatsManager.getInstance().markCombat(victim.getUniqueId());
        }
    }

    private Player resolveCombatAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    private boolean isBossEntity(LivingEntity entity) {
        if (entity instanceof org.bukkit.entity.Boss) {
            return true;
        }
        if (entity.getScoreboardTags().contains("field_boss")) {
            return true;
        }
        return entity.getScoreboardTags().contains("dungeon_boss");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFireTick(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FIRE_TICK) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        double maxHealth = player.getMaxHealth();
        if (maxHealth <= 0) {
            return;
        }
        event.setDamage(maxHealth * 0.03);
    }
}
