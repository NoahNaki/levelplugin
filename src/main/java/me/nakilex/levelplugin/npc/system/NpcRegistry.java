package me.nakilex.levelplugin.npc.system;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class NpcRegistry implements Iterable<NPC> {
    private static NamespacedKey npcIdKey;
    private final Map<Integer, NPC> npcs = new HashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public NpcRegistry(Plugin plugin) {
        npcIdKey = new NamespacedKey(plugin, "npc_id");
    }

    public static NamespacedKey getNpcIdKey() {
        return npcIdKey;
    }

    public NPC createNpc(EntityType type, String name) {
        int id = nextId.getAndIncrement();
        NPC npc = new NPC(id, type, name);
        npc.setPersistent(true);
        npcs.put(id, npc);
        return npc;
    }

    public NPC createNPC(EntityType type, String name) {
        return createNpc(type, name);
    }

    public NPC cloneNpc(NPC template) {
        if (template == null) {
            return null;
        }
        int id = nextId.getAndIncrement();
        NPC clone = template.copy(id);
        clone.setPersistent(false);
        npcs.put(id, clone);
        return clone;
    }

    public NPC getById(int id) {
        return npcs.get(id);
    }

    public void register(NPC npc) {
        npcs.put(npc.getId(), npc);
        nextId.set(Math.max(nextId.get(), npc.getId() + 1));
    }

    public void remove(int id) {
        NPC npc = npcs.remove(id);
        if (npc != null) {
            npc.despawn();
        }
    }

    public boolean isNPC(Entity entity) {
        return getNPC(entity) != null;
    }

    public NPC getNPC(Entity entity) {
        if (entity == null || npcIdKey == null) {
            return null;
        }
        PersistentDataContainer container = entity.getPersistentDataContainer();
        Integer npcId = container.get(npcIdKey, PersistentDataType.INTEGER);
        if (npcId == null) {
            return null;
        }
        return getById(npcId);
    }

    public List<NPC> list() {
        List<NPC> list = new ArrayList<>();
        for (NPC npc : npcs.values()) {
            if (npc.isPersistent()) {
                list.add(npc);
            }
        }
        return list;
    }

    public List<NPC> sorted() {
        List<NPC> list = list();
        list.sort(Comparator.comparingInt(NPC::getId));
        return list;
    }

    @Override
    public java.util.Iterator<NPC> iterator() {
        return list().iterator();
    }
}
