package me.nakilex.levelplugin.mob.utils;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.lootchests.utils.LocationUtils;
import me.nakilex.levelplugin.utils.AttributeUtil;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
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
    var mobOptional = MobNameUtil.resolveMythicInternalName(mobName);
    String locationString = LocationUtils.blockLocationString(loc);
    if (mobOptional.isEmpty()) {
        Main.getInstance().getLogger().warning(String.format(
                "[DungeonSpawn] MythicMob template '%s' could not be resolved (location=%s)",
                mobName,
                locationString));
        return null;
    }
    String internalName = mobOptional.get();
    ActiveMob active = MythicBukkit.inst().getMobManager().spawnMob(internalName, loc, 1.0);
    if (active == null) {
        Main.getInstance().getLogger().warning(String.format(
                "[DungeonSpawn] MythicMob '%s' (internal='%s') failed to spawn at %s",
                mobName,
                internalName,
                locationString));
        return null;
    }
    LivingEntity entity = (LivingEntity) active.getEntity().getBukkitEntity();

    if (hp != null) {
        Attribute attr = AttributeUtil.resolve("GENERIC_MAX_HEALTH", "MAX_HEALTH");
        if (attr != null && entity.getAttribute(attr) != null) {
            entity.getAttribute(attr).setBaseValue(hp);
        }
        entity.setHealth(hp);
    }
    if (damage != null) {
        Attribute attr = AttributeUtil.resolve("GENERIC_ATTACK_DAMAGE", "ATTACK_DAMAGE");
        if (attr != null && entity.getAttribute(attr) != null) {
            entity.getAttribute(attr).setBaseValue(damage);
        }
    }
    if (moveSpeed != null) {
        Attribute attr = AttributeUtil.resolve("GENERIC_MOVEMENT_SPEED", "MOVEMENT_SPEED");
        if (attr != null && entity.getAttribute(attr) != null) {
            entity.getAttribute(attr).setBaseValue(moveSpeed);
        }
    }
    if (attackSpeed != null) {
        Attribute attr = AttributeUtil.resolve("GENERIC_ATTACK_SPEED", "ATTACK_SPEED");
        if (attr != null && entity.getAttribute(attr) != null) {
            entity.getAttribute(attr).setBaseValue(attackSpeed);
        }
    }

    String overrides = String.format("hp=%s dmg=%s move=%s atk=%s",
            hp == null ? "-" : String.format("%.2f", hp),
            damage == null ? "-" : String.format("%.2f", damage),
            moveSpeed == null ? "-" : String.format("%.2f", moveSpeed),
            attackSpeed == null ? "-" : String.format("%.2f", attackSpeed));
    Main.getInstance().getLogger().info(String.format(
            "[DungeonSpawn] MythicMob '%s' (internal='%s') spawned at %s with overrides %s",
            mobName,
            internalName,
            locationString,
            overrides));

    return active;
}

    /**
     * Attempt to extract a Bukkit {@link Entity} from various MythicMobs
     * wrapper objects. Supports both direct {@code Entity} instances and
     * MythicMobs' adapter classes through reflection.
     *
     * @param obj possible MythicMobs entity wrapper
     * @return underlying Bukkit entity or {@code null} if it cannot be resolved
     */
    public static Entity toBukkitEntity(Object obj) {
        if (obj instanceof Entity e) return e;
        if (obj == null) return null;

        // Try obj.getBukkitEntity()
        try {
            Object bukkit = obj.getClass().getMethod("getBukkitEntity").invoke(obj);
            if (bukkit instanceof Entity e) return e;
        } catch (Exception ignored) {
        }

        // Try obj.getEntity().getBukkitEntity()
        try {
            Object inner = obj.getClass().getMethod("getEntity").invoke(obj);
            if (inner != null) {
                Object bukkit = inner.getClass().getMethod("getBukkitEntity").invoke(inner);
                if (bukkit instanceof Entity e) return e;
            }
        } catch (Exception ignored) {
        }

        return null;
    }
}
