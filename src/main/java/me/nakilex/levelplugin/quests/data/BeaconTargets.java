package me.nakilex.levelplugin.quests.data;

import org.bukkit.Location;

/**
 * Factory helpers for creating {@link BeaconTarget} instances.
 */
public final class BeaconTargets {
    private BeaconTargets() {}

    /**
     * Create a beacon target that always points to the given location.
     */
    public static BeaconTarget staticLoc(Location location) {
        return new StaticBeaconTarget(location);
    }

    /**
     * Create a beacon target that tracks a Citizens NPC by its ID.
     */
    public static BeaconTarget npc(int npcId) {
        return new NpcBeaconTarget(npcId);
    }

    /**
     * Create a beacon target that tracks a Citizens NPC by its (stripped) name.
     */
    public static BeaconTarget npc(String npcName) {
        return new NpcBeaconTarget(npcName);
    }

    /**
     * Create a beacon target that tracks a Citizens NPC by its (stripped) name.
     */
    @Deprecated
    public static BeaconTarget npcByName(String npcName) {
        return npc(npcName);
    }
}

