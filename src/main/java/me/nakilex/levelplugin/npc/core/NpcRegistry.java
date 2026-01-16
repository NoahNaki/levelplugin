package me.nakilex.levelplugin.npc.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NpcRegistry {
    private final Map<String, PlayerNpc> byName = new HashMap<>();
    private final Map<UUID, PlayerNpc> byId = new HashMap<>();

    public boolean exists(String name) {
        return name != null && byName.containsKey(name.toLowerCase());
    }

    public PlayerNpc getByName(String name) {
        if (name == null) {
            return null;
        }
        return byName.get(name.toLowerCase());
    }

    public PlayerNpc getById(UUID uuid) {
        return uuid == null ? null : byId.get(uuid);
    }

    public void add(PlayerNpc npc) {
        if (npc == null) {
            return;
        }
        byName.put(npc.getName().toLowerCase(), npc);
        byId.put(npc.getUuid(), npc);
    }

    public void remove(PlayerNpc npc) {
        if (npc == null) {
            return;
        }
        byName.remove(npc.getName().toLowerCase());
        byId.remove(npc.getUuid());
    }

    public Collection<PlayerNpc> getAll() {
        return Collections.unmodifiableCollection(byId.values());
    }
}
