package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpellCastManager {
    private static final SpellCastManager instance = new SpellCastManager();
    public static SpellCastManager getInstance() {
        return instance;
    }

    private static final long MOVEMENT_RESET_MS = 4_000L;
    private static final double MOVEMENT_SCALE = 0.25;

    private final Map<UUID, MovementChain> movementChains = new ConcurrentHashMap<>();

    public int getManaCost(Player player, SpellDefinition spell) {
        if (player == null || spell == null) {
            return 0;
        }
        int baseCost = spell.baseManaCost();
        if (!spell.movementSpell()) {
            return baseCost;
        }
        UUID playerId = player.getUniqueId();
        if (!StatsManager.getInstance().isInCombat(playerId)) {
            return baseCost;
        }
        MovementChain chain = movementChains.get(playerId);
        if (chain == null || chain.isExpired()) {
            return baseCost;
        }
        double multiplier = 1.0 + (MOVEMENT_SCALE * chain.streak);
        return (int) Math.ceil(baseCost * multiplier);
    }

    public void recordCast(Player player, SpellDefinition spell) {
        if (player == null || spell == null || !spell.movementSpell()) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (!StatsManager.getInstance().isInCombat(playerId)) {
            movementChains.remove(playerId);
            return;
        }
        MovementChain chain = movementChains.computeIfAbsent(playerId, id -> new MovementChain());
        if (chain.isExpired()) {
            chain.streak = 0;
        }
        chain.streak++;
        chain.lastCastAt = System.currentTimeMillis();
    }

    public void clear(Player player) {
        if (player == null) {
            return;
        }
        movementChains.remove(player.getUniqueId());
    }

    private static final class MovementChain {
        private int streak;
        private long lastCastAt;

        private boolean isExpired() {
            return System.currentTimeMillis() - lastCastAt > MOVEMENT_RESET_MS;
        }
    }
}
