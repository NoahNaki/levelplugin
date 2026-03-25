package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.party.Party;
import me.nakilex.levelplugin.party.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SpellPartyUtil {
    private SpellPartyUtil() {
    }

    public static List<Player> resolvePartyPlayersInRange(Main plugin, Player caster, double radius, boolean includeCaster) {
        if (plugin == null || caster == null || caster.getWorld() == null) {
            return List.of();
        }
        double radiusSq = Math.max(0.1, radius) * Math.max(0.1, radius);
        List<Player> targets = new ArrayList<>();

        PartyManager partyManager = plugin.getPartyManager();
        Party party = partyManager == null ? null : partyManager.getParty(caster.getUniqueId());
        if (party == null) {
            if (includeCaster) {
                targets.add(caster);
            }
            return targets;
        }

        for (UUID memberId : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member == null || !member.isOnline() || !member.getWorld().equals(caster.getWorld())) {
                continue;
            }
            if (member.getLocation().distanceSquared(caster.getLocation()) > radiusSq) {
                continue;
            }
            if (!includeCaster && member.getUniqueId().equals(caster.getUniqueId())) {
                continue;
            }
            targets.add(member);
        }

        if (includeCaster && targets.stream().noneMatch(player -> player.getUniqueId().equals(caster.getUniqueId()))) {
            targets.add(caster);
        }
        return targets;
    }
}
