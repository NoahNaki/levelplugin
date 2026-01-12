package me.nakilex.npc.core.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

public class NpcPosition {
    private String worldName;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    public NpcPosition(String worldName, double x, double y, double z, float yaw, float pitch) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static NpcPosition fromLocation(Location location) {
        return new NpcPosition(location.getWorld().getName(), location.getX(), location.getY(),
                location.getZ(), location.getYaw(), location.getPitch());
    }

    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public NpcPosition copy() {
        return new NpcPosition(worldName, x, y, z, yaw, pitch);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NpcPosition other)) {
            return false;
        }
        return Objects.equals(worldName, other.worldName)
                && Double.compare(other.x, x) == 0
                && Double.compare(other.y, y) == 0
                && Double.compare(other.z, z) == 0
                && Float.compare(other.yaw, yaw) == 0
                && Float.compare(other.pitch, pitch) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldName, x, y, z, yaw, pitch);
    }
}
