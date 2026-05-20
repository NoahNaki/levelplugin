package me.nakilex.levelplugin.npc;

import net.citizensnpcs.api.CitizensAPI;

public final class NpcCleanupUtil {
    private NpcCleanupUtil() {
    }

    public static PruneResult pruneOrphanCitizensNpcs() {
        int totalChecked = 0;
        int removed = 0;
        for (net.citizensnpcs.api.npc.NPC npc : CitizensAPI.getNPCRegistry()) {
            totalChecked++;
            if (!isOrphanCitizensNpc(npc)) {
                continue;
            }
            CitizensAPI.getNPCRegistry().deregister(npc);
            removed++;
        }
        return new PruneResult(totalChecked, removed);
    }

    public static boolean isOrphanCitizensNpc(net.citizensnpcs.api.npc.NPC npc) {
        if (npc == null) {
            return false;
        }
        if (npc.isSpawned() && npc.getEntity() != null) {
            return false;
        }
        org.bukkit.Location stored = npc.getStoredLocation();
        return stored == null || stored.getWorld() == null;
    }

    public record PruneResult(int totalChecked, int removed) {
    }
}
