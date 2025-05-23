package me.nakilex.levelplugin.spells.effect.mage;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.party.Party;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Exact reincarnation of original healPlayer logic as a SpellEffect.
 */
public class HealEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        UUID pid = player.getUniqueId();

        // 1) Compute heal amount (base=10)
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(pid);
        int intel = ps.baseIntelligence + ps.bonusIntelligence;
        double healAmount = 10.0 + (intel * 0.5);
        healAmount *= ctx.getFinalDamage()/ctx.getBaseSpell().getBaseDamage();

        // 2) Build target list (self + party)
        List<Player> toHeal = new ArrayList<>();
        toHeal.add(player);
        Party party = Main.getInstance().getPartyManager().getParty(pid);
        DuelManager dm = DuelManager.getInstance();
        if (party != null) {
            for (UUID memberId : party.getMembers()) {
                if (memberId.equals(pid)) continue;
                Player member = Main.getInstance().getServer().getPlayer(memberId);
                if (member == null || !member.isOnline()) continue;
                // skip duel opponents
                if (dm.areInDuel(pid, memberId)) continue;
                toHeal.add(member);
            }
        }

        // 3) Apply heal and VFX/SFX
        for (Player target : toHeal) {
            double maxHp = Objects.requireNonNull(target.getAttribute(Attribute.MAX_HEALTH)).getValue();
            target.setHealth(Math.min(target.getHealth() + healAmount, maxHp));
            target.spawnParticle(Particle.HAPPY_VILLAGER, target.getLocation(), 30, 1, 1, 1, 0.2);
            target.playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1f, 1f);
            if (target.equals(player)) {
                target.sendMessage("§aYou have been healed for " + Math.round(healAmount) + " health!");
            } else {
                target.sendMessage("§a" + player.getName() + " healed you for " + Math.round(healAmount) + " health!");
            }
        }
    }
}