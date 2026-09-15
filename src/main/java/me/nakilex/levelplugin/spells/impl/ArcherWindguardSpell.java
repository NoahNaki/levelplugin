package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellCastManager;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.spells.SpellPartyUtil;
import me.nakilex.levelplugin.utils.PotionEffectUtil;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class ArcherWindguardSpell implements SpellHandler {
    private final Main plugin;
    private final int durationTicks;
    private final int speedAmplifier;
    private final double partyRadius;

    public ArcherWindguardSpell(Main plugin, int durationTicks, int speedAmplifier, double partyRadius) {
        this.plugin = plugin;
        this.durationTicks = Math.max(20, durationTicks);
        this.speedAmplifier = Math.max(0, speedAmplifier);
        this.partyRadius = Math.max(1.0, partyRadius);
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        var center = caster.getLocation().clone().add(0.0, 0.35, 0.0);
        SpellEffectUtil.applyRadialKnockback(center, 6.0, 1.05, 0.32, 0.45,
                target -> !target.equals(caster));
        caster.getWorld().spawnParticle(Particle.GUST_EMITTER_LARGE, center, 1);
        caster.getWorld().spawnParticle(Particle.GUST, center, 8, 1.4, 0.35, 1.4, 0.0);
        caster.getWorld().playSound(center, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 0.9f, 1.05f);
        for (Player ally : SpellPartyUtil.resolvePartyPlayersInRange(plugin, caster, partyRadius, true)) {
            PotionEffectUtil.applyHiddenEffect(ally, PotionEffectType.SPEED, durationTicks, speedAmplifier);
            SpellCastManager.getInstance().clear(ally);
            ally.getWorld().spawnParticle(Particle.SMALL_GUST, ally.getLocation().add(0.0, 1.0, 0.0),
                    4, 0.35, 0.35, 0.35, 0.0);
            ally.getWorld().playSound(ally.getLocation(), Sound.ENTITY_BREEZE_WHIRL, 0.45f, 1.45f);
        }
    }
}
