package me.nakilex.levelplugin.pathfinding.npc;

import me.nakilex.levelplugin.pathfinding.npc.PathNpc.Skill;
import me.nakilex.levelplugin.spells.managers.CooldownManager;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.LivingEntity;

/** Ranged archer profile firing a suite of Awakened Archer abilities. */
public class ArcherMercenary extends AbstractRangedMercenary {
    private static final Skill SHOT = new Skill("Shot_Of_Destruction", 10);
    private static final Skill VOLLEY = new Skill("Volley_Of_Arrows", 8);
    private static final Skill SKYFALL = new Skill("Piercing_Skyfall", 6);
    private static final Skill RAPID = new Skill("Rapid_Arrows", 3);
    private static final Skill EVASIVE = new Skill("Evasive_Shot", 5);
    private static final Skill COMBO = new Skill("Blasting_Combo", 1);

    public ArcherMercenary() {
        super(org.bukkit.Material.BOW, SHOT, VOLLEY, RAPID, COMBO);
    }

    @Override
    public String name() {
        return "archermercenary";
    }


    @Override
    public void handleCombat(NPC npc, LivingEntity target, CooldownManager cd) {
        if (!maintainRange(npc, target)) return;
        double distSq = npc.getEntity().getLocation().distanceSquared(target.getEyeLocation());
        if (distSq < 49 && cast(npc, EVASIVE, target, cd)) return;
        if (distSq > 81 && cast(npc, SKYFALL, target, cd)) return;
        castNextSkill(npc, target, cd);
    }
}
