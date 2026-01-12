package me.nakilex.npc.core.registry;

import me.nakilex.npc.core.model.Npc;
import org.bukkit.entity.EntityType;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface NpcRegistry {
    Npc create(String name, EntityType type, double x, double y, double z, float yaw, float pitch, String worldName);

    boolean delete(int id);

    Optional<Npc> get(int id);

    Optional<Npc> getByName(String name);

    Collection<Npc> list();

    Npc cloneNpc(int id, String newName);

    void setSelectedNpc(UUID playerId, Integer npcId);

    Optional<Npc> getSelectedNpc(UUID playerId);

    Optional<Integer> getSelectedNpcId(UUID playerId);

    void clearSelection(UUID playerId);
}
