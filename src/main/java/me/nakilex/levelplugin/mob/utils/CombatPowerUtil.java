package me.nakilex.levelplugin.mob.utils;

import me.nakilex.levelplugin.utils.AttributeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

/** Utility for calculating a MythicMob's combat power based on common attributes. */
public final class CombatPowerUtil {
    private CombatPowerUtil() {}

    /**
     * Calculate a simple combat power rating for the given living entity.
     *
     * @param entity living entity to inspect
     * @param level  level to include in the power calculation
     * @return combat power value
     */
    public static int getCombatPower(LivingEntity entity, double level) {
        if (entity == null) {
            return 0;
        }
        Attribute hpAttr = AttributeUtil.resolve("GENERIC_MAX_HEALTH", "MAX_HEALTH");
        Attribute dmgAttr = AttributeUtil.resolve("GENERIC_ATTACK_DAMAGE", "ATTACK_DAMAGE");
        Attribute moveAttr = AttributeUtil.resolve("GENERIC_MOVEMENT_SPEED", "MOVEMENT_SPEED");
        Attribute atkAttr = AttributeUtil.resolve("GENERIC_ATTACK_SPEED", "ATTACK_SPEED");

        double hp = hpAttr != null && entity.getAttribute(hpAttr) != null
                ? entity.getAttribute(hpAttr).getBaseValue()
                : entity.getMaxHealth();
        double dmg = dmgAttr != null && entity.getAttribute(dmgAttr) != null
                ? entity.getAttribute(dmgAttr).getBaseValue()
                : 0;
        double move = moveAttr != null && entity.getAttribute(moveAttr) != null
                ? entity.getAttribute(moveAttr).getBaseValue()
                : 0;
        double atk = atkAttr != null && entity.getAttribute(atkAttr) != null
                ? entity.getAttribute(atkAttr).getBaseValue()
                : 0;
        double power = hp + dmg * 10 + move * 100 + atk * 20 + level * 5;
        return (int) Math.round(power);
    }

    /**
     * Spawn the given MythicMob template briefly to estimate its combat power.
     * The spawned entity is removed immediately after evaluation.
     *
     * @param mobName MythicMob internal name
     * @return estimated combat power or 0 if the mob could not be spawned
     */
    public static int estimateCombatPower(String mobName) {
        if (!Bukkit.getPluginManager().isPluginEnabled("MythicMobs")) {
            return 0;
        }
        try {
            Class<?> mythicClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Object mythic = mythicClass.getMethod("inst").invoke(null);
            Object apiHelper = mythic.getClass().getMethod("getAPIHelper").invoke(mythic);
            Location loc = Bukkit.getWorlds().get(0).getSpawnLocation();
            Object entity = apiHelper.getClass()
                    .getMethod("spawnMythicMob", String.class, Location.class, int.class)
                    .invoke(apiHelper, mobName, loc, 1);
            if (!(entity instanceof LivingEntity livingEntity)) {
                return 0;
            }
            Object activeMob = apiHelper.getClass()
                    .getMethod("getMythicMobInstance", org.bukkit.entity.Entity.class)
                    .invoke(apiHelper, livingEntity);
            int power = activeMob != null ? getCombatPower(activeMob) : getCombatPower(livingEntity, 1.0);
            livingEntity.remove();
            return power;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return 0;
        }
    }

    /**
     * Calculate a simple combat power rating for a MythicMob instance without a hard dependency.
     *
     * @param mythicMob MythicMob instance (ActiveMob) or similar wrapper
     * @return combat power value
     */
    public static int getCombatPower(Object mythicMob) {
        if (mythicMob == null) {
            return 0;
        }
        try {
            Object entityWrapper = mythicMob.getClass().getMethod("getEntity").invoke(mythicMob);
            if (entityWrapper == null) {
                return 0;
            }
            Object bukkitEntity = entityWrapper.getClass().getMethod("getBukkitEntity").invoke(entityWrapper);
            if (!(bukkitEntity instanceof LivingEntity livingEntity)) {
                return 0;
            }
            double level = 0.0;
            try {
                Object levelValue = mythicMob.getClass().getMethod("getLevel").invoke(mythicMob);
                if (levelValue instanceof Number number) {
                    level = number.doubleValue();
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
            return getCombatPower(livingEntity, level);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return 0;
        }
    }
}
