package me.nakilex.levelplugin.dungeon.stronghold;

import me.nakilex.levelplugin.dungeon.stronghold.StrongholdEnums.Rotation;

/** Immutable geometry primitives for transform-safe placement. */
public final class Geometry {
    private Geometry() {}

    public record Vec3(int x, int y, int z) {
        public Vec3 add(Vec3 other) {
            return new Vec3(x + other.x, y + other.y, z + other.z);
        }

        public Vec3 subtract(Vec3 other) {
            return new Vec3(x - other.x, y - other.y, z - other.z);
        }

        public Vec3 rotate(Rotation rotation) {
            return switch (rotation) {
                case R0 -> this;
                case R90 -> new Vec3(-z, y, x);
                case R180 -> new Vec3(-x, y, -z);
                case R270 -> new Vec3(z, y, -x);
            };
        }
    }

    public record BoundingBox(Vec3 min, Vec3 max) {
        public BoundingBox {
            if (min.x() > max.x() || min.y() > max.y() || min.z() > max.z()) {
                throw new IllegalArgumentException("BoundingBox min must be <= max on all axes");
            }
        }

        public BoundingBox translate(Vec3 delta) {
            return new BoundingBox(min.add(delta), max.add(delta));
        }

        public BoundingBox expand(int margin) {
            if (margin <= 0) {
                return this;
            }
            return new BoundingBox(
                    new Vec3(min.x() - margin, min.y() - margin, min.z() - margin),
                    new Vec3(max.x() + margin, max.y() + margin, max.z() + margin)
            );
        }

        public BoundingBox rotate(Rotation rotation) {
            Vec3[] corners = new Vec3[]{
                    new Vec3(min.x(), min.y(), min.z()),
                    new Vec3(min.x(), min.y(), max.z()),
                    new Vec3(max.x(), min.y(), min.z()),
                    new Vec3(max.x(), min.y(), max.z()),
                    new Vec3(min.x(), max.y(), min.z()),
                    new Vec3(min.x(), max.y(), max.z()),
                    new Vec3(max.x(), max.y(), min.z()),
                    new Vec3(max.x(), max.y(), max.z())
            };
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (Vec3 corner : corners) {
                Vec3 rotated = corner.rotate(rotation);
                minX = Math.min(minX, rotated.x());
                minY = Math.min(minY, rotated.y());
                minZ = Math.min(minZ, rotated.z());
                maxX = Math.max(maxX, rotated.x());
                maxY = Math.max(maxY, rotated.y());
                maxZ = Math.max(maxZ, rotated.z());
            }
            return new BoundingBox(new Vec3(minX, minY, minZ), new Vec3(maxX, maxY, maxZ));
        }

        public boolean intersects(BoundingBox other) {
            return min.x() <= other.max.x() && max.x() >= other.min.x()
                    && min.y() <= other.max.y() && max.y() >= other.min.y()
                    && min.z() <= other.max.z() && max.z() >= other.min.z();
        }
    }
}
