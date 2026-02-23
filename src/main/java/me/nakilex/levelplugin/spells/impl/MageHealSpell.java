package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.party.Party;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MageHealSpell implements SpellHandler {
    private final Main plugin;
    private final double baseHeal;
    private final boolean partyHeal;
    private final boolean applyRegen;

    public MageHealSpell(Main plugin, double baseHeal, boolean partyHeal, boolean applyRegen) {
        this.plugin = plugin;
        this.baseHeal = baseHeal;
        this.partyHeal = partyHeal;
        this.applyRegen = applyRegen;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        List<Player> targets = resolveTargets(caster);
        for (Player target : targets) {
            double amount = SpellEffectUtil.computeIntTecScaledDamage(caster, baseHeal, 0.35, 0.0);
            double max = target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
                    ? target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue() : 20.0;
            target.setHealth(Math.min(max, target.getHealth() + amount));
            if (applyRegen) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 80, 0, true, true, true));
            }
            target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 1.0, 0), 5, 0.35, 0.4, 0.35, 0.0);
            target.playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.65f, 1.3f);
        }
    }

    private List<Player> resolveTargets(Player caster) {
        if (!partyHeal) {
            return List.of(caster);
        }
        List<Player> targets = new ArrayList<>();
        PartyManager partyManager = plugin.getPartyManager();
        Party party = partyManager == null ? null : partyManager.getParty(caster.getUniqueId());
        if (party == null) {
            return List.of(caster);
        }
        for (UUID memberId : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline() && member.getWorld().equals(caster.getWorld())
                    && member.getLocation().distanceSquared(caster.getLocation()) <= 14 * 14) {
                targets.add(member);
            }
        }
        if (!targets.contains(caster)) {
            targets.add(caster);
        }
        return targets;
    }
}
