package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.ArcSlashCombatUtil;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.spells.SpellTargetingUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class RoguePhantomCrossSpell implements SpellHandler {
    private final Main plugin;

    public RoguePhantomCrossSpell(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        LivingEntity target = SpellTargetingUtil.resolveTargetLivingEntity(caster, 14.0, 0.45,
                living -> !living.equals(caster));
        if (target == null) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING,
                    "Look at a target mob for Phantom Cross.");
            return;
        }

        new BukkitRunnable() {
            private int slash;

            @Override
            public void run() {
                if (!caster.isOnline() || !target.isValid() || target.isDead() || slash >= 5) {
                    cancel();
                    return;
                }
                Vector forward = target.getLocation().toVector().subtract(caster.getLocation().toVector()).setY(0.0);
                if (forward.lengthSquared() <= 0.0001) {
                    forward = caster.getLocation().getDirection().setY(0.0);
                }
                forward.normalize();
                Vector right = new Vector(0, 1, 0).crossProduct(forward).normalize();
                double side = (slash % 2 == 0 ? 1.0 : -1.0) * (0.38 + (slash * 0.05));
                Location impact = target.getLocation().clone().add(0.0, 1.0, 0.0).add(right.multiply(side));
                Location echoImpact = impact.clone().add(right.clone().multiply(-side * 0.9)).add(0.0, 0.2, 0.0);
                Location orientation = caster.getLocation().clone();
                orientation.setDirection(forward.clone());
                double damage = 2.7 + (slash * 0.42);
                ArcSlashCombatUtil.strike(caster, impact, orientation, damage, 2.1);
                ArcSlashCombatUtil.strike(caster, echoImpact, orientation, damage * 0.58, 1.6);
                SpellEffectUtil.applyDirectSpellDamage(context.plugin(), caster, target, damage, true);
                caster.getWorld().playSound(impact, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.82f, 1.2f + slash * 0.07f);
                caster.getWorld().spawnParticle(org.bukkit.Particle.CRIT, impact, 8 + slash, 0.2, 0.25, 0.2, 0.01);
                slash++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}
