// VectorUtil.java
package me.nakilex.levelplugin.effects.utils;

import org.bukkit.util.Vector;

public class VectorUtil {

    public static Vector rotateAroundAxisY(Vector vector, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = vector.getX() * cos - vector.getZ() * sin;
        double z = vector.getX() * sin + vector.getZ() * cos;
        return new Vector(x, vector.getY(), z);
    }
}
