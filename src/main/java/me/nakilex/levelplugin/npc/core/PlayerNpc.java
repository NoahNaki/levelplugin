package me.nakilex.levelplugin.npc.core;

import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Location;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerNpc {
    private final UUID uuid;
    private final String name;
    private Location location;
    private final ServerPlayer handle;
    private final int entityId;
    private final Set<UUID> viewers = new HashSet<>();

    public PlayerNpc(UUID uuid, String name, Location location, ServerPlayer handle) {
        this.uuid = uuid;
        this.name = name;
        this.location = location;
        this.handle = handle;
        this.entityId = handle.getId();
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

    public ServerPlayer getHandle() {
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
}
