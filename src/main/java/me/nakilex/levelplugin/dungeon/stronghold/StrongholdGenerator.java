package me.nakilex.levelplugin.dungeon.stronghold;

import me.nakilex.levelplugin.dungeon.stronghold.StrongholdEnums.GraphMode;
import me.nakilex.levelplugin.dungeon.stronghold.StrongholdPlacement.PlacementConfig;
import me.nakilex.levelplugin.dungeon.stronghold.StrongholdPlacement.PlacementResult;

import java.util.List;
import java.util.logging.Logger;

/** End-to-end deterministic stronghold generation with hard graph-regeneration fallback. */
public final class StrongholdGenerator {
    private final Logger logger;

    public StrongholdGenerator(Logger logger) {
        this.logger = logger;
    }

    public PlacementResult generate(GraphMode mode, int roomCount, long seed, int maxGraphAttempts) {
        List<StrongholdModel.Template> catalog = StrongholdTemplateCatalog.currentCatalog();
        int maxTemplateDegree = catalog.stream()
                .mapToInt(StrongholdModel.Template::degree)
                .max()
                .orElse(1);
        PlacementConfig config = new PlacementConfig(1, false);

        for (int attempt = 0; attempt < Math.max(1, maxGraphAttempts); attempt++) {
            long graphSeed = seed + (attempt * 9973L);
            StrongholdGraph.Graph graph = StrongholdGraph.generate(mode, roomCount, graphSeed, maxTemplateDegree);
            PlacementResult result = StrongholdPlacement.placeGraph(graph, catalog, graphSeed, config, logger);
            if (result.success()) {
                return result;
            }
            if (logger != null) {
                logger.warning("Stronghold generation failed for seed=" + graphSeed + ", regenerating graph");
            }
        }
        return new PlacementResult(false, java.util.Map.of(), List.of("Exhausted graph regeneration attempts"));
    }
}
