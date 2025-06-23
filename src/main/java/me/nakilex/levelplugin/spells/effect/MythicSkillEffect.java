package me.nakilex.levelplugin.spells.effect;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import org.bukkit.entity.Player;

/**
 * Simple effect that triggers a MythicMobs skill by name.
 */
public class MythicSkillEffect implements SpellEffect {
    private final String skillName;

    public MythicSkillEffect(String skillName) {
        this.skillName = skillName;
    }

    @Override
    public void apply(SpellCastContext ctx) {
        Player caster = ctx.getPlayer();
        MythicBukkit.inst().getAPIHelper().castSkill(caster, skillName);
    }
}
