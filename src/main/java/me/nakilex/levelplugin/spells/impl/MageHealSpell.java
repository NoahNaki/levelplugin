package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.spells.SpellPartyUtil;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class MageHealSpell implements SpellHandler {
    private final Main plugin;
    private final double baseHeal;
    private final boolean partyHeal;
    private final boolean applyRegen;
    private final int manaRestore;
    private final int absorptionAmplifier;

    public MageHealSpell(Main plugin, double baseHeal, boolean partyHeal, boolean applyRegen,
                         int manaRestore, int absorptionAmplifier) {
        this.plugin = plugin;
        this.baseHeal = baseHeal;
        this.partyHeal = partyHeal;
        this.applyRegen = applyRegen;
        this.manaRestore = manaRestore;
        this.absorptionAmplifier = absorptionAmplifier;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        List<Player> targets = SpellPartyUtil.resolvePartyPlayersInRange(plugin, caster, 10.0, true);
        for (Player target : targets) {
            double amount = SpellEffectUtil.computeIntTecScaledDamage(caster, baseHeal, 0.35, 0.0);
            double max = target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
                    ? target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue() : 20.0;
            target.setHealth(Math.min(max, target.getHealth() + amount));
            if (applyRegen) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 80, 0, true, true, true));
            }
            if (absorptionAmplifier >= 0) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 90, absorptionAmplifier, true, true, true));
            }
            clearNegativeEffects(target);
            restoreMana(target);
            SpellEffectUtil.applyAreaDamage(caster, target.getLocation(), 1.6, 1.5);
            target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 1.0, 0), 5, 0.35, 0.4, 0.35, 0.0);
            target.getWorld().spawnParticle(Particle.END_ROD, target.getLocation().add(0, 1.0, 0), 14, 0.35, 0.35, 0.35, 0.02);
            target.playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.65f, 1.3f);
        }
    }

    private void clearNegativeEffects(Player target) {
        PotionEffectType[] removable = {
                PotionEffectType.POISON,
                PotionEffectType.WITHER,
                PotionEffectType.WEAKNESS,
                PotionEffectType.SLOWNESS,
                PotionEffectType.BLINDNESS,
                PotionEffectType.DARKNESS,
                PotionEffectType.MINING_FATIGUE,
                PotionEffectType.NAUSEA,
                PotionEffectType.HUNGER,
                PotionEffectType.LEVITATION
        };
        for (PotionEffectType effectType : removable) {
            target.removePotionEffect(effectType);
        }
    }

    private void restoreMana(Player target) {
        if (manaRestore <= 0) {
            return;
        }
        StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(target.getUniqueId());
        stats.setCurrentMana(Math.min(stats.getMaxMana(), stats.getCurrentMana() + manaRestore));
    }


}
