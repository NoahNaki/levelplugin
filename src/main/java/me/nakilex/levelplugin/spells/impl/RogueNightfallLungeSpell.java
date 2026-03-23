package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.ArcSlashCombatUtil;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.spells.SpellTargetingUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class RogueNightfallLungeSpell implements SpellHandler {
    private final Main plugin;

    public RogueNightfallLungeSpell(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        LivingEntity target = SpellTargetingUtil.resolveTargetLivingEntity(caster, 16.0, 0.45,
                living -> !living.equals(caster));
        if (target == null) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING,
                    "Look at a target mob for Nightfall Lunge.");
            return;
        }

        new BukkitRunnable() {
            private int strike;

            @Override
            public void run() {
                if (!caster.isOnline() || !target.isValid() || target.isDead() || strike >= 3) {
                    cancel();
                    return;
                }

                Vector toTarget = target.getLocation().toVector().subtract(caster.getLocation().toVector());
                Vector dash = toTarget.clone().setY(0.0);
                if (dash.lengthSquared() > 0.0001) {
                    caster.setVelocity(dash.normalize().multiply(0.74).setY(0.15));
                }

                Vector facing = target.getLocation().toVector().subtract(caster.getLocation().toVector()).setY(0.0);
                if (facing.lengthSquared() <= 0.0001) {
                    facing = caster.getLocation().getDirection().setY(0.0);
                }
                facing.normalize();
                Vector right = new Vector(0, 1, 0).crossProduct(facing).normalize();

                double side = (strike % 2 == 0 ? 0.62 : -0.62);
                Location impact = target.getLocation().clone().add(0.0, 1.0, 0.0).add(right.multiply(side));
                Location orientation = caster.getLocation().clone();
                orientation.setDirection(facing.clone());

                double damage = 5.6 + (strike * 1.8);
                ArcSlashCombatUtil.applyConeDamage(caster, impact, facing, 3.5, 82.0, 2.4, damage);
                SpellEffectUtil.applyDirectSpellDamage(context.plugin(), caster, target, damage + 1.2, true);

                caster.getWorld().spawnParticle(Particle.LARGE_SMOKE, impact, 14 + strike * 4,
                        0.32, 0.22, 0.32, 0.01);
                caster.getWorld().spawnParticle(Particle.CRIT, impact, 12 + strike * 3,
                        0.24, 0.14, 0.24, 0.03);
                caster.getWorld().playSound(impact, Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                        0.95f, 0.95f + (strike * 0.06f));

                ArcSlashCombatUtil.strike(caster, impact, orientation, 3.3 + strike, 1.85);
                strike++;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }
}
