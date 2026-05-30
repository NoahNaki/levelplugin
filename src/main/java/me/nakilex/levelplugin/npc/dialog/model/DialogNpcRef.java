package me.nakilex.levelplugin.npc.dialog.model;

import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.npc.system.NpcApi;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

/** Lightweight wrapper that lets dialog code work with either LevelPlugin or Citizens NPCs. */
public final class DialogNpcRef {
    private final NPC npc;
    private final net.citizensnpcs.api.npc.NPC citizensNpc;
    private final int fallbackId;
    private final String fallbackName;

    private DialogNpcRef(NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc, int fallbackId, String fallbackName) {
        this.npc = npc;
        this.citizensNpc = citizensNpc;
        this.fallbackId = fallbackId;
        this.fallbackName = fallbackName;
    }

    public static DialogNpcRef of(NPC npc) {
        return new DialogNpcRef(npc, null, npc == null ? -1 : npc.getId(), npc == null ? "NPC" : npc.getName());
    }

    public static DialogNpcRef of(net.citizensnpcs.api.npc.NPC npc) {
        return new DialogNpcRef(null, npc, npc == null ? -1 : npc.getId(), npc == null ? "NPC" : npc.getName());
    }

    public static DialogNpcRef forId(int npcId, String displayName) {
        return new DialogNpcRef(null, null, npcId, displayName == null || displayName.isBlank() ? "NPC" : displayName);
    }

    public NPC npc() { return npc; }
    public net.citizensnpcs.api.npc.NPC citizensNpc() { return citizensNpc; }

    public int id() {
        if (npc != null) return npc.getId();
        return citizensNpc != null ? citizensNpc.getId() : fallbackId;
    }

    public String name() {
        if (npc != null) return npc.getName();
        return citizensNpc != null ? citizensNpc.getName() : fallbackName;
    }

    public Location location() {
        if (npc != null) {
            if (npc.isSpawned() && npc.getEntity() != null) return npc.getEntity().getLocation();
            return npc.getStoredLocation();
        }
        if (citizensNpc != null) {
            if (citizensNpc.isSpawned() && citizensNpc.getEntity() != null) return citizensNpc.getEntity().getLocation();
            return citizensNpc.getStoredLocation();
        }
        return null;
    }

    public boolean matches(DialogNpcRef other) {
        return other != null && id() >= 0 && id() == other.id();
    }

    public boolean matches(Entity entity) {
        if (entity == null) return false;
        NPC clickedNpc = NpcApi.getRegistry().getNPC(entity);
        net.citizensnpcs.api.npc.NPC clickedCitizens = CitizensAPI.getNPCRegistry().getNPC(entity);
        int expectedId = id();
        return expectedId >= 0 && ((clickedNpc != null && clickedNpc.getId() == expectedId)
                || (clickedCitizens != null && clickedCitizens.getId() == expectedId));
    }
}
