package me.nakilex.levelplugin.spells.effect;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
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
        // Invoke MythicMobs to cast the configured skill as this player
        MythicBukkit.inst().getAPIHelper().castSkill(caster, skill);
    }
}
