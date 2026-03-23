package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.ArcherArrowUtil;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class ArcherHomingBarrageSpell implements SpellHandler {
    private final Main plugin;
    private final int arrowCount;
    private final long intervalTicks;
    private final double arrowSpeed;
    private final double homingStrength;
    private final double baseDamage;
    private final double dexScale;

    public ArcherHomingBarrageSpell(Main plugin,
                                    int arrowCount,
                                    long intervalTicks,
                                    double arrowSpeed,
                                    double homingStrength,
                                    double baseDamage,
                                    double dexScale) {
        this.plugin = plugin;
        this.arrowCount = Math.max(1, arrowCount);
        this.intervalTicks = Math.max(1L, intervalTicks);
        this.arrowSpeed = Math.max(0.2, arrowSpeed);
        this.homingStrength = Math.max(0.02, Math.min(0.45, homingStrength));
        this.baseDamage = Math.max(0.1, baseDamage);
        this.dexScale = Math.max(0.0, dexScale);
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        double damage = SpellEffectUtil.computeDexTecScaledDamage(caster, baseDamage, dexScale, 0.001);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_SKELETON_SHOOT, 0.85f, 1.28f);

        new BukkitRunnable() {
            private int fired;

            @Override
            public void run() {
                if (!caster.isOnline() || fired >= arrowCount) {
                    cancel();
                    return;
                }
                shootHomingArrow(caster, damage);
                fired++;
            }
        }.runTaskTimer(plugin, 0L, intervalTicks);
    }

    private void shootHomingArrow(Player caster, double damage) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vector direction = caster.getEyeLocation().getDirection().clone();
        direction.add(new Vector(
                random.nextDouble(-0.07, 0.07),
                random.nextDouble(-0.04, 0.04),
                random.nextDouble(-0.07, 0.07)));

        Arrow arrow = ArcherArrowUtil.launchClassArrow(plugin, caster, direction, arrowSpeed, damage);
        if (arrow == null) {
            return;
        }
        caster.getWorld().spawnParticle(Particle.CRIT, caster.getEyeLocation(), 5, 0.12, 0.08, 0.12, 0.02);
        ArcherArrowUtil.attachHomingTask(plugin, caster, arrow, homingStrength, 36, 18.0, 0.7);
    }
}
