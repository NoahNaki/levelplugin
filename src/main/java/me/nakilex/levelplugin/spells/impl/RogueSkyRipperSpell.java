package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class RogueSkyRipperSpell implements SpellHandler {
    private final Main plugin;

    public RogueSkyRipperSpell(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        Vector forward = caster.getLocation().getDirection().setY(0.0);
        if (forward.lengthSquared() <= 0.0001) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING,
                    "No forward direction for Sky Ripper.");
            return;
        }
        forward.normalize();
        Location origin = caster.getLocation().clone().add(0.0, 1.0, 0.0);

        new BukkitRunnable() {
            private int hitIndex;

            @Override
            public void run() {
                if (!caster.isOnline() || hitIndex >= 4) {
                    cancel();
                    return;
                }
                double travel = 1.8 + hitIndex * 1.45;
                Location impact = origin.clone().add(forward.clone().multiply(travel));
                double damage = hitIndex < 3 ? 4.8 + (hitIndex * 0.35) : 8.4;
                SpellEffectUtil.applyAreaDamage(caster, impact, 2.0, damage);
                caster.getWorld().playSound(impact, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.12f + (hitIndex * 0.14f));
                caster.getWorld().spawnParticle(Particle.CLOUD, impact, 10 + hitIndex * 3, 0.32, 0.20, 0.32, 0.03);
                caster.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, impact, 8 + hitIndex * 2, 0.22, 0.26, 0.22, 0.03);
                for (int i = 0; i < 12; i++) {
                    double angle = (Math.PI * 2.0 * i / 12.0) + (hitIndex * 0.36);
                    double radius = 0.55 + hitIndex * 0.18;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    caster.getWorld().spawnParticle(Particle.END_ROD, impact.clone().add(x, 0.25 + (i % 3) * 0.06, z),
                            1, 0.0, 0.0, 0.0, 0.0);
                }

                for (LivingEntity targetEntity : SpellEffectUtil.getLivingTargets(impact, 2.0,
                        living -> !living.equals(caster))) {
                    targetEntity.setVelocity(targetEntity.getVelocity().add(new Vector(0.0, 0.16 + hitIndex * 0.11, 0.0)));
                }
                hitIndex++;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }
}
