package me.nakilex.levelplugin.spells.effect.rogue;

import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class SpinAttackEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        double radius = 3.0;
        Object r = ctx.getExtraParam("radius");
        if (r instanceof Number num) radius += num.doubleValue();

        int hits = 1;
        Object h = ctx.getExtraParam("extraHits");
        if (h instanceof Number num) hits += num.intValue();

        int blindTicks = 0;
        Object b = ctx.getExtraParam("blindDuration");
        if (b instanceof Number num) blindTicks = num.intValue();

        boolean transferDebuffs = Boolean.TRUE.equals(ctx.getExtraParam("transferDebuffs"));
        int weakTicks = 0;
        Object w = ctx.getExtraParam("weaknessDuration");
        if (w instanceof Number num) weakTicks = num.intValue();

        double damage = ctx.getFinalDamage();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.2f);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation(), 30, radius, 1, radius);

        for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
            if (!(e instanceof LivingEntity le) || le.equals(player)) continue;
            for (int i = 0; i < hits; i++) {
                SpellUtils.dealWithChat(player, le, damage, "Spin Attack");
            }
            if (blindTicks > 0) {
                le.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, blindTicks, 0));
            }
            if (transferDebuffs) {
                player.getActivePotionEffects().stream()
                    .filter(pe -> pe.getType().equals(org.bukkit.potion.PotionEffectType.POISON)
                            || pe.getType().equals(org.bukkit.potion.PotionEffectType.WITHER)
                            || pe.getType().equals(PotionEffectType.SLOWNESS)
                            || pe.getType().equals(org.bukkit.potion.PotionEffectType.WEAKNESS)
                            || pe.getType().equals(org.bukkit.potion.PotionEffectType.BLINDNESS))
                    .forEach(le::addPotionEffect);
            }
            if (weakTicks > 0) {
                le.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS, weakTicks, 0));
            }
        }

        Object speed = ctx.getExtraParam("speedDuration");
        if (speed instanceof Number num && num.intValue() > 0) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, num.intValue(), 1));
        }
        Object jump = ctx.getExtraParam("jumpDuration");
        if (jump instanceof Number num && num.intValue() > 0) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(PotionEffectType.JUMP_BOOST, num.intValue(), 1));
        }
    }
}
