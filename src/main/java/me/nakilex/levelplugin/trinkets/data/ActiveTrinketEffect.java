package me.nakilex.levelplugin.trinkets.data;

import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/**
 * Runtime state tracking an active trinket effect on a player.
 */
public class ActiveTrinketEffect {

    private final UUID playerId;
    private final TrinketTemplate template;
    private final TrinketEffectDefinition effect;
    private final long expiresAt;
    private final BukkitTask expireTask;
    private double appliedAbsorption;

    public ActiveTrinketEffect(UUID playerId,
                               TrinketTemplate template,
                               TrinketEffectDefinition effect,
                               long expiresAt,
                               BukkitTask expireTask) {
        this.playerId = playerId;
        this.template = template;
        this.effect = effect;
        this.expiresAt = expiresAt;
        this.expireTask = expireTask;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public TrinketTemplate getTemplate() {
        return template;
    }

    public TrinketEffectDefinition getEffect() {
        return effect;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void cancel() {
        if (expireTask != null) {
            expireTask.cancel();
        }
    }

    public double getAppliedAbsorption() {
        return appliedAbsorption;
    }

    public void setAppliedAbsorption(double appliedAbsorption) {
        this.appliedAbsorption = appliedAbsorption;
    }
}
