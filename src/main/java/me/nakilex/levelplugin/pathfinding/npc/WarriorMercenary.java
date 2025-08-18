package me.nakilex.levelplugin.pathfinding.npc;

import me.nakilex.levelplugin.pathfinding.npc.PathNpc.Skill;

/** Melee warrior using a variety of Awakened Warrior skills. */
public class WarriorMercenary extends AbstractMeleeMercenary {
    public WarriorMercenary() {
        super(org.bukkit.Material.NETHERITE_SWORD,
                new Skill("Berserkers_Leap", 5),
                new Skill("Vicious_Strike", 6),
                new Skill("Brutal_Combo", 1));
    }

    @Override
    public String name() {
        return "warriormercenary";
    }
}
