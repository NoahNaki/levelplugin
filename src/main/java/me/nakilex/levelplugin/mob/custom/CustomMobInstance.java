package me.nakilex.levelplugin.mob.custom;

import org.bukkit.entity.LivingEntity;

public record CustomMobInstance(CustomMobDefinition definition, LivingEntity entity) {
    public String id() {
        return definition.id();
    }

    public int level() {
        return definition.level();
    }
}
