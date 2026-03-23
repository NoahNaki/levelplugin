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

public class RogueShadowFlurrySpell implements SpellHandler {
    private final Main plugin;

    public RogueShadowFlurrySpell(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        LivingEntity target = SpellTargetingUtil.resolveTargetLivingEntity(caster, 14.0, 0.45,
                living -> !living.equals(caster));
        if (target == null) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING,
                    "Look at a target mob for Shadow Flurry.");
            return;
        }

        new BukkitRunnable() {
            private int slash;

            @Override
            public void run() {
                if (!caster.isOnline() || !target.isValid() || target.isDead() || slash >= 6) {
                    cancel();
                    return;
                }

                Location targetCenter = target.getLocation().clone().add(0.0, 1.0, 0.0);
                Vector facing = target.getLocation().toVector().subtract(caster.getLocation().toVector()).setY(0.0);
                if (facing.lengthSquared() <= 0.0001) {
                    facing = caster.getLocation().getDirection().setY(0.0);
                }
                facing.normalize();
                Vector right = new Vector(0, 1, 0).crossProduct(facing).normalize();

                double side = (slash % 2 == 0 ? 0.55 : -0.55) + (slash * 0.04);
                Location slashPoint = targetCenter.clone().add(right.multiply(side));
                Location orientation = caster.getLocation().clone();
                orientation.setDirection(facing);

                ArcSlashCombatUtil.strike(caster, slashPoint, orientation, 4.6 + (slash * 0.55), 2.0);
                SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, 3.7 + (slash * 0.45), true);

                double lift = slash < 5 ? 0.10 + (slash * 0.018) : 0.30;
                target.setVelocity(target.getVelocity().multiply(0.82).add(new Vector(0.0, lift, 0.0)));

                target.getWorld().spawnParticle(Particle.CRIT, targetCenter, 9, 0.24, 0.2, 0.24, 0.03);
                target.getWorld().playSound(targetCenter, Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                        0.9f, 1.08f + (slash * 0.06f));
                slash++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}
