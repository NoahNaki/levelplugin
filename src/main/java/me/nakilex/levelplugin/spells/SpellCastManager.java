package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.pet.PetEffectType;
import me.nakilex.levelplugin.pet.PetManager;
import me.nakilex.levelplugin.player.attributes.managers.CooldownIndicatorManager;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpellCastManager {
    private static final SpellCastManager instance = new SpellCastManager();
    public static SpellCastManager getInstance() {
        return instance;
    }

    private static final long MOVEMENT_RESET_MS = 5_000L;
    private static final double MOVEMENT_SCALE = 0.10;
    private static final Map<String, Long> COOLDOWN_OVERRIDES_MS = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Long>> PLAYER_COOLDOWNS = new ConcurrentHashMap<>();
    private static volatile boolean cooldownsEnabled = true;
    private static volatile boolean manaCostsEnabled = true;

    private final Map<UUID, MovementChain> movementChains = new ConcurrentHashMap<>();

    public int getManaCost(Player player, SpellDefinition spell) {
        if (player == null || spell == null) {
            return 0;
        }
        int baseCost = spell.baseManaCost();
        if (!spell.movementSpell()) {
            return baseCost;
        }
        double cost = baseCost;
        UUID playerId = player.getUniqueId();
        MovementChain chain = movementChains.get(playerId);
        if (chain != null && !chain.isExpired()) {
            double multiplier = getMobilityManaMultiplier(chain.streak);
            cost = baseCost * multiplier;
        }
        PetManager petManager = Main.getInstance().getPetManager();
        if (petManager != null) {
            double reduction = petManager.getActiveEffectValue(playerId, PetEffectType.MOVEMENT_MANA_REDUCTION);
            if (reduction > 0.0) {
                cost *= Math.max(0.0, 1.0 - reduction);
            }
        }
        return (int) Math.ceil(cost);
    }

    public void recordCast(Player player, SpellDefinition spell) {
        if (player == null || spell == null || !spell.movementSpell()) {
            return;
        }
        UUID playerId = player.getUniqueId();
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
        PLAYER_COOLDOWNS.remove(player.getUniqueId());
    }

    public static void setCooldownsEnabled(boolean enabled) {
        cooldownsEnabled = enabled;
    }

    public static boolean areCooldownsEnabled() {
        return cooldownsEnabled;
    }

    public static void setManaCostsEnabled(boolean enabled) {
        manaCostsEnabled = enabled;
    }

    public static boolean areManaCostsEnabled() {
        return manaCostsEnabled;
    }

    public static void setSpellCooldownMs(String spellId, long cooldownMs) {
        if (spellId == null || spellId.isBlank()) {
            return;
        }
        COOLDOWN_OVERRIDES_MS.put(spellId.toLowerCase(), Math.max(0L, cooldownMs));
    }

    public long getCooldownMs(SpellDefinition spell) {
        return getCooldownMs(null, spell);
    }

    public long getCooldownMs(Player player, SpellDefinition spell) {
        if (spell == null) {
            return 0L;
        }
        Long configured = COOLDOWN_OVERRIDES_MS.get(spell.id().toLowerCase());
        long baseCooldown = configured != null
                ? configured
                : Math.max(0L, Math.round(spell.baseManaCost() * 220.0));
        if (player == null) {
            return baseCooldown;
        }
        StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        double reduction = Math.max(0.0, Math.min(0.75, stats.cooldownReduction));
        return Math.max(0L, Math.round(baseCooldown * (1.0 - reduction)));
    }

    public long getRemainingCooldownMs(Player player, SpellDefinition spell) {
        if (player == null || spell == null) {
            return 0L;
        }
        Map<String, Long> cooldowns = PLAYER_COOLDOWNS.get(player.getUniqueId());
        if (cooldowns == null) {
            return 0L;
        }
        long expires = cooldowns.getOrDefault(spell.id().toLowerCase(), 0L);
        return Math.max(0L, expires - System.currentTimeMillis());
    }

    public boolean tryConsumeResources(Player player, SpellDefinition spell) {
        if (player == null || spell == null) {
            return false;
        }
        StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        int manaCost = getManaCost(player, spell);
        if (manaCostsEnabled && manaCost > 0 && stats.getCurrentMana() < manaCost) {
            return false;
        }
        if (cooldownsEnabled && getRemainingCooldownMs(player, spell) > 0) {
            return false;
        }

        if (manaCostsEnabled && manaCost > 0) {
            stats.setCurrentMana(stats.getCurrentMana() - manaCost);
        }
        long cooldownMs = getCooldownMs(player, spell);
        if (cooldownsEnabled && cooldownMs > 0L) {
            PLAYER_COOLDOWNS.computeIfAbsent(player.getUniqueId(), id -> new ConcurrentHashMap<>())
                    .put(spell.id().toLowerCase(), System.currentTimeMillis() + cooldownMs);
            CooldownIndicatorManager.getInstance().show(player, spell.displayName(), cooldownMs, manaCost);
        } else if (manaCost > 0) {
            CooldownIndicatorManager.getInstance().show(player, spell.displayName(), 0L, manaCost);
        }
        return true;
    }

    private static final class MovementChain {
        private int streak;
        private long lastCastAt;

        private boolean isExpired() {
            return System.currentTimeMillis() - lastCastAt > MOVEMENT_RESET_MS;
        }
    }

    public static double getMobilityManaMultiplier(int consecutiveCasts) {
        return 1.0 + (MOVEMENT_SCALE * Math.max(0, consecutiveCasts));
    }
}
