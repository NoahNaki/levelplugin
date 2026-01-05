package me.nakilex.levelplugin.quests.data;

import me.nakilex.levelplugin.utils.NpcNameUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Beacon target that resolves to the location of a Citizens NPC when spawned.
 */
public class NpcBeaconTarget implements BeaconTarget {
    private final Integer npcId;
    private final String normalizedName;

    public NpcBeaconTarget(int npcId) {
        this.npcId = npcId;
        this.normalizedName = null;
    }

    public NpcBeaconTarget(String npcName) {
        this.npcId = null;
        this.normalizedName = NpcNameUtil.normalize(npcName);
    }

    @Override
    public Location resolve(Player viewer) {
        if (npcId != null) {
            NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
            if (npc != null) {
                Location active = getSpawnedOrStoredLocation(npc);
                if (active != null) {
                    return active;
                }
            }
        }

        if (normalizedName != null) {
            Location viewerLocation = viewer != null ? viewer.getLocation() : null;
            NPC highestSameWorld = null;
            Location highestSameWorldLocation = null;
            NPC highestOverall = null;
            Location highestOverallLocation = null;
            NPC nearestNpc = null;
            Location nearestLocation = null;
            double nearestDistance = Double.MAX_VALUE;
            for (NPC npc : CitizensAPI.getNPCRegistry()) {
                String npcNormalized = NpcNameUtil.normalize(npc.getName());
                if (npcNormalized == null || !normalizedName.equals(npcNormalized)) {
                    continue;
                }
                Location active = getSpawnedOrStoredLocation(npc);
                if (active == null) {
                    continue;
                }
                if (highestOverall == null || npc.getId() > highestOverall.getId()) {
                    highestOverall = npc;
                    highestOverallLocation = active;
                }
                if (viewerLocation != null && viewerLocation.getWorld() != null
                        && viewerLocation.getWorld().equals(active.getWorld())) {
                    double dist = viewerLocation.distanceSquared(active);
                    if (dist < nearestDistance) {
                        nearestDistance = dist;
                        nearestNpc = npc;
                        nearestLocation = active;
                    }
                    if (highestSameWorld == null || npc.getId() > highestSameWorld.getId()) {
                        highestSameWorld = npc;
                        highestSameWorldLocation = active;
                    }
                }
            }
            if (highestSameWorldLocation != null) {
                return highestSameWorldLocation;
            }
            if (nearestLocation != null) {
                return nearestLocation;
            }
            if (highestOverallLocation != null) {
                return highestOverallLocation;
            }
        }
        return null;
    }

    private Location getSpawnedOrStoredLocation(NPC npc) {
        if (npc.isSpawned() && npc.getEntity() != null) {
            return npc.getEntity().getLocation();
        }
        return npc.getStoredLocation();
    }
}
