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
        Location target = SpellTargetingUtil.resolveTargetGround(caster, 16.0);
        if (target == null) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING,
                    "No target in sight for Sky Ripper.");
            return;
        }
        Vector direction = target.toVector().subtract(caster.getLocation().toVector());
        direction.setY(0.0);
        if (direction.lengthSquared() <= 0.0001) {
            direction = caster.getLocation().getDirection().setY(0.0);
        }
        Vector forward = direction.normalize();
        Location origin = caster.getLocation().clone().add(0.0, 1.0, 0.0);

        new BukkitRunnable() {
            private int hitIndex;

            @Override
            public void run() {
                if (!caster.isOnline() || hitIndex >= 3) {
                    cancel();
                    return;
                }
                double travel = 2.0 + hitIndex * 1.6;
                Location impact = origin.clone().add(forward.clone().multiply(travel));
                Location orientation = caster.getLocation().clone();
                orientation.setDirection(forward.clone());

                ArcSlashCombatUtil.strike(caster, impact, orientation, Particle.CRIT, 5.6, 1.9);
                caster.getWorld().playSound(impact, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.85f, 1.2f + (hitIndex * 0.12f));

                for (LivingEntity targetEntity : SpellEffectUtil.getLivingTargets(impact, 1.9,
                        living -> !living.equals(caster))) {
                    targetEntity.setVelocity(targetEntity.getVelocity().add(new Vector(0.0, 0.18 + hitIndex * 0.08, 0.0)));
                }
                hitIndex++;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }
}
