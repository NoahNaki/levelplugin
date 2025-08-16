package me.nakilex.levelplugin.quests.data;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Beacon target that resolves to the location of a Citizens NPC when spawned.
 */
public class NpcBeaconTarget implements BeaconTarget {
    private final int npcId;

    public NpcBeaconTarget(int npcId) {
        this.npcId = npcId;
    }

    @Override
    public Location resolve(Player viewer) {
        NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
        if (npc != null && npc.isSpawned()) {
            return npc.getEntity().getLocation();
        }
        return null;
    }
}

