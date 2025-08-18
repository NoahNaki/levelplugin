package me.nakilex.levelplugin.pathfinding.npc;

import me.nakilex.levelplugin.pathfinding.npc.PathNpc.Skill;

/** Ranged mage profile wielding multiple Awakened Mage spells. */
public class MageMercenary extends AbstractRangedMercenary {
    public MageMercenary() {
        super(org.bukkit.Material.BLAZE_ROD,
                new Skill("Meteor", 8),
                new Skill("Frost_Nova", 5),
                new Skill("Fireball", 1));
    }

    @Override
    public String name() {
        return "magemercenary";
    }
}
