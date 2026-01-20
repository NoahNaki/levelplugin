package me.nakilex.levelplugin.mob.custom;

import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitTask;

public class CustomMobInstance {
    private final CustomMobDefinition definition;
    private final LivingEntity entity;
    private final int level;
    private final boolean baseAi;
    private boolean stunned;
    private BukkitTask stunResetTask;
    private BukkitTask stunParticleTask;

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

    public boolean isStunned() {
        return stunned;
    }

    public void setStunned(boolean stunned) {
        this.stunned = stunned;
    }

    public boolean baseAi() {
        return baseAi;
    }

    public BukkitTask getStunResetTask() {
        return stunResetTask;
    }

    public void setStunResetTask(BukkitTask stunResetTask) {
        if (this.stunResetTask != null) {
            this.stunResetTask.cancel();
        }
        this.stunResetTask = stunResetTask;
    }

    public BukkitTask getStunParticleTask() {
        return stunParticleTask;
    }

    public void setStunParticleTask(BukkitTask stunParticleTask) {
        if (this.stunParticleTask != null) {
            this.stunParticleTask.cancel();
        }
        this.stunParticleTask = stunParticleTask;
    }

    public void clearStunTasks() {
        if (stunResetTask != null) {
            stunResetTask.cancel();
            stunResetTask = null;
        }
        if (stunParticleTask != null) {
            stunParticleTask.cancel();
            stunParticleTask = null;
        }
    }
}
