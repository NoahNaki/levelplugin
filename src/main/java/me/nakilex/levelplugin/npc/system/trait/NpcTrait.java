package me.nakilex.levelplugin.npc.system.trait;

import me.nakilex.levelplugin.npc.system.NPC;

public interface NpcTrait {
    default void onAttach(NPC npc) {
    }

    default void onDetach(NPC npc) {
    }

    default void onSpawn(NPC npc) {
    }

    default void onDespawn(NPC npc) {
    }
}
