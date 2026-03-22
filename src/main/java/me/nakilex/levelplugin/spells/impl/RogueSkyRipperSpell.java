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
                Location orientation = caster.getLocation().clone();
                orientation.setDirection(forward.clone());

                double damage = hitIndex < 3 ? 4.6 + (hitIndex * 0.35) : 7.8;
                ArcSlashCombatUtil.strike(caster, impact, orientation, damage, 2.0);
                ArcSlashCombatUtil.strike(caster, impact.clone().add(0.0, 0.22, 0.0), orientation, damage * 0.65, 1.8);
                caster.getWorld().playSound(impact, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.12f + (hitIndex * 0.14f));
                caster.getWorld().spawnParticle(Particle.CLOUD, impact, 8 + hitIndex * 2, 0.28, 0.18, 0.28, 0.02);
                caster.getWorld().spawnParticle(Particle.CRIT, impact, 10 + hitIndex * 2, 0.24, 0.25, 0.24, 0.02);

                for (LivingEntity targetEntity : SpellEffectUtil.getLivingTargets(impact, 1.9,
                        living -> !living.equals(caster))) {
                    targetEntity.setVelocity(targetEntity.getVelocity().add(new Vector(0.0, 0.16 + hitIndex * 0.11, 0.0)));
                }
                hitIndex++;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }
}
