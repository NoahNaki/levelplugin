package me.nakilex.levelplugin.spells.effect.mage;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.party.Party;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.PlayerStats;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class HealEffect implements SpellEffect {

    // Prevent duplicate casts in the same tick
    private static final Set<UUID> healingLock = Collections.newSetFromMap(new WeakHashMap<>());

    @Override
    public void apply(SpellCastContext ctx) {
        Player caster   = ctx.getPlayer();
        UUID   casterId = caster.getUniqueId();
        Main   plugin   = Main.getInstance();

        // single-invoke guard
        if (!healingLock.add(casterId)) return;
        Bukkit.getScheduler().runTask(plugin, () -> healingLock.remove(casterId));

        // compute base heal by intelligence
        PlayerStats stats    = StatsManager.getInstance().getPlayerStats(casterId);
        int         intel    = stats.baseIntelligence + stats.bonusIntelligence;
        double      baseHeal = 10.0 + intel * 0.5;

        // rune parameters
        double  bonusHealPercent = parseDouble(ctx.getExtraParam("bonusHealPercent"), 0.0);
        boolean healOverTime     = parseBoolean(ctx.getExtraParam("healOverTime"), false);
        int     hotDuration      = Math.max(1, parseInt(ctx.getExtraParam("hotDuration"), 5));   // seconds
        int     hotInterval      = Math.max(1, parseInt(ctx.getExtraParam("hotInterval"), 20));// ticks
        boolean cleanseDebuffs   = parseBoolean(ctx.getExtraParam("cleanseDebuffs"), false);

        // sum aoeDamage modifiers if multiple runes are applied
        double aoeDamage = 0.0;
        Object dmgObj = ctx.getExtraParam("aoeDamage");
        if (dmgObj instanceof List) {
            for (Object o : (List<?>) dmgObj) {
                aoeDamage += parseDouble(o, 0.0);
            }
        } else {
            aoeDamage = parseDouble(dmgObj, 0.0);
        }

        // sum aoeRange modifiers so they stack
        double aoeRange = 0.0;
        Object rangeObj = ctx.getExtraParam("aoeRange");
        if (rangeObj instanceof List) {
            for (Object o : (List<?>) rangeObj) {
                aoeRange += parseDouble(o, 0.0);
            }
        } else {
            aoeRange = parseDouble(rangeObj, 0.0);
        }


        // final heal amount
        double healAmount = baseHeal * (1.0 + bonusHealPercent/100.0);
        String spellName  = ctx.getBaseSpell().getDisplayName();

        // gather targets: self + party (skip duel opponents)
        List<Player> toHeal = new ArrayList<>();
        toHeal.add(caster);
        Party party = plugin.getPartyManager().getParty(casterId);
        DuelManager dm = DuelManager.getInstance();
        if (party != null) {
            for (UUID mid : party.getMembers()) {
                if (mid.equals(casterId)) continue;
                if (dm.areInDuel(casterId, mid)) continue;
                Player member = Bukkit.getPlayer(mid);
                if (member != null) toHeal.add(member);
            }
        }

        // cleanse debuffs
        if (cleanseDebuffs) {
            toHeal.forEach(t ->
                Arrays.asList(
                    org.bukkit.potion.PotionEffectType.POISON,
                    PotionEffectType.SLOWNESS,
                    org.bukkit.potion.PotionEffectType.WEAKNESS,
                    org.bukkit.potion.PotionEffectType.BLINDNESS
                ).forEach(type -> {
                    if (t.hasPotionEffect(type)) t.removePotionEffect(type);
                })
            );
        }

        if (healOverTime) {
            int runs       = (hotDuration * 20) / hotInterval;
            double tickHeal = healAmount / runs;

            double finalAoeDamage = aoeDamage;
            double finalAoeRange = aoeRange;
            double finalAoeRange1 = aoeRange;
            double finalAoeRange2 = aoeRange;
            new BukkitRunnable() {
                int count = 0;
                @Override public void run() {
                    if (count++ >= runs) {
                        cancel();
                        return;
                    }

                    // Display AoE ring of particles
                    int particleCount = 36;
                    for (int i = 0; i < particleCount; i++) {
                        double angle = 2 * Math.PI * i / particleCount;
                        double px = caster.getLocation().getX() + Math.cos(angle) * finalAoeRange;
                        double pz = caster.getLocation().getZ() + Math.sin(angle) * finalAoeRange;
                        double py = caster.getLocation().getY();
                        caster.getWorld().spawnParticle(Particle.HEART, px, py, pz, 1, 0, 0, 0, 0);
                    }

                    // HEAL YOUR FRIENDS
                    for (Player t : toHeal) {
                        double before = t.getHealth();
                        double maxHp  = Objects.requireNonNull(
                            t.getAttribute(Attribute.MAX_HEALTH)
                        ).getValue();
                        double after  = Math.min(before + tickHeal, maxHp);
                        t.setHealth(after);
                        t.spawnParticle(Particle.HEART, t.getLocation(), 5, 0.5, 0.5, 0.5, 0.1);
                        t.sendMessage(String.format(
                            "§a%s healed you for %d ❤",
                            spellName, Math.round(after - before)
                        ));
                    }

                    // DAMAGE ENEMIES IN AOE (skip allies & duel partners)
                    if (finalAoeDamage > 0 && finalAoeRange > 0) {
                        for (Entity ent : caster.getWorld()
                            .getNearbyEntities(caster.getLocation(), finalAoeRange, finalAoeRange1, finalAoeRange2)) {
                            if (!(ent instanceof Player)) continue;
                            Player enemy = (Player) ent;
                            if (toHeal.contains(enemy)) continue;
                            if (dm.areInDuel(casterId, enemy.getUniqueId())) continue;
                            enemy.damage(finalAoeDamage, caster);
                        }
                    }
                }
            }.runTaskTimer(plugin, 0L, hotInterval);

        } else {
            // immediate heal
            for (Player t : toHeal) {
                double before = t.getHealth();
                double maxHp  = Objects.requireNonNull(
                    t.getAttribute(Attribute.MAX_HEALTH)
                ).getValue();
                double after  = Math.min(before + healAmount, maxHp);
                t.setHealth(after);

                t.spawnParticle(Particle.HAPPY_VILLAGER, t.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                t.playSound(t.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1f, 1f);

                t.sendMessage(String.format(
                    "§a%s §fhealed you for §a%d §fhealth!",
                    spellName, Math.round(after - before)
                ));
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Helper parsers
    private double parseDouble(Object p, double d) {
        if (p instanceof Number) return ((Number)p).doubleValue();
        if (p instanceof String) {
            try { return Double.parseDouble((String)p); }
            catch (Exception ignored) {}
        }
        return d;
    }
    private int parseInt(Object p, int d) {
        if (p instanceof Number) return ((Number)p).intValue();
        if (p instanceof String) {
            try { return Integer.parseInt((String)p); }
            catch (Exception ignored) {}
        }
        return d;
    }
    private boolean parseBoolean(Object p, boolean d) {
        if (p instanceof Boolean) return (Boolean)p;
        if (p instanceof String)  return Boolean.parseBoolean((String)p);
        return d;
    }
}
