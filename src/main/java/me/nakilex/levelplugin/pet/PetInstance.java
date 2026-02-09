package me.nakilex.levelplugin.pet;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PetInstance {
    private final UUID ownerId;
    private final PetDefinition definition;
    private final UUID entityId;
    private int level;
    private int xp;
    private int tier;
    private Map<StatType, Integer> appliedStats = new EnumMap<>(StatType.class);
    private List<PetEffectDefinition> appliedEffects = List.of();
    private BukkitTask followTask;
    private BukkitTask effectTask;

    public PetInstance(UUID ownerId, PetDefinition definition, UUID entityId, int level, int xp, int tier) {
        this.ownerId = ownerId;
        this.definition = definition;
        this.entityId = entityId;
        this.level = level;
        this.xp = xp;
        this.tier = Math.max(0, tier);
    }

    public UUID ownerId() {
        return ownerId;
    }

    public PetDefinition definition() {
        return definition;
    }

    public UUID entityId() {
        return entityId;
    }

    public int level() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public int xp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = Math.max(0, xp);
    }

    public int tier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = Math.max(0, tier);
    }

    public Map<StatType, Integer> appliedStats() {
        return Collections.unmodifiableMap(appliedStats);
    }

    public void setAppliedStats(Map<StatType, Integer> appliedStats) {
        if (appliedStats == null || appliedStats.isEmpty()) {
            this.appliedStats = new EnumMap<>(StatType.class);
            return;
        }
        this.appliedStats = new EnumMap<>(appliedStats);
    }

    public List<PetEffectDefinition> appliedEffects() {
        return appliedEffects;
    }

    public void setAppliedEffects(List<PetEffectDefinition> appliedEffects) {
        this.appliedEffects = appliedEffects == null ? List.of() : List.copyOf(appliedEffects);
    }

    public void setFollowTask(BukkitTask followTask) {
        if (this.followTask != null) {
            this.followTask.cancel();
        }
        this.followTask = followTask;
    }

    public void setEffectTask(BukkitTask effectTask) {
        if (this.effectTask != null) {
            this.effectTask.cancel();
        }
        this.effectTask = effectTask;
    }

    public void cancelTasks() {
        if (followTask != null) {
            followTask.cancel();
            followTask = null;
        }
        if (effectTask != null) {
            effectTask.cancel();
            effectTask = null;
        }
    }
}
