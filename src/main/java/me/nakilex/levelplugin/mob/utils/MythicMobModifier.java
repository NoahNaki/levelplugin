package me.nakilex.levelplugin.mob.utils;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.mobs.MythicMob;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

/**
 * Utility to spawn a MythicMob and override common Bukkit attributes.
 * If an attribute value is {@code null} it will not be modified.
 */
public final class MythicMobModifier {

    private MythicMobModifier() {}

    /**
     * Spawn the named MythicMob template at the given location and apply
     * Bukkit attribute overrides if provided.
     *
     * @param mobName     MythicMob internal name
     * @param loc         spawn location
     * @param hp          max & current health override
     * @param damage      base attack damage override
     * @param moveSpeed   movement speed override
     * @param attackSpeed attack speed override
     * @return spawned {@link ActiveMob} or {@code null} if the mob template wasn't found
     */
    public static ActiveMob spawnModifiedMob(
            String mobName,
            Location loc,
            Double hp,
            Double damage,
            Double moveSpeed,
            Double attackSpeed
    ) {
        MythicMob template = MythicBukkit.inst().getMobManager().getMythicMob(mobName).orElse(null);
        if (template == null) {
            return null;
        }
        ActiveMob active = MythicBukkit.inst().getMobManager().spawnMob(mobName, loc, 1.0);
        LivingEntity entity = active.getEntity().getBukkitEntity();
        if (hp != null) {
            entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(hp);
            entity.setHealth(hp);
        }
        if (damage != null && entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
        }
        if (moveSpeed != null && entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
            entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(moveSpeed);
        }
        if (attackSpeed != null && entity.getAttribute(Attribute.GENERIC_ATTACK_SPEED) != null) {
            entity.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(attackSpeed);
        }
        return active;
    }
}
