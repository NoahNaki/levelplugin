package me.nakilex.levelplugin.utils;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.guild.GuildManager;
import me.nakilex.levelplugin.guild.siege.GuildSiegeManager;

/** Utility methods for combat permissions. */
public final class CombatUtil {
    private CombatUtil() {}

    /**
     * Determines if the attacker is allowed to damage the target. PvE is always allowed.
     *
     * @param attackerId attacker entity UUID
     * @param targetId   target entity UUID
     * @return {@code true} if damage should be permitted
     */
    public static boolean canDamage(UUID attackerId, UUID targetId) {
        Entity attackerEnt = Bukkit.getEntity(attackerId);
        Entity targetEnt = Bukkit.getEntity(targetId);

        // Default to allowing damage if target isn't a player (covers null targets too)
        if (!(targetEnt instanceof Player victim)) {
            return true;
        }
        if (!(attackerEnt instanceof Player attacker)) {
            return true;
        }

        // Block if players are in the same or allied guilds
        if (GuildManager.getInstance().areFriendly(attacker.getUniqueId(), victim.getUniqueId())) {
            return false;
        }

        DuelManager duels = DuelManager.getInstance();
        boolean duel = duels.areFormallyDueling(attacker.getUniqueId(), victim.getUniqueId());
        boolean siege = GuildSiegeManager.getInstance().areSiegeOpponents(attacker.getUniqueId(), victim.getUniqueId());
        return duel || siege;
    }
}
