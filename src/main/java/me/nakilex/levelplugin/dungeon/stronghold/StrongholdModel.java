package me.nakilex.levelplugin.dungeon.stronghold;

import me.nakilex.levelplugin.dungeon.Direction;
import me.nakilex.levelplugin.dungeon.stronghold.Geometry.BoundingBox;
import me.nakilex.levelplugin.dungeon.stronghold.Geometry.Vec3;
import me.nakilex.levelplugin.dungeon.stronghold.StrongholdEnums.ConnectorType;
import me.nakilex.levelplugin.dungeon.stronghold.StrongholdEnums.Rotation;
import me.nakilex.levelplugin.dungeon.stronghold.StrongholdEnums.TemplateTag;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Core immutable template and transform model. */
public final class StrongholdModel {
    private StrongholdModel() {}

    public record Connector(Vec3 localPosition, Direction facing, ConnectorType type) {
        public Connector {
            Objects.requireNonNull(localPosition, "localPosition");
            Objects.requireNonNull(facing, "facing");
            Objects.requireNonNull(type, "type");
        }

        public Connector rotated(Rotation rotation) {
            return new Connector(localPosition.rotate(rotation), rotation.rotate(facing), type);
        }

        public boolean isCompatibleWith(Connector other) {
            return facing == other.facing.opposite() && type == other.type;
        }
    }

    public record Template(String id, BoundingBox localBounds, List<Connector> connectors, Set<TemplateTag> tags) {
        public Template {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(localBounds, "localBounds");
            connectors = List.copyOf(connectors);
            tags = Collections.unmodifiableSet(tags.isEmpty() ? EnumSet.noneOf(TemplateTag.class) : EnumSet.copyOf(tags));
        }

        public int degree() {
            return connectors.size();
        }

        public Connector rotatedConnector(Connector connector, Rotation rotation) {
            if (!connectors.contains(connector)) {
                throw new IllegalArgumentException("Connector does not belong to template " + id);
            }
            return connector.rotated(rotation);
        }

        public BoundingBox rotatedBounds(Rotation rotation) {
            return localBounds.rotate(rotation);
        }
    }

    public record Transform(Vec3 position, Rotation rotation) {
        public Transform {
            Objects.requireNonNull(position, "position");
            Objects.requireNonNull(rotation, "rotation");
        }

        public Vec3 apply(Vec3 local) {
            return position.add(local.rotate(rotation));
        }
    }
}
