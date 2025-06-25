package me.nakilex.levelplugin.spells.effect;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import org.bukkit.entity.Player;

/**
 * Simple effect that triggers a MythicMobs skill by name.
 */
public class MythicSkillEffect implements SpellEffect {
    private final String skill;

    public MythicSkillEffect(String skill) {
        this.skill = skill;
    }

    @Override
    public boolean apply(SpellCastContext ctx) {
        Player caster = ctx.getPlayer();

        // Calculate scaled damage using player stats and any modifiers
        var stats = StatsManager.getInstance().getPlayerStats(caster.getUniqueId());
        double strength = stats.baseStrength + stats.bonusStrength;
        double damage = ctx.getFinalDamage() + strength * 0.5;

        me.nakilex.levelplugin.Main.getPlugin().getLogger().info(
                "[MythicSkillEffect] skill=" + skill + " dmg=" + damage +
                " player=" + caster.getName());

        // Pass the damage value as a metadata variable so MythicMobs can use
        // <skill.damage> in the skill file. Fall back to simple cast if the
        // API version lacks the Consumer overload.
        boolean success;
        try {
            success = MythicBukkit.inst().getAPIHelper().castSkill(caster, skill, meta -> {
                try {
                    var vars = meta.getVariables();
                    // Attempt to call setDouble(String,double) if present
                    try {
                        var m = vars.getClass().getMethod("setDouble", String.class, double.class);
                        m.invoke(vars, "damage", damage);
                    } catch (NoSuchMethodException ex) {
                        // Fallback to a generic setter
                        var m = vars.getClass().getMethod("set", String.class, Object.class);
                        m.invoke(vars, "damage", damage);
                    }
                } catch (Exception ignore) {
                    // ignore if variable API changed
                }
            });
        } catch (NoSuchMethodError e) {
            // Older API - just cast normally
            success = MythicBukkit.inst().getAPIHelper().castSkill(caster, skill);
        }
        ctx.markSuccess(success);
        return success;
    }
}
