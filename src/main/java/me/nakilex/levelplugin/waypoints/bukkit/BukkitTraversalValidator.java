package me.nakilex.levelplugin.waypoints.bukkit;

import me.nakilex.levelplugin.waypoints.api.pathing.processing.ValidationProcessor;
import me.nakilex.levelplugin.waypoints.api.pathing.processing.context.EvaluationContext;
import me.nakilex.levelplugin.waypoints.api.pathing.processing.context.SearchContext;
import me.nakilex.levelplugin.waypoints.api.provider.NavigationPointProvider;
import me.nakilex.levelplugin.waypoints.api.wrapper.PathPosition;

/**
 * Ensures nodes are traversable and reachable with player-like step heights.
 */
public class BukkitTraversalValidator implements ValidationProcessor {

    @Override
    public boolean isValid(EvaluationContext context) {
        if (context == null) {
            return false;
        }
        NavigationPointProvider provider = context.getNavigationPointProvider();
        if (provider == null) {
            return false;
        }
        if (!provider.getNavigationPoint(context.getCurrentPathPosition(), context.getEnvironmentContext()).isTraversable()) {
            return false;
        }

        PathPosition previous = context.getPreviousPathPosition();
        if (previous == null) {
            return true;
        }

        PathPosition current = context.getCurrentPathPosition();
        int dx = current.getFlooredX() - previous.getFlooredX();
        int dy = current.getFlooredY() - previous.getFlooredY();
        int dz = current.getFlooredZ() - previous.getFlooredZ();

        int absDx = Math.abs(dx);
        int absDy = Math.abs(dy);
        int absDz = Math.abs(dz);

        if (absDx > 1 || absDz > 1) {
            return false;
        }
        if (absDy > 1) {
            return false;
        }
        return absDx + absDz > 0;
    }

    @Override
    public void initializeSearch(SearchContext searchContext) {
        // No initialization required.
    }

    @Override
    public void finalizeSearch(SearchContext searchContext) {
        // No cleanup required.
    }
}
