package me.nakilex.npc.core.trait;

import me.nakilex.npc.core.model.Npc;
import org.bukkit.entity.Player;

import java.util.Map;

public interface Trait {
    String getId();

    default boolean requiresTicking() {
        return false;
    }

    default void onLoad(Npc npc, Map<String, Object> data) {
    }

    default Map<String, Object> onSave(Npc npc) {
        return null;
    }

    default void onSpawn(Npc npc) {
    }

    default void onDespawn(Npc npc) {
    }

    default void onTick(Npc npc) {
    }

    default void onInteract(Npc npc, Player player) {
    }
}
