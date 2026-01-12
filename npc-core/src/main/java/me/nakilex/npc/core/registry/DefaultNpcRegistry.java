package me.nakilex.npc.core.registry;

import me.nakilex.npc.core.model.Npc;
import me.nakilex.npc.core.model.NpcPosition;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultNpcRegistry implements NpcRegistry {
    private final Map<Integer, Npc> npcs = new HashMap<>();
    private final Map<UUID, Integer> selections = new HashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    @Override
    public Npc create(String name, EntityType type, double x, double y, double z, float yaw, float pitch, String worldName) {
        int id = nextId.getAndIncrement();
        Npc npc = new Npc(id, name, type, new NpcPosition(worldName, x, y, z, yaw, pitch));
        npcs.put(id, npc);
        return npc;
    }

    @Override
    public boolean delete(int id) {
        return npcs.remove(id) != null;
    }

    @Override
    public Optional<Npc> get(int id) {
        return Optional.ofNullable(npcs.get(id));
    }

    @Override
    public Optional<Npc> getByName(String name) {
        return npcs.values().stream()
                .filter(npc -> npc.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public Collection<Npc> list() {
        return new ArrayList<>(npcs.values());
    }

    @Override
    public Npc cloneNpc(int id, String newName) {
        Npc original = npcs.get(id);
        if (original == null) {
            throw new IllegalArgumentException("NPC " + id + " does not exist");
        }
        int newId = nextId.getAndIncrement();
        Npc clone = original.copy(newId, newName == null ? original.getName() + "_copy" : newName);
        npcs.put(newId, clone);
        return clone;
    }

    @Override
    public void setSelectedNpc(UUID playerId, Integer npcId) {
        if (npcId == null) {
            selections.remove(playerId);
        } else {
            selections.put(playerId, npcId);
        }
    }

    @Override
    public Optional<Npc> getSelectedNpc(UUID playerId) {
        Integer id = selections.get(playerId);
        return id == null ? Optional.empty() : get(id);
    }

    @Override
    public Optional<Integer> getSelectedNpcId(UUID playerId) {
        return Optional.ofNullable(selections.get(playerId));
    }

    @Override
    public void clearSelection(UUID playerId) {
        selections.remove(playerId);
    }

    public void register(Npc npc) {
        npcs.put(npc.getId(), npc);
        nextId.set(Math.max(nextId.get(), npc.getId() + 1));
    }

    public Map<UUID, Integer> getSelectionsSnapshot() {
        return new HashMap<>(selections);
    }

    public void setSelections(Map<UUID, Integer> selectionMap) {
        selections.clear();
        if (selectionMap != null) {
            selections.putAll(selectionMap);
        }
    }

    public void clear() {
        npcs.clear();
        selections.clear();
        nextId.set(1);
    }
}
