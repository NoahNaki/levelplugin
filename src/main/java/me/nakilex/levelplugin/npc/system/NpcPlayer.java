package me.nakilex.levelplugin.npc.system;

import org.bukkit.Location;

import java.util.UUID;

public final class NpcPlayer {
    private final UUID uuid;
    private final int entityId;
    private final String name;
    private final Object gameProfile;
    private volatile Location location;

    public NpcPlayer(UUID uuid, int entityId, String name, Object gameProfile, Location location) {
        this.uuid = uuid;
        this.entityId = entityId;
        this.name = name;
        this.gameProfile = gameProfile;
        this.location = location.clone();
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getEntityId() {
        return entityId;
    }

    public String getName() {
        return name;
    }

    public Object getGameProfile() {
        return gameProfile;
    }

    public Location getLocation() {
        return location.clone();
    }

    public void setLocation(Location location) {
        if (location != null) {
            this.location = location.clone();
        }
    }
}
