package me.nakilex.levelplugin.dungeon.stronghold;

import me.nakilex.levelplugin.dungeon.stronghold.Geometry.Vec3;
import me.nakilex.levelplugin.dungeon.stronghold.StrongholdEnums.Rotation;
import me.nakilex.levelplugin.dungeon.stronghold.StrongholdModel.Connector;
import me.nakilex.levelplugin.dungeon.stronghold.StrongholdModel.Template;
import me.nakilex.levelplugin.dungeon.stronghold.StrongholdModel.Transform;

/** Canonical placement solver used by all stronghold alignment operations. */
public final class TransformSolver {
    private TransformSolver() {}

    public static Transform solveTransform(
            Template aTemplate,
            Transform tA,
            Connector aConn,
            Template bTemplate,
            Rotation rotB,
            Connector bConn
    ) {
        if (!aTemplate.connectors().contains(aConn)) {
            throw new IllegalArgumentException("aConn is not owned by template " + aTemplate.id());
        }
        if (!bTemplate.connectors().contains(bConn)) {
            throw new IllegalArgumentException("bConn is not owned by template " + bTemplate.id());
        }

        Connector worldAConn = aConn.rotated(tA.rotation());
        Connector worldBConn = bConn.rotated(rotB);
        if (!worldAConn.isCompatibleWith(worldBConn)) {
            throw new IllegalArgumentException("Incompatible connector pairing: "
                    + worldAConn.facing() + " vs " + worldBConn.facing());
        }

        Vec3 worldConnectorA = tA.apply(aConn.localPosition());
        Vec3 bOffset = bConn.localPosition().rotate(rotB);
        Vec3 worldPositionB = worldConnectorA.subtract(bOffset);
        return new Transform(worldPositionB, rotB);
    }
}
