package me.nakilex.levelplugin.mob.utils;

import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import me.nakilex.levelplugin.utils.AttributeUtil;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

/** Utility for calculating a MythicMob's combat power based on common attributes. */
public final class CombatPowerUtil {
    private CombatPowerUtil() {}

    /**
     * Calculate a simple combat power rating for the given MythicMob.
     * The formula currently sums max health and weighted attributes.
     *
     * @param mob ActiveMob instance
     * @return combat power value
     */
    public static int getCombatPower(ActiveMob mob) {
        if (mob == null || mob.getEntity() == null) return 0;
        LivingEntity ent = (LivingEntity) mob.getEntity().getBukkitEntity();
        return getCombatPower(ent, mob.getLevel());
    }

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
        var opt = io.lumine.mythic.bukkit.MythicBukkit.inst()
                .getMobManager().getMythicMob(mobName);
        if (opt.isEmpty()) return 0;
        org.bukkit.Location loc = org.bukkit.Bukkit.getWorlds().get(0).getSpawnLocation();
        ActiveMob mob = opt.get().spawn(BukkitAdapter.adapt(loc), 1.0);
        int power = getCombatPower(mob);
        mob.getEntity().getBukkitEntity().remove();
        return power;
    }
}
