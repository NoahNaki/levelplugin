package me.nakilex.levelplugin.pathfinding.npc;

/** Ranged archer profile firing Blasting Combo arrows from a distance. */
public class ArcherMercenary extends AbstractRangedMercenary {
    public ArcherMercenary() {
        // Uses AwakenedArcher's basic Blasting Combo as its auto-attack
        super(org.bukkit.Material.BOW, "Blasting_Combo");
    }

    @Override
    public String name() {
        return "archermercenary";
    }
}
