package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.spells.WarriorCombatUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class WarriorExecutionArcSpell implements SpellHandler {
    private final Main plugin;
    private final double range;
    private final double halfAngle;
    private final double damage;

    public WarriorExecutionArcSpell(Main plugin, double range, double halfAngle, double damage) {
        this.plugin = plugin;
        this.range = range;
        this.halfAngle = halfAngle;
        this.damage = damage;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        int hits = WarriorCombatUtil.strikeCone(plugin, caster, caster.getEyeLocation(), range, halfAngle, damage, 0.34);
        caster.getWorld().spawnParticle(Particle.SWEEP_ATTACK, caster.getLocation().add(0.0, 1.0, 0.0), 1);
        caster.getWorld().spawnParticle(Particle.FLAME, caster.getLocation().add(0.0, 1.1, 0.0),
                18, 0.45, 0.25, 0.45, 0.01);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.95f, 0.95f);
        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 0.45f, 1.8f);
        if (hits <= 0) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING,
                    "Solar Reaver did not hit a target.");
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (LivingEntity target : me.nakilex.levelplugin.spells.SpellEffectUtil.getLivingTargets(
                        caster.getLocation(), 3.6, living -> !living.equals(caster))) {
                    me.nakilex.levelplugin.spells.SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, damage * 0.45, true);
                }
                caster.getWorld().spawnParticle(Particle.END_ROD, caster.getLocation().add(0.0, 1.0, 0.0),
                        14, 0.35, 0.25, 0.35, 0.02);
            }
        }.runTaskLater(plugin, 4L);
    }
}
