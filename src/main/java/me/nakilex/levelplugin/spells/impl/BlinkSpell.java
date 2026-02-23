package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.spells.SpellTargetingUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.TeleportUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.Bukkit;

public class BlinkSpell implements SpellHandler {
    private final Main plugin;
    private final double range;
    private final boolean trailDamage;
    private final boolean defensiveBuff;

    public BlinkSpell(Main plugin, double range, boolean trailDamage, boolean defensiveBuff) {
        this.plugin = plugin;
        this.range = range;
        this.trailDamage = trailDamage;
        this.defensiveBuff = defensiveBuff;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        Location from = caster.getLocation().clone();
        Location to = SpellTargetingUtil.resolveSafeTeleportTarget(caster, range);
        if (to == null) {
            Location coarse = caster.getLocation().clone().add(caster.getEyeLocation().getDirection().normalize().multiply(Math.max(2.0, Math.min(range, 8.0))));
            to = SpellTargetingUtil.findNearbySafeLocation(coarse, 2, 6);
        }
        if (to == null || !SpellTargetingUtil.isSafeTeleportLocation(to)) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING, "Blink failed: no safe teleport location in sight.");
            return;
        }
        Vector preservedVelocity = caster.getVelocity().clone();
        caster.getWorld().spawnParticle(Particle.ENTITY_EFFECT, from.clone().add(0, 1.0, 0), 16, 0.3, 0.3, 0.3, 0.01);
        caster.getWorld().spawnParticle(Particle.PORTAL, from.clone().add(0, 1.0, 0), 24, 0.35, 0.35, 0.35, 0.18);
        caster.getWorld().spawnParticle(Particle.ITEM, from.clone().add(0, 1.0, 0), 18, 0.3, 0.3, 0.3,
                org.bukkit.Material.ENDER_EYE.createBlockData());
        TeleportUtils.safeTeleport(caster, to);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (caster.isOnline()) {
                caster.setVelocity(preservedVelocity);
            }
        }, 1L);
        caster.getWorld().playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
        caster.getWorld().spawnParticle(Particle.END_ROD, to.clone().add(0, 1.0, 0), 30, 0.35, 0.45, 0.35, 0.02);
        caster.getWorld().spawnParticle(Particle.ITEM, to.clone().add(0, 1.0, 0), 30, 0.4, 0.4, 0.4,
                org.bukkit.Material.ENDER_EYE.createBlockData());

        if (trailDamage) {
            Vector step = to.toVector().subtract(from.toVector()).multiply(1.0 / 6.0);
            Location sample = from.clone().add(0, 1.0, 0);
            for (int i = 0; i < 6; i++) {
                SpellEffectUtil.applyAreaDamage(caster, sample, 1.25, 2.5);
                sample.add(step);
            }
        }
        if (defensiveBuff) {
            caster.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 80, 1, true, true, true));
            caster.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 50, 0, true, true, true));
        }
    }
}
