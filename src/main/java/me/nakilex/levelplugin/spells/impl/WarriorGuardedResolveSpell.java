package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.spells.SpellPartyUtil;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WarriorGuardedResolveSpell implements SpellHandler {
    private static final Map<UUID, GuardState> ACTIVE_GUARDS = new ConcurrentHashMap<>();

    private final Main plugin;
    private final int durationTicks;
    private final int blockedHits;
    private final double partyRadius;

    public WarriorGuardedResolveSpell(Main plugin, int durationTicks, int blockedHits, double partyRadius) {
        this.plugin = plugin;
        this.durationTicks = Math.max(20, durationTicks);
        this.blockedHits = Math.max(1, blockedHits);
        this.partyRadius = Math.max(1.0, partyRadius);
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        long expiresAt = System.currentTimeMillis() + (durationTicks * 50L);
        for (Player ally : SpellPartyUtil.resolvePartyPlayersInRange(plugin, caster, partyRadius, true)) {
            ACTIVE_GUARDS.put(ally.getUniqueId(), new GuardState(expiresAt, blockedHits));
            ally.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, ally.getLocation().add(0.0, 1.1, 0.0),
                    8, 0.3, 0.35, 0.3, 0.01);
            ally.getWorld().playSound(ally.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.7f, 1.2f);
        }
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.4f);
    }

    public static double applyIncomingDamage(Player player, double incomingDamage) {
        if (player == null || incomingDamage <= 0.0) {
            return Math.max(0.0, incomingDamage);
        }
        GuardState state = ACTIVE_GUARDS.get(player.getUniqueId());
        if (state == null) {
            return incomingDamage;
        }
        if (System.currentTimeMillis() > state.expiresAtMs) {
            ACTIVE_GUARDS.remove(player.getUniqueId());
            return incomingDamage;
        }

        state.remainingHits--;
        player.getWorld().spawnParticle(Particle.WAX_OFF, player.getLocation().add(0.0, 1.0, 0.0),
                7, 0.2, 0.28, 0.2, 0.005);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.45f, 1.5f);
        if (state.remainingHits <= 0) {
            ACTIVE_GUARDS.remove(player.getUniqueId());
        }
        return 0.0;
    }

    private static final class GuardState {
        private final long expiresAtMs;
        private int remainingHits;

        private GuardState(long expiresAtMs, int remainingHits) {
            this.expiresAtMs = expiresAtMs;
            this.remainingHits = Math.max(1, remainingHits);
        }
    }
}
