package me.nakilex.levelplugin.mob.utils;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mob.custom.CustomMobDefinition;
import me.nakilex.levelplugin.mob.custom.CustomMobManager;
import me.nakilex.levelplugin.mob.custom.CustomMobStats;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.utils.AttributeUtil;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

/** Utility for calculating combat power based on common attributes. */
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
     * Estimate combat power for a configured custom mob ID.
     *
     * @param mobId custom mob identifier
     * @return estimated combat power or 0 if the mob could not be resolved
     */
    public static int estimateCombatPower(String mobId) {
        if (mobId == null || mobId.isBlank()) {
            return 0;
        }
        CustomMobManager manager = Main.getInstance().getCustomMobManager();
        if (manager == null) {
            return 0;
        }
        CustomMobDefinition definition = manager.getDefinition(mobId).orElse(null);
        if (definition == null) {
            return 0;
        }
        int level = resolveLevel(definition);
        return estimateCombatPower(definition, level);
    }

    private static int estimateCombatPower(CustomMobDefinition definition, int level) {
        CustomMobStats stats = definition.stats();
        double baseHealth = definition.baseHealth() != null
                ? definition.baseHealth()
                : StatsManager.BASE_HEALTH;
        double hp = stats.computeMaxHealth(baseHealth);
        CustomMobDefinition.CustomMobOptions options = definition.options();
        if (options == null) {
            options = new CustomMobDefinition.CustomMobOptions(null, null, null, null, null, true, false, false);
        }
        double dmg = options.attackDamage() != null
                ? options.attackDamage()
                : (stats.strength() > 0 ? 1.0 + stats.strength() * 0.5 : 0.0);
        double move = options.movementSpeed() != null
                ? options.movementSpeed()
                : 0.2 + stats.agility() * 0.002;
        double atk = options.attackSpeed() != null
                ? options.attackSpeed()
                : (stats.technique() > 0 ? 0.5 * (1.0 + 0.0075 * stats.technique()) * 8.0 : 0.0);
        double power = hp + dmg * 10 + move * 100 + atk * 20 + level * 5;
        return (int) Math.round(power);
    }

    private static int resolveLevel(CustomMobDefinition definition) {
        CustomMobDefinition.LevelRange range = definition.levelRange();
        if (range == null) {
            return 1;
        }
        return Math.max(1, (range.min() + range.max()) / 2);
    }
}
