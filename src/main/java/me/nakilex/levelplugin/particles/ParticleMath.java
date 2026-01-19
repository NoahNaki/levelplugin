package me.nakilex.levelplugin.particles;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public final class ParticleMath {
    private ParticleMath() {}

    public static Vector buildOffset(double angle, double radius, ParticlePlane plane) {
        ParticlePlane resolved = plane == null ? ParticlePlane.Y : plane;
        double cos = Math.cos(angle) * radius;
        double sin = Math.sin(angle) * radius;
        return switch (resolved) {
            case X -> new Vector(0, cos, sin);
            case Z -> new Vector(cos, sin, 0);
            case LOOK, Y -> new Vector(cos, 0, sin);
        };
    }

    public static Vector mapToPlane(Vector base, ParticlePlane plane) {
        if (base == null) {
            return new Vector();
        }
        ParticlePlane resolved = plane == null ? ParticlePlane.Y : plane;
        return switch (resolved) {
            case X -> new Vector(0, base.getX(), base.getZ());
            case Z -> new Vector(base.getX(), base.getZ(), 0);
            case LOOK, Y -> base.clone();
        };
    }

    public static Vector addHeight(Vector offset, double height, ParticlePlane plane, Location orientation) {
        ParticlePlane resolved = plane == null ? ParticlePlane.Y : plane;
        if (resolved == ParticlePlane.X) {
            return offset.clone().add(new Vector(height, 0, 0));
        }
        if (resolved == ParticlePlane.Z) {
            return offset.clone().add(new Vector(0, 0, height));
        }
        if (resolved == ParticlePlane.LOOK && orientation != null) {
            Vector direction = orientation.getDirection().clone().normalize().multiply(height);
            return offset.clone().add(direction);
        }
        return offset.clone().add(new Vector(0, height, 0));
    }

    public static Vector rotateByAxis(Vector vector, ParticleRotationAxis axis, double degrees) {
        if (axis == null || Math.abs(degrees) < 0.0001) {
            return vector.clone();
        }
        Vector rotated = vector.clone();
        double radians = Math.toRadians(degrees);
        switch (axis) {
            case X -> rotated.rotateAroundX(radians);
            case Y -> rotated.rotateAroundY(radians);
            case Z -> rotated.rotateAroundZ(radians);
        }
        return rotated;
    }

    public static Vector rotateByOrientation(Vector vector, Location orientation) {
        if (orientation == null) {
            return vector.clone();
        }
        Vector rotated = vector.clone();
        double yaw = Math.toRadians(-orientation.getYaw());
        double pitch = Math.toRadians(orientation.getPitch());
        rotated.rotateAroundY(yaw);
        rotated.rotateAroundX(pitch);
        return rotated;
    }

    public static Vector orientAndTilt(Vector vector, ParticlePlane plane, Location orientation,
                                       ParticleRotationAxis tiltAxis, double tiltDegrees) {
        Vector rotated = vector == null ? new Vector() : vector.clone();
        if (plane == ParticlePlane.LOOK) {
            rotated = rotateByOrientation(rotated, orientation);
        }
        return rotateByAxis(rotated, tiltAxis, tiltDegrees);
    }
}
