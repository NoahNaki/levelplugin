package me.nakilex.levelplugin.pathfinding.npc;

/** Ranged archer profile firing arrows from a distance. */
public class ArcherMercenary extends AbstractRangedMercenary {
    public ArcherMercenary() {
        super(org.bukkit.Material.BOW, "Archer_Shot");
    }

    @Override
    public String name() {
        return "archermercenary";
    }
}
