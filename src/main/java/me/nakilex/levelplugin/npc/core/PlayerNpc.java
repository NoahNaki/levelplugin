package me.nakilex.levelplugin.npc.core;

import org.bukkit.Location;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerNpc {
    private final UUID uuid;
    private final String name;
    private Location location;
    private final Object handle;
    private final int entityId;
    private final Set<UUID> viewers = new HashSet<>();

    public PlayerNpc(UUID uuid, String name, Location location, Object handle) {
        this.uuid = uuid;
        this.name = name;
        this.location = location;
        this.handle = handle;
        this.entityId = readEntityId(handle);
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Object getHandle() {
        return handle;
    }

    public int getEntityId() {
        return entityId;
    }

    public Set<UUID> getViewers() {
        return Collections.unmodifiableSet(viewers);
    }

    public boolean hasViewer(UUID viewerId) {
        return viewers.contains(viewerId);
    }

    public void addViewer(UUID viewerId) {
        viewers.add(viewerId);
    }

    public void removeViewer(UUID viewerId) {
        viewers.remove(viewerId);
    }

    public void clearViewers() {
        viewers.clear();
    }

    private int readEntityId(Object handle) {
        if (handle == null) {
            return -1;
        }
        try {
            Object id = handle.getClass().getMethod("getId").invoke(handle);
            if (id instanceof Integer intId) {
                return intId;
            }
        } catch (ReflectiveOperationException ignored) {
            // ignore
        }
        return -1;
    }
}
