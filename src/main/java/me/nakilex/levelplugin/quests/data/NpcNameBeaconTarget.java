package me.nakilex.levelplugin.quests.data;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Beacon target that resolves to a Citizens NPC by matching its (stripped) name.
 */
public class NpcNameBeaconTarget implements BeaconTarget {
    private final String normalizedName;

    public NpcNameBeaconTarget(String npcName) {
        String stripped = npcName == null ? null : ChatColor.stripColor(npcName);
        this.normalizedName = stripped == null ? null : stripped.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public Location resolve(Player viewer) {
        if (normalizedName == null) {
            return null;
        }
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            String npcName = ChatColor.stripColor(npc.getName());
            if (npcName == null) {
                continue;
            }
            String normalizedNpc = npcName.trim().toLowerCase(Locale.ROOT);
            if (!normalizedName.equals(normalizedNpc)) {
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
