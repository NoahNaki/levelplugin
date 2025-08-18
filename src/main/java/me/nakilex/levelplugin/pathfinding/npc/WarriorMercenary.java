package me.nakilex.levelplugin.pathfinding.npc;

import me.nakilex.levelplugin.pathfinding.npc.PathNpc.Skill;

/** Melee warrior using the full Awakened Warrior kit. */
public class WarriorMercenary extends AbstractMeleeMercenary {
    public WarriorMercenary() {
        super(org.bukkit.Material.NETHERITE_SWORD,
                new Skill("Berserkers_Leap", 5),
                new Skill("Vicious_Strike", 6),
                new Skill("Brutal_Combo", 1),
                new Skill("Bulwark_Instinct", 12),
                new Skill("Relentless_Whirlwind", 10),
                new Skill("Bloodbound_Barrier", 20),
                new Skill("Strike_Of_Fury", 15));
    }

    @Override
    public String name() {
        return "warriormercenary";
    }

}
