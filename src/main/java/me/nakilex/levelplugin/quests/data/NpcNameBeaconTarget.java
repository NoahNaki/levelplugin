package me.nakilex.levelplugin.quests.data;

import me.nakilex.levelplugin.utils.NpcNameUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Beacon target that resolves to a Citizens NPC by matching its (stripped) name.
 */
public class NpcNameBeaconTarget implements BeaconTarget {
    private final String normalizedName;

    public NpcNameBeaconTarget(String npcName) {
        this.normalizedName = NpcNameUtil.normalize(npcName);
    }

    @Override
    public Location resolve(Player viewer) {
        if (normalizedName == null) {
            return null;
        }
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            String npcNormalized = NpcNameUtil.normalize(npc.getName());
            if (npcNormalized == null || !normalizedName.equals(npcNormalized)) {
                continue;
            }
            if (npc.isSpawned() && npc.getEntity() != null) {
                return npc.getEntity().getLocation();
            }
            Location stored = npc.getStoredLocation();
            if (stored != null) {
                return stored;
            }
        }
        return null;
    }
}
