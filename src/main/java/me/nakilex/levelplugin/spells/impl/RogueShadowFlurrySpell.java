package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class RogueShadowFlurrySpell implements SpellHandler {
    private final Main plugin;

    public RogueShadowFlurrySpell(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        Location center = caster.getLocation().clone().add(0.0, 1.0, 0.0);
        caster.getWorld().playSound(center, Sound.ENTITY_BREEZE_INHALE, 0.7f, 0.85f);

        new BukkitRunnable() {
            private int pulse;

            @Override
            public void run() {
                if (!caster.isOnline() || pulse >= 4) {
                    cancel();
                    return;
                }

                Location pulseCenter = caster.getLocation().clone().add(0.0, 1.0, 0.0);
                double radius = 1.5 + (pulse * 0.9);
                SpellEffectUtil.spawnRingParticles(pulseCenter, radius, Particle.CRIT, 26, 0.0);
                caster.getWorld().playSound(pulseCenter, Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                        0.75f, 1.1f + (pulse * 0.1f));

                Set<java.util.UUID> hitTargets = new HashSet<>();
                for (LivingEntity living : SpellEffectUtil.getLivingTargets(pulseCenter, radius + 0.55,
                        target -> !target.equals(caster))) {
                    double distance = living.getLocation().distance(pulseCenter);
                    if (Math.abs(distance - radius) > 0.8 || !hitTargets.add(living.getUniqueId())) {
                        continue;
                    }
                    SpellEffectUtil.applyDirectSpellDamage(plugin, caster, living, 5.2 + (pulse * 1.1), true);
                    SpellEffectUtil.applyStun(living, 10, true);
                }
                pulse++;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }
}
