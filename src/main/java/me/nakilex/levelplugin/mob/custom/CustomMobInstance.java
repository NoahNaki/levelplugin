package me.nakilex.levelplugin.mob.custom;

import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CustomMobInstance {
    private final CustomMobDefinition definition;
    private final LivingEntity entity;
    private final int level;
    private final boolean baseAi;
    private final EnumSet<CustomMobStatus> activeStatuses = EnumSet.noneOf(CustomMobStatus.class);
    private final Map<CustomMobStatus, BukkitTask> resetTasks = new EnumMap<>(CustomMobStatus.class);
    private final Map<CustomMobStatus, BukkitTask> particleTasks = new EnumMap<>(CustomMobStatus.class);
    private final Map<CustomMobStatus, BukkitTask> effectTasks = new EnumMap<>(CustomMobStatus.class);
    private final Map<CustomMobStatus, UUID> sources = new EnumMap<>(CustomMobStatus.class);

    public CustomMobInstance(CustomMobDefinition definition, LivingEntity entity, int level) {
        this.definition = definition;
        this.entity = entity;
        this.level = level;
        this.baseAi = entity != null && entity.hasAI();
    }

    public String id() {
        return definition.id();
    }

    public CustomMobDefinition definition() {
        return definition;
    }

    public LivingEntity entity() {
        return entity;
    }

    public int level() {
        return level;
    }

    public boolean baseAi() {
        return baseAi;
    }

    public boolean isStatusActive(CustomMobStatus status) {
        return activeStatuses.contains(status);
    }

    public void setStatusActive(CustomMobStatus status, boolean active) {
        if (active) {
            activeStatuses.add(status);
        } else {
            activeStatuses.remove(status);
        }
    }

    public Optional<UUID> getStatusSource(CustomMobStatus status) {
        return Optional.ofNullable(sources.get(status));
    }

    public void setStatusSource(CustomMobStatus status, UUID source) {
        if (source == null) {
            sources.remove(status);
        } else {
            sources.put(status, source);
        }
    }

    public void setResetTask(CustomMobStatus status, BukkitTask task) {
        replaceTask(resetTasks, status, task);
    }

    public void setParticleTask(CustomMobStatus status, BukkitTask task) {
        replaceTask(particleTasks, status, task);
    }

    public void setEffectTask(CustomMobStatus status, BukkitTask task) {
        replaceTask(effectTasks, status, task);
    }

    public void clearStatusTasks(CustomMobStatus status) {
        cancelTask(resetTasks.remove(status));
        cancelTask(particleTasks.remove(status));
        cancelTask(effectTasks.remove(status));
        sources.remove(status);
        activeStatuses.remove(status);
    }

    public void clearAllStatusTasks() {
        for (BukkitTask task : resetTasks.values()) {
            cancelTask(task);
        }
        for (BukkitTask task : particleTasks.values()) {
            cancelTask(task);
        }
        for (BukkitTask task : effectTasks.values()) {
            cancelTask(task);
        }
        resetTasks.clear();
        particleTasks.clear();
        effectTasks.clear();
        sources.clear();
        activeStatuses.clear();
    }

    private void replaceTask(Map<CustomMobStatus, BukkitTask> map, CustomMobStatus status, BukkitTask task) {
        cancelTask(map.put(status, task));
    }

    private void cancelTask(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }
}
