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
                Location orientation = caster.getLocation().clone();
                orientation.setDirection(forward.clone());
                double damage = 2.7 + (slash * 0.42);
                ArcSlashCombatUtil.strike(caster, impact, orientation, Particle.SWEEP_ATTACK, damage, 2.1);
                SpellEffectUtil.applyDirectSpellDamage(context.plugin(), caster, target, damage, true);
                caster.getWorld().playSound(impact, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.82f, 1.2f + slash * 0.07f);
                slash++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}
