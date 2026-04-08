package me.nakilex.levelplugin.dungeon.stronghold;

import me.nakilex.levelplugin.dungeon.stronghold.StrongholdPlacement.PlacedRoom;

import java.util.Collection;
import java.util.logging.Logger;

/** Debug controls and structured placement logging hooks for stronghold generation. */
public final class StrongholdDebug {
    private boolean enabled;
    private double overlapTolerance = 0.0D;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setOverlapTolerance(double overlapTolerance) {
        this.overlapTolerance = Math.max(0.0D, overlapTolerance);
    }

    public double overlapTolerance() {
        return overlapTolerance;
    }

    public void logPlacement(Logger logger, String templateId, String rotation, String connectorPairing, String outcome) {
        if (!enabled || logger == null) {
            return;
        }
        logger.info("[StrongholdDebug] template=" + templateId
                + " rotation=" + rotation
                + " connectors=" + connectorPairing
                + " outcome=" + outcome);
    }

    public void render(Logger logger, Collection<PlacedRoom> rooms) {
        if (!enabled || logger == null) {
            return;
        }
        for (PlacedRoom room : rooms) {
            logger.info("[StrongholdDebug] bbox=" + room.worldBounds()
                    + " connectors=" + room.template().connectors().size()
                    + " template=" + room.template().id());
        }
    }
}
