package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellCastManager;
import me.nakilex.levelplugin.spells.SpellContext;
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
        for (Player ally : SpellPartyUtil.resolvePartyPlayersInRange(plugin, caster, partyRadius, true)) {
            PotionEffectUtil.applyHiddenEffect(ally, PotionEffectType.SPEED, durationTicks, speedAmplifier);
            SpellCastManager.getInstance().clear(ally);
            ally.getWorld().spawnParticle(Particle.CLOUD, ally.getLocation().add(0.0, 1.0, 0.0),
                    14, 0.35, 0.35, 0.35, 0.01);
            ally.getWorld().playSound(ally.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_2, 0.5f, 1.6f);
        }
    }
}
