package me.nakilex.levelplugin.pathfinding.npc;

import me.nakilex.levelplugin.pathfinding.npc.PathNpc.Skill;
import me.nakilex.levelplugin.spells.managers.CooldownManager;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

/** Ranged archer profile firing a suite of Awakened Archer abilities. */
public class ArcherMercenary extends AbstractRangedMercenary {
    private final Skill shot = new Skill("Shot_Of_Destruction", 10);
    private final Skill volley = new Skill("Volley_Of_Arrows", 8);
    private final Skill skyfall = new Skill("Piercing_Skyfall", 6);
    private final Skill rapid = new Skill("Rapid_Arrows", 3);
    private final Skill evasive = new Skill("Evasive_Shot", 5);
    private final Skill combo = new Skill("Blasting_Combo", 1);

    public ArcherMercenary() {
        super(org.bukkit.Material.BOW, new Skill("Blasting_Combo", 1));
    }

    @Override
    public String name() {
        return "archermercenary";
    }

    @Override
    public void handleCombat(NPC npc, LivingEntity target, CooldownManager cd) {
        Location npcLoc = npc.getEntity().getLocation();
        Location targetLoc = target.getEyeLocation();
        double distSq = npcLoc.distanceSquared(targetLoc);
        npc.faceLocation(targetLoc);

        if (distSq < 49 && cast(npc, evasive, target, cd)) return;
        if (distSq > 100 && cast(npc, skyfall, target, cd)) return;
        if (cast(npc, shot, target, cd)) return;
        if (cast(npc, volley, target, cd)) return;
        if (cast(npc, rapid, target, cd)) return;
        cast(npc, combo, target, cd);
    }
}
