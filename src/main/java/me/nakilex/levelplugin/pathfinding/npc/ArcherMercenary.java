package me.nakilex.levelplugin.pathfinding.npc;

/** Ranged archer profile firing Blasting Combo arrows from a distance. */
public class ArcherMercenary extends AbstractRangedMercenary {
    private static final String SKILL_SHOT = "Blasting_Combo";

    public ArcherMercenary() {
        // Uses AwakenedArcher's basic Blasting Combo as its auto-attack
        super(org.bukkit.Material.BOW, SKILL_SHOT);
    }

    @Override
    public String name() {
        return "archermercenary";
    }
}
