package me.nakilex.levelplugin.pathfinding.npc;

/** Ranged mage profile casting a basic bolt from afar. */
public class MageMercenary extends AbstractRangedMercenary {
    public MageMercenary() {
        super(org.bukkit.Material.BLAZE_ROD, "Mage_Bolt");
    }

    @Override
    public String name() {
        return "magemercenary";
    }
}
