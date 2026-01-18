package me.nakilex.levelplugin.npc.system;

import me.nakilex.levelplugin.npc.system.trait.NpcTrait;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NPC {
    private final int id;
    private String name;
    private EntityType type;
    private final NpcDataStore dataStore = new NpcDataStore();
    private final Map<Class<? extends NpcTrait>, NpcTrait> traits = new HashMap<>();
    private Location storedLocation;
    private Entity entity;
    private final NpcNavigator navigator = new NpcNavigator(this);
    private boolean persistent = true;

    NPC(int id, EntityType type, String name) {
        this.id = id;
        this.type = type;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        if (entity instanceof LivingEntity living) {
            living.setCustomName(name);
        }
    }

    public EntityType getType() {
        return type;
    }

    public void setBukkitEntityType(EntityType type) {
        if (type != null) {
            this.type = type;
        }
    }

    public Entity getEntity() {
        return entity;
    }

    public boolean isSpawned() {
        return entity != null && entity.isValid();
    }

    public Location getStoredLocation() {
        return storedLocation;
    }

    public void setStoredLocation(Location storedLocation) {
        this.storedLocation = storedLocation == null ? null : storedLocation.clone();
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public void spawn(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        storedLocation = location.clone();
        if (entity != null && entity.isValid()) {
            entity.teleport(location);
            return;
        }
        EntityType spawnType = type == EntityType.PLAYER ? EntityType.ARMOR_STAND : type;
        entity = location.getWorld().spawnEntity(location, spawnType);
        if (entity instanceof LivingEntity living) {
            living.setCustomName(name);
            living.setCustomNameVisible(true);
        }
        if (entity instanceof ArmorStand stand) {
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setGravity(false);
        }
        setIdMetadata();
        traits.values().forEach(trait -> trait.onSpawn(this));
    }

    public void despawn() {
        if (entity != null) {
            traits.values().forEach(trait -> trait.onDespawn(this));
            entity.remove();
            entity = null;
        }
    }

    public void destroy() {
        despawn();
        if (!persistent) {
            try {
                NpcApi.getRegistry().remove(id);
            } catch (IllegalStateException ignored) {
                // registry not initialized
            }
        }
    }

    public void teleport(Location location, PlayerTeleportEvent.TeleportCause cause) {
        if (entity != null && location != null) {
            entity.teleport(location, cause);
        }
    }

    public NpcDataStore data() {
        return dataStore;
    }

    public NpcNavigator getNavigator() {
        return navigator;
    }

    public UUID getEntityUuid() {
        return entity == null ? null : entity.getUniqueId();
    }

    public <T extends NpcTrait> T getOrAddTrait(Class<T> type) {
        NpcTrait existing = traits.get(type);
        if (existing != null) {
            return type.cast(existing);
        }
        try {
            T created = type.getDeclaredConstructor().newInstance();
            traits.put(type, created);
            created.onAttach(this);
            return created;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create trait " + type.getSimpleName(), ex);
        }
    }

    public void removeTrait(Class<? extends NpcTrait> type) {
        NpcTrait trait = traits.remove(type);
        if (trait != null) {
            trait.onDetach(this);
        }
    }

    public <T extends NpcTrait> T getTrait(Class<T> type) {
        return type.cast(traits.get(type));
    }

    public NPC copy(int newId) {
        NPC clone = new NPC(newId, type, name);
        clone.copyFrom(this);
        return clone;
    }

    private void copyFrom(NPC source) {
        setStoredLocation(source.getStoredLocation());
        source.dataStore.snapshot().forEach(dataStore::set);
        traits.putAll(source.traits);
    }

    private void setIdMetadata() {
        if (entity == null || NpcRegistry.getNpcIdKey() == null) {
            return;
        }
        PersistentDataContainer container = entity.getPersistentDataContainer();
        container.set(NpcRegistry.getNpcIdKey(), PersistentDataType.INTEGER, id);
    }
}
