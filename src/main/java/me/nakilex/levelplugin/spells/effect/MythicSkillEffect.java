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
    public void apply(SpellCastContext ctx) {
        Player caster = ctx.getPlayer();

        // Calculate scaled damage using player stats and any modifiers
        var stats = StatsManager.getInstance().getPlayerStats(caster.getUniqueId());
        double strength = stats.baseStrength + stats.bonusStrength;
        double damage = ctx.getFinalDamage() + strength * 0.5;

        // Pass the damage value as a metadata variable so MythicMobs can use
        // <skill.damage> in the skill file. Fall back to simple cast if the
        // API version lacks the Consumer overload.
        try {
            MythicBukkit.inst().getAPIHelper().castSkill(caster, skill, meta -> {
                meta.getVariables().setDouble("damage", damage);
            });
        } catch (NoSuchMethodError e) {
            // Older API - just cast normally
            MythicBukkit.inst().getAPIHelper().castSkill(caster, skill);
        }
    }
}
