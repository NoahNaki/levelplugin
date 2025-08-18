package me.nakilex.levelplugin.pathfinding.npc;

import me.nakilex.levelplugin.pathfinding.npc.PathNpc.Skill;

/** Ranged archer profile firing a suite of Awakened Archer abilities. */
public class ArcherMercenary extends AbstractRangedMercenary {
    public ArcherMercenary() {
        super(org.bukkit.Material.BOW,
                new Skill("Shot_Of_Destruction", 10),
                new Skill("Evasive_Shot", 5),
                new Skill("Rapid_Arrows", 3),
                new Skill("Blasting_Combo", 1));
    }

    @Override
    public String name() {
        return "archermercenary";
    }
}
