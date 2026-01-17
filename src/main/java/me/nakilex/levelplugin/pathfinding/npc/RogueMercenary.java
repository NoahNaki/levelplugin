package me.nakilex.levelplugin.pathfinding.npc;

import me.nakilex.levelplugin.utils.cooldowns.CooldownManager;
import me.nakilex.levelplugin.npc.system.NPC;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import me.nakilex.levelplugin.pathfinding.npc.PathNpc.Skill;

/**
 * Melee rogue profile that cycles through the full Awakened Rogue kit.
 */
public class RogueMercenary extends AbstractMeleeMercenary {
    private static final Skill SKILL_DASH = new Skill("Ravaging_Dash", 5);
    private static final Skill SKILL_LETHAL = new Skill("Lethal_Combo", 1);
    private static final Skill SKILL_BLOOM = new Skill("Death_Bloom", 5);
    private static final Skill SKILL_SHADOW = new Skill("Shadowquake", 8);
    private static final Skill SKILL_CRIMSON = new Skill("Crimson_Arc", 3);
    private static final Skill SKILL_LAST = new Skill("Last_Dance", 12);
    private static final Skill SKILL_DEADLY = new Skill("Deadly_Calm", 20);

    public RogueMercenary() {
        super(Material.NETHERITE_SWORD, SKILL_LETHAL, SKILL_BLOOM, SKILL_SHADOW, SKILL_CRIMSON, SKILL_LAST);
    }

    @Override
    public void handleCombat(NPC npc, LivingEntity target, CooldownManager cd) {
        cast(npc, SKILL_DEADLY, target, cd);
        double distSq = npc.getEntity().getLocation().distanceSquared(target.getEyeLocation());
        if (distSq > 9) {
            if (!cast(npc, SKILL_DASH, target, cd)) {
                npc.getNavigator().setTarget(target, true);
            }
        } else {
            super.handleCombat(npc, target, cd);
        }
    }

    @Override
    public String name() {
        return "roguemercenary";
    }

    @Override
    public String primarySkill() {
        return SKILL_LETHAL.name();
    }
}
