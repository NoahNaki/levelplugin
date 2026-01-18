package me.nakilex.levelplugin.quests.data;

import me.nakilex.levelplugin.npc.system.NpcApi;
import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.utils.NpcNameUtil;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Beacon target that resolves to the location of a tracked NPC when spawned.
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

    public Integer getNpcId() {
        return npcId;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    @Override
    public Location resolve(Player viewer) {
        if (npcId != null) {
            NPC npc = NpcApi.getRegistry().getById(npcId);
            if (npc != null) {
                Location active = getSpawnedOrStoredLocation(npc);
                if (active != null) {
                    return active;
                }
            }
            net.citizensnpcs.api.npc.NPC citizensNpc = CitizensAPI.getNPCRegistry().getById(npcId);
            Location citizensLocation = getSpawnedOrStoredLocation(citizensNpc);
            if (citizensLocation != null) {
                return citizensLocation;
            }
        }

        if (normalizedName != null) {
            Location viewerLocation = viewer != null ? viewer.getLocation() : null;
            NPC highestSameWorld = null;
            Location highestSameWorldLocation = null;
            NPC highestOverall = null;
            Location highestOverallLocation = null;
            net.citizensnpcs.api.npc.NPC highestSameWorldCitizen = null;
            Location highestSameWorldCitizenLocation = null;
            net.citizensnpcs.api.npc.NPC highestOverallCitizen = null;
            Location highestOverallCitizenLocation = null;
            Location nearestLocation = null;
            Location nearestCitizenLocation = null;
            double nearestDistance = Double.MAX_VALUE;
            double nearestCitizenDistance = Double.MAX_VALUE;
            for (NPC npc : NpcApi.getRegistry()) {
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
                        nearestLocation = active;
                    }
                    if (highestSameWorld == null || npc.getId() > highestSameWorld.getId()) {
                        highestSameWorld = npc;
                        highestSameWorldLocation = active;
                    }
                }
            }
            for (net.citizensnpcs.api.npc.NPC npc : CitizensAPI.getNPCRegistry()) {
                String npcNormalized = NpcNameUtil.normalize(npc.getName());
                if (npcNormalized == null || !normalizedName.equals(npcNormalized)) {
                    continue;
                }
                Location active = getSpawnedOrStoredLocation(npc);
                if (active == null) {
                    continue;
                }
                if (highestOverallCitizen == null || npc.getId() > highestOverallCitizen.getId()) {
                    highestOverallCitizen = npc;
                    highestOverallCitizenLocation = active;
                }
                if (viewerLocation != null && viewerLocation.getWorld() != null
                        && viewerLocation.getWorld().equals(active.getWorld())) {
                    double dist = viewerLocation.distanceSquared(active);
                    if (dist < nearestCitizenDistance) {
                        nearestCitizenDistance = dist;
                        nearestCitizenLocation = active;
                    }
                    if (highestSameWorldCitizen == null || npc.getId() > highestSameWorldCitizen.getId()) {
                        highestSameWorldCitizen = npc;
                        highestSameWorldCitizenLocation = active;
                    }
                }
            }
            if (highestSameWorldLocation != null) {
                return highestSameWorldLocation;
            }
            if (highestSameWorldCitizenLocation != null) {
                return highestSameWorldCitizenLocation;
            }
            if (nearestLocation != null) {
                return nearestLocation;
            }
            if (nearestCitizenLocation != null) {
                return nearestCitizenLocation;
            }
            if (highestOverallLocation != null) {
                return highestOverallLocation;
            }
            if (highestOverallCitizenLocation != null) {
                return highestOverallCitizenLocation;
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

    private Location getSpawnedOrStoredLocation(net.citizensnpcs.api.npc.NPC npc) {
        if (npc == null) {
            return null;
        }
        if (npc.isSpawned() && npc.getEntity() != null) {
            return npc.getEntity().getLocation();
        }
        return npc.getStoredLocation();
    }
}
