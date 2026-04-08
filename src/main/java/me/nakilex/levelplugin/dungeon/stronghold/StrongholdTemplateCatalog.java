package me.nakilex.levelplugin.dungeon.stronghold;

import me.nakilex.levelplugin.dungeon.Direction;
import me.nakilex.levelplugin.dungeon.stronghold.Geometry.BoundingBox;
import me.nakilex.levelplugin.dungeon.stronghold.Geometry.Vec3;
import me.nakilex.levelplugin.dungeon.stronghold.StrongholdEnums.ConnectorType;
import me.nakilex.levelplugin.dungeon.stronghold.StrongholdEnums.TemplateTag;
import me.nakilex.levelplugin.dungeon.stronghold.StrongholdModel.Connector;
import me.nakilex.levelplugin.dungeon.stronghold.StrongholdModel.Template;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Current in-repo stronghold template catalog with normalized local bounds. */
public final class StrongholdTemplateCatalog {
    private StrongholdTemplateCatalog() {}

    public static List<Template> currentCatalog() {
        List<Template> templates = new ArrayList<>();

        templates.add(template("corner_1", box(473, -61, -5346, 543, -38, -5276),
                connectors(Direction.NORTH, Direction.EAST), tags(TemplateTag.CORNER)));
        templates.add(template("corner_2", box(544, -61, -5701, 614, -38, -5631),
                connectors(Direction.SOUTH, Direction.WEST), tags(TemplateTag.CORNER)));
        templates.add(template("corner_3", box(544, -61, -5630, 614, -38, -5560),
                connectors(Direction.NORTH, Direction.WEST), tags(TemplateTag.CORNER)));

        templates.add(template("straight_1", box(402, -61, -5346, 472, -38, -5276),
                connectors(Direction.NORTH, Direction.SOUTH), tags(TemplateTag.STRAIGHT)));
        templates.add(template("straight_2", box(402, -61, -5417, 472, -38, -5347),
                connectors(Direction.NORTH, Direction.SOUTH), tags(TemplateTag.STRAIGHT)));
        templates.add(template("straight_3", box(402, -61, -5488, 472, -38, -5418),
                connectors(Direction.NORTH, Direction.SOUTH), tags(TemplateTag.STRAIGHT)));
        templates.add(template("straight_4", box(402, -61, -5559, 472, -38, -5489),
                connectors(Direction.NORTH, Direction.SOUTH), tags(TemplateTag.STRAIGHT)));
        templates.add(template("straight_5", box(402, -61, -5630, 472, -38, -5560),
                connectors(Direction.NORTH, Direction.SOUTH), tags(TemplateTag.STRAIGHT)));
        templates.add(template("straight_6", box(402, -61, -5701, 472, -38, -5631),
                connectors(Direction.NORTH, Direction.SOUTH), tags(TemplateTag.STRAIGHT)));
        templates.add(template("straight_7", box(473, -61, -5701, 543, -38, -5631),
                connectors(Direction.EAST, Direction.WEST), tags(TemplateTag.STRAIGHT)));
        templates.add(template("straight_8", box(473, -61, -5630, 543, -38, -5560),
                connectors(Direction.EAST, Direction.WEST), tags(TemplateTag.STRAIGHT)));
        templates.add(template("straight_9", box(473, -61, -5417, 543, -38, -5347),
                connectors(Direction.EAST, Direction.WEST), tags(TemplateTag.STRAIGHT)));

        templates.add(template("deadend_1", box(473, -61, -5488, 543, -38, -5418),
                connectors(Direction.WEST), tags(TemplateTag.DEADEND)));
        templates.add(template("deadend_2", box(473, -61, -5559, 543, -38, -5489),
                connectors(Direction.WEST), tags(TemplateTag.DEADEND)));

        templates.add(template("connector_1", box(402, -61, -5711, 412, -38, -5701),
                connectors(Direction.NORTH, Direction.SOUTH), tags(TemplateTag.CONNECTOR)));
        templates.add(template("connector_2", box(402, -61, -5721, 412, -38, -5711),
                connectors(Direction.NORTH, Direction.SOUTH), tags(TemplateTag.CONNECTOR)));

        templates.add(template("tower_1", box(615, -61, -5488, 685, -7, -5418),
                connectors(Direction.WEST), tags(TemplateTag.TOWER, TemplateTag.LARGE, TemplateTag.LANDMARK)));
        templates.add(template("tower_2", box(615, -61, -5276, 685, -7, -5206),
                connectors(Direction.WEST), tags(TemplateTag.TOWER, TemplateTag.LARGE, TemplateTag.LANDMARK)));

        templates.add(template("gate_1", box(614, -61, -5418, 686, -10, -5346),
                connectors(Direction.WEST, Direction.EAST), tags(TemplateTag.GATE, TemplateTag.LARGE, TemplateTag.LANDMARK)));
        templates.add(template("gate_2", box(614, -61, -5346, 686, -10, -5276),
                connectors(Direction.WEST, Direction.EAST), tags(TemplateTag.GATE, TemplateTag.LARGE, TemplateTag.LANDMARK)));

        return List.copyOf(templates);
    }

    private static Template template(String id, BoundingBox worldBounds, List<Connector> connectors, Set<TemplateTag> tags) {
        BoundingBox local = toLocal(worldBounds);
        List<Connector> normalized = normalizeConnectors(local, connectors, worldBounds);
        return new Template(id, local, normalized, tags);
    }

    private static BoundingBox toLocal(BoundingBox worldBounds) {
        Vec3 origin = worldBounds.min();
        Vec3 max = worldBounds.max().subtract(origin);
        return new BoundingBox(new Vec3(0, 0, 0), max);
    }

    private static List<Connector> normalizeConnectors(BoundingBox localBounds, List<Connector> connectors, BoundingBox worldBounds) {
        List<Connector> out = new ArrayList<>(connectors.size());
        int cx = (localBounds.max().x()) / 2;
        int cy = 0;
        int cz = (localBounds.max().z()) / 2;
        for (Connector c : connectors) {
            Vec3 world = c.localPosition();
            Vec3 local = world.subtract(worldBounds.min());
            if (local.x() < 0 || local.y() < 0 || local.z() < 0) {
                // Fallback for direction-only connectors that use a dummy origin.
                local = switch (c.facing()) {
                    case NORTH -> new Vec3(cx, cy, 0);
                    case SOUTH -> new Vec3(cx, cy, localBounds.max().z());
                    case EAST -> new Vec3(localBounds.max().x(), cy, cz);
                    case WEST -> new Vec3(0, cy, cz);
                };
            }
            out.add(new Connector(local, c.facing(), c.type()));
        }
        return out;
    }

    private static BoundingBox box(int x1, int y1, int z1, int x2, int y2, int z2) {
        return new BoundingBox(
                new Vec3(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2)),
                new Vec3(Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2))
        );
    }

    private static List<Connector> connectors(Direction... faces) {
        List<Connector> out = new ArrayList<>(faces.length);
        for (Direction face : faces) {
            out.add(new Connector(new Vec3(-1, -1, -1), face, ConnectorType.CORRIDOR));
        }
        return out;
    }

    private static Set<TemplateTag> tags(TemplateTag... tags) {
        if (tags.length == 0) {
            return EnumSet.noneOf(TemplateTag.class);
        }
        EnumSet<TemplateTag> set = EnumSet.noneOf(TemplateTag.class);
        for (TemplateTag tag : tags) {
            set.add(tag);
        }
        return set;
    }
}
