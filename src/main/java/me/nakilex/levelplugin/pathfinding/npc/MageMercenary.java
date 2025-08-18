package me.nakilex.levelplugin.pathfinding.npc;

/** Ranged mage profile hurling fireballs from a distance. */
public class MageMercenary extends AbstractRangedMercenary {
    public MageMercenary() {
        // Fires the Awakened Mage's Fireball spell as its basic attack
        super(org.bukkit.Material.BLAZE_ROD, "Fireball");
    }

    @Override
    public String name() {
        return "magemercenary";
    }
}
