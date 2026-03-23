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
            private int tick;

            @Override
            public void run() {
                if (!caster.isOnline() || !target.isValid() || target.isDead() || tick >= 10) {
                    cancel();
                    return;
                }

                Location center = target.getLocation().clone().add(0.0, 1.0, 0.0);
                double angle = (Math.PI * 2.0 * tick) / 10.0;
                double radius = 1.3;
                Location orbit = center.clone().add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
                Vector inward = center.toVector().subtract(orbit.toVector()).normalize();

                Location orientation = caster.getLocation().clone();
                orientation.setDirection(inward);
                ArcSlashCombatUtil.strike(caster, orbit, orientation, 4.9 + (tick * 0.22), 1.9);

                if (tick % 2 == 0) {
                    SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, 6.0 + (tick * 0.35), true);
                    target.setVelocity(target.getVelocity().multiply(0.85).add(new Vector(0.0, 0.08, 0.0)));
                }

                center.getWorld().spawnParticle(Particle.CRIT, orbit, 7, 0.14, 0.12, 0.14, 0.02);
                center.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.78f, 0.92f + (tick * 0.03f));

                if (tick == 9) {
                    SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, 10.5, true);
                    center.getWorld().spawnParticle(Particle.CRIT, center, 18, 0.32, 0.22, 0.32, 0.04);
                    center.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.86f);
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}
