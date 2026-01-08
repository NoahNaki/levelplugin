package me.nakilex.levelplugin.mob.custom;

import org.bukkit.entity.LivingEntity;

public record CustomMobInstance(CustomMobDefinition definition, LivingEntity entity, int level) {
    public String id() {
        return definition.id();
    }
}
