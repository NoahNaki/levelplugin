package me.nakilex.levelplugin.debug;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.Direction;
import me.nakilex.levelplugin.dungeon.Dungeon;
import me.nakilex.levelplugin.dungeon.DungeonManager;
import me.nakilex.levelplugin.dungeon.RoomTemplate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Debug helper that spawns randomized stronghold wall chains from flatland templates. */
public class StrongholdDebugManager {
    private final Main plugin;
    private final DungeonManager dungeonManager;
    private final Map<UUID, Dungeon> activeStrongholds = new HashMap<>();
    private final Map<UUID, Map<Location, org.bukkit.block.data.BlockData>> activeRestoreSnapshots = new HashMap<>();
    private static final Set<Material> STRONGHOLD_IGNORED_MATERIALS =
            Set.of(Material.WHITE_CONCRETE, Material.LIGHT_BLUE_CONCRETE);

    private final List<RoomTemplate> cornerTemplates = new ArrayList<>();
    private final List<RoomTemplate> straightTemplates = new ArrayList<>();
    private final List<RoomTemplate> deadEndTemplates = new ArrayList<>();
    private final List<RoomTemplate> connectorTemplates = new ArrayList<>();

    private boolean templatesLoaded;

    public StrongholdDebugManager(Main plugin, DungeonManager dungeonManager) {
        this.plugin = plugin;
        this.dungeonManager = dungeonManager;
    }

    public SpawnResult spawn(Player player, int sideLength) {
        if (player == null) {
            return SpawnResult.error("Player was null.");
        }
        if (sideLength < 1) {
            return SpawnResult.error("Size must be at least 1.");
        }
        if (!ensureTemplatesLoaded()) {
            return SpawnResult.error("Stronghold templates failed to load. Check flatland world/coords.");
        }

        despawn(player.getUniqueId());

        Location origin = player.getLocation().getBlock().getLocation();
        World world = origin.getWorld();
        if (world == null) {
            return SpawnResult.error("Could not resolve target world.");
        }

        Dungeon stronghold = new Dungeon(world, "debug_stronghold_" + player.getUniqueId());
        Map<Location, org.bukkit.block.data.BlockData> restoreSnapshot = new LinkedHashMap<>();
        Map<GridPoint, Set<Direction>> graph = generateSnakeGraph(sideLength, player.getUniqueId());
        if (graph.isEmpty()) {
            return SpawnResult.error("Could not generate a wall layout.");
        }

        PlacementResult placement = placeGraphAligned(stronghold, origin, graph, restoreSnapshot);
        if (!placement.success) {
            restoreSnapshot(restoreSnapshot);
            return SpawnResult.error(placement.errorMessage);
        }

        activeStrongholds.put(player.getUniqueId(), stronghold);
        activeRestoreSnapshots.put(player.getUniqueId(), restoreSnapshot);
        return SpawnResult.success(placement.piecesPlaced, resolveStep());
    }

    public boolean despawn(UUID playerId) {
        Dungeon existing = activeStrongholds.remove(playerId);
        Map<Location, org.bukkit.block.data.BlockData> snapshot = activeRestoreSnapshots.remove(playerId);
        if (snapshot != null && !snapshot.isEmpty()) {
            restoreSnapshot(snapshot);
        }
        if (existing == null) {
            return false;
        }
        return true;
    }

    private boolean ensureTemplatesLoaded() {
        if (templatesLoaded) {
            return !cornerTemplates.isEmpty() && !straightTemplates.isEmpty() && !connectorTemplates.isEmpty();
        }
        templatesLoaded = true;

        World world = Bukkit.getWorld("flatland");
        if (world == null) {
            plugin.getLogger().warning("[StrongholdDebug] flatland world not found.");
            return false;
        }

        // Corner pieces
        cornerTemplates.add(RoomTemplate.capture(world, 473, -38, -5346, 543, -61, -5276, false));
        cornerTemplates.add(RoomTemplate.capture(world, 544, -38, -5631, 614, -61, -5701, false));
        cornerTemplates.add(RoomTemplate.capture(world, 614, -61, -5630, 544, -38, -5560, false));

        // Straight pieces
        straightTemplates.add(RoomTemplate.capture(world, 402, -38, -5276, 472, -61, -5346, false));
        straightTemplates.add(RoomTemplate.capture(world, 472, -61, -5347, 402, -38, -5417, false));
        straightTemplates.add(RoomTemplate.capture(world, 402, -38, -5418, 472, -61, -5488, false));
        straightTemplates.add(RoomTemplate.capture(world, 472, -61, -5489, 402, -38, -5559, false));
        straightTemplates.add(RoomTemplate.capture(world, 402, -38, -5560, 472, -61, -5630, false));
        straightTemplates.add(RoomTemplate.capture(world, 472, -61, -5631, 402, -38, -5701, false));
        straightTemplates.add(RoomTemplate.capture(world, 473, -38, -5701, 543, -61, -5631, false));
        straightTemplates.add(RoomTemplate.capture(world, 543, -61, -5630, 473, -38, -5560, false));
        straightTemplates.add(RoomTemplate.capture(world, 473, -38, -5417, 543, -61, -5347, false));

        // Dead-end pieces
        deadEndTemplates.add(RoomTemplate.capture(world, 543, -38, -5418, 473, -61, -5488, false));
        deadEndTemplates.add(RoomTemplate.capture(world, 473, -61, -5489, 543, -38, -5559, false));

        connectorTemplates.add(RoomTemplate.capture(world, 412, -61, -5711, 402, -38, -5701, false));
        connectorTemplates.add(RoomTemplate.capture(world, 402, -38, -5721, 412, -61, -5711, false));

        return !cornerTemplates.isEmpty() && !straightTemplates.isEmpty() && !connectorTemplates.isEmpty();
    }

    private int resolveStep() {
        RoomTemplate sample = straightTemplates.get(0);
        List<RoomTemplate.Connector> connectors = sample.getConnectors();
        if (connectors.size() >= 2) {
            RoomTemplate.Connector first = connectors.get(0);
            for (RoomTemplate.Connector next : connectors) {
                if (first.facing != next.facing) {
                    int dx = Math.abs(first.x - next.x);
                    int dz = Math.abs(first.z - next.z);
                    int size = Math.max(dx, dz);
                    if (size > 0) {
                        return size;
                    }
                }
            }
        }
        return Math.max(1, sample.getWidth() - 1);
    }

    private RoomTemplate selectTemplate(Set<Direction> dirs) {
        List<RoomTemplate> candidates;
        if (dirs.size() == 2) {
            boolean opposite = (dirs.contains(Direction.NORTH) && dirs.contains(Direction.SOUTH))
                    || (dirs.contains(Direction.EAST) && dirs.contains(Direction.WEST));
            candidates = opposite ? straightTemplates : cornerTemplates;
        } else if (dirs.size() == 1) {
            candidates = deadEndTemplates;
        } else {
            return null;
        }

        for (RoomTemplate template : candidates) {
            int rotation = dungeonManager.findRotation(template, dirs);
            if (template.getRotatedDirections(rotation).equals(dirs)) {
                return template;
            }
        }
        return null;
    }

    private PlacementResult placeGraphAligned(Dungeon stronghold, Location origin, Map<GridPoint, Set<Direction>> graph,
                                              Map<Location, org.bukkit.block.data.BlockData> restoreSnapshot) {
        GridPoint root = new GridPoint(0, 0);
        if (!graph.containsKey(root)) {
            root = graph.keySet().iterator().next();
        }

        Map<GridPoint, PlacedPiece> placed = new HashMap<>();
        PlacementResult rootPlacement = placeSinglePiece(stronghold, root, origin.clone(), graph, placed, restoreSnapshot);
        if (!rootPlacement.success) {
            return rootPlacement;
        }

        Set<GridPoint> pending = new HashSet<>(graph.keySet());
        pending.remove(root);

        while (!pending.isEmpty()) {
            boolean progressed = false;
            for (GridPoint point : new ArrayList<>(pending)) {
                Set<Direction> openSides = graph.get(point);
                RoomTemplate template = selectTemplate(openSides);
                if (template == null) {
                    return PlacementResult.error("No template matched connector pattern " + openSides + ".");
                }
                int rotation = dungeonManager.findRotation(template, openSides);
                Location center = resolveAlignedCenter(point, template, rotation, graph, placed);
                if (center == null) {
                    continue;
                }

                PlacementResult placement = placeSinglePiece(stronghold, point, center, graph, placed, restoreSnapshot);
                if (!placement.success) {
                    return placement;
                }
                pending.remove(point);
                progressed = true;
            }
            if (!progressed) {
                GridPoint failed = pending.iterator().next();
                return PlacementResult.error("Failed to align piece at " + failed.x + "," + failed.z + ".");
            }
        }
        int connectorCount = placeInterConnectors(stronghold, graph, placed, restoreSnapshot);
        if (connectorCount < 0) {
            return PlacementResult.error("Failed to place connector bridge pieces.");
        }
        return PlacementResult.success(placed.size() + connectorCount);
    }

    private PlacementResult placeSinglePiece(Dungeon stronghold, GridPoint point, Location center,
                                             Map<GridPoint, Set<Direction>> graph, Map<GridPoint, PlacedPiece> placed,
                                             Map<Location, org.bukkit.block.data.BlockData> restoreSnapshot) {
        Set<Direction> openSides = graph.get(point);
        RoomTemplate template = selectTemplate(openSides);
        if (template == null) {
            return PlacementResult.error("No template matched connector pattern " + openSides + ".");
        }
        int rotation = dungeonManager.findRotation(template, openSides);
        capturePieceReplacements(center, template, rotation, restoreSnapshot);
        if (!placeTemplate(stronghold, template, rotation, center, restoreSnapshot)) {
            return PlacementResult.error("Failed to paste piece at grid " + point.x + "," + point.z + ".");
        }
        placed.put(point, new PlacedPiece(template, rotation, center));
        return PlacementResult.success(1);
    }

    private boolean placeTemplate(Dungeon stronghold, RoomTemplate template, int rotation, Location center,
                                  Map<Location, org.bukkit.block.data.BlockData> restoreSnapshot) {
        DungeonManager.PasteResult result = dungeonManager.pasteRoom(
                stronghold, template, rotation, center, null, false, STRONGHOLD_IGNORED_MATERIALS);
        if (result.instance() == null) {
            return false;
        }
        patchConnectorPlaceholders(center, template, rotation, restoreSnapshot);
        return true;
    }

    private int placeInterConnectors(Dungeon stronghold, Map<GridPoint, Set<Direction>> graph,
                                     Map<GridPoint, PlacedPiece> placed,
                                     Map<Location, org.bukkit.block.data.BlockData> restoreSnapshot) {
        Set<String> visitedEdges = new HashSet<>();
        int placedConnectors = 0;
        for (Map.Entry<GridPoint, Set<Direction>> entry : graph.entrySet()) {
            GridPoint point = entry.getKey();
            PlacedPiece source = placed.get(point);
            if (source == null) continue;

            for (Direction direction : entry.getValue()) {
                GridPoint neighborPoint = point.move(direction);
                if (!graph.containsKey(neighborPoint)) continue;
                String edgeKey = edgeKey(point, neighborPoint);
                if (!visitedEdges.add(edgeKey)) continue;
                PlacedPiece neighbor = placed.get(neighborPoint);
                if (neighbor == null) continue;

                RoomTemplate connectorTemplate = selectConnectorTemplate(direction);
                if (connectorTemplate == null) return -1;
                Set<Direction> straightDirs = Set.of(direction, direction.opposite());
                int connectorRotation = dungeonManager.findRotation(connectorTemplate, straightDirs);

                RoomTemplate.Connector sourceConnector = findConnector(source.template, source.rotation, direction);
                RoomTemplate.Connector connectorSide = findConnector(connectorTemplate, connectorRotation, direction.opposite());
                if (sourceConnector == null || connectorSide == null) return -1;

                Location sourceConnectorLoc = connectorWorldLocation(source.center, source.template, source.rotation, sourceConnector);
                Location connectorCenter = centerFromConnector(sourceConnectorLoc, connectorTemplate, connectorRotation, connectorSide);

                capturePieceReplacements(connectorCenter, connectorTemplate, connectorRotation, restoreSnapshot);
                if (!placeTemplate(stronghold, connectorTemplate, connectorRotation, connectorCenter, restoreSnapshot)) {
                    return -1;
                }
                placedConnectors++;
            }
        }
        return placedConnectors;
    }

    private String edgeKey(GridPoint a, GridPoint b) {
        String left = a.x + "," + a.z;
        String right = b.x + "," + b.z;
        return left.compareTo(right) <= 0 ? left + "|" + right : right + "|" + left;
    }

    private RoomTemplate selectConnectorTemplate(Direction direction) {
        Set<Direction> dirs = Set.of(direction, direction.opposite());
        for (RoomTemplate template : connectorTemplates) {
            int rotation = dungeonManager.findRotation(template, dirs);
            if (template.getRotatedDirections(rotation).equals(dirs)) {
                return template;
            }
        }
        return null;
    }

    private void capturePieceReplacements(Location center, RoomTemplate template, int rotation,
                                          Map<Location, org.bukkit.block.data.BlockData> restoreSnapshot) {
        if (center.getWorld() == null) {
            return;
        }
        for (RoomTemplate.BlockDef block : template.getBlocks()) {
            if (shouldSkipForStrongholdPaste(block.data.getMaterial())) {
                continue;
            }
            Location worldLoc = resolveWorldLocation(center, template, rotation, block.x, block.y, block.z);
            rememberOriginalBlock(worldLoc, restoreSnapshot);
        }
    }

    private void patchConnectorPlaceholders(Location center, RoomTemplate template, int rotation,
                                            Map<Location, org.bukkit.block.data.BlockData> restoreSnapshot) {
        Map<String, RoomTemplate.BlockDef> blockIndex = indexBlocks(template);
        for (RoomTemplate.BlockDef marker : template.getBlocks()) {
            if (marker.data.getMaterial() != Material.REDSTONE_BLOCK) {
                continue;
            }
            RoomTemplate.BlockDef replacement = findNeighborReplacement(blockIndex, marker.x, marker.y, marker.z);
            if (replacement == null) {
                continue;
            }
            Location worldLoc = resolveWorldLocation(center, template, rotation, marker.x, marker.y, marker.z);
            rememberOriginalBlock(worldLoc, restoreSnapshot);
            org.bukkit.block.data.BlockData data = RoomTemplate.rotateBlockData(replacement.data, rotation);
            worldLoc.getBlock().setBlockData(data, false);
        }
    }

    private Map<String, RoomTemplate.BlockDef> indexBlocks(RoomTemplate template) {
        Map<String, RoomTemplate.BlockDef> index = new HashMap<>();
        for (RoomTemplate.BlockDef block : template.getBlocks()) {
            index.put(key(block.x, block.y, block.z), block);
        }
        return index;
    }

    private RoomTemplate.BlockDef findNeighborReplacement(Map<String, RoomTemplate.BlockDef> blockIndex, int x, int y, int z) {
        int[][] offsets = new int[][]{
                {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
                {0, 1, 0}, {0, -1, 0},
                {2, 0, 0}, {-2, 0, 0}, {0, 0, 2}, {0, 0, -2}
        };
        for (int[] offset : offsets) {
            RoomTemplate.BlockDef candidate = blockIndex.get(key(x + offset[0], y + offset[1], z + offset[2]));
            if (candidate == null) {
                continue;
            }
            Material material = candidate.data.getMaterial();
            if (material == Material.REDSTONE_BLOCK || material == Material.PINK_WOOL || material == Material.LIME_WOOL) {
                continue;
            }
            if (material == Material.AIR || STRONGHOLD_IGNORED_MATERIALS.contains(material)) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    private String key(int x, int y, int z) {
        return x + ":" + y + ":" + z;
    }

    private Location resolveWorldLocation(Location center, RoomTemplate template, int rotation, int relativeX, int relativeY, int relativeZ) {
        int[] vec = RoomTemplate.rotate(
                relativeX - (int) Math.round(template.getCenterX()),
                relativeZ - (int) Math.round(template.getCenterZ()),
                rotation);
        int y = center.getBlockY() + (relativeY - template.getConnectorMinY());
        return new Location(center.getWorld(), center.getBlockX() + vec[0], y, center.getBlockZ() + vec[1]);
    }

    private void rememberOriginalBlock(Location worldLoc, Map<Location, org.bukkit.block.data.BlockData> restoreSnapshot) {
        if (worldLoc == null || worldLoc.getWorld() == null) {
            return;
        }
        Location key = worldLoc.getBlock().getLocation();
        restoreSnapshot.computeIfAbsent(key, ignored -> key.getBlock().getBlockData().clone());
    }

    private void restoreSnapshot(Map<Location, org.bukkit.block.data.BlockData> snapshot) {
        for (Map.Entry<Location, org.bukkit.block.data.BlockData> entry : snapshot.entrySet()) {
            Location location = entry.getKey();
            if (location.getWorld() == null) {
                continue;
            }
            location.getBlock().setBlockData(entry.getValue(), false);
        }
    }

    private boolean shouldSkipForStrongholdPaste(Material material) {
        if (material == Material.REDSTONE_BLOCK || material == Material.PINK_WOOL || material == Material.LIME_WOOL) {
            return true;
        }
        return STRONGHOLD_IGNORED_MATERIALS.contains(material);
    }

    private Location resolveAlignedCenter(GridPoint point, RoomTemplate template, int rotation,
                                          Map<GridPoint, Set<Direction>> graph, Map<GridPoint, PlacedPiece> placed) {
        Location chosen = null;
        for (Direction direction : Direction.values()) {
            GridPoint neighborPoint = point.move(direction);
            PlacedPiece neighbor = placed.get(neighborPoint);
            if (neighbor == null) {
                continue;
            }
            if (!graph.get(point).contains(direction)) {
                continue;
            }
            if (!graph.getOrDefault(neighborPoint, Set.of()).contains(direction.opposite())) {
                continue;
            }

            Location candidateCenter = resolveCenterWithBridge(template, rotation, direction, neighbor);
            if (candidateCenter == null) {
                continue;
            }
            if (chosen == null) {
                chosen = candidateCenter;
                continue;
            }
            if (!sameBlock(chosen, candidateCenter)) {
                return null;
            }
        }
        return chosen;
    }

    private Location resolveCenterWithBridge(RoomTemplate currentTemplate, int currentRotation, Direction directionToNeighbor,
                                             PlacedPiece neighbor) {
        Direction neighborToCurrent = directionToNeighbor.opposite();
        RoomTemplate connectorTemplate = selectConnectorTemplate(neighborToCurrent);
        if (connectorTemplate == null) {
            return null;
        }
        Set<Direction> connectorDirs = Set.of(neighborToCurrent, directionToNeighbor);
        int connectorRotation = dungeonManager.findRotation(connectorTemplate, connectorDirs);

        RoomTemplate.Connector neighborConnector = findConnector(neighbor.template, neighbor.rotation, neighborToCurrent);
        RoomTemplate.Connector connectorTowardNeighbor = findConnector(connectorTemplate, connectorRotation, directionToNeighbor);
        RoomTemplate.Connector connectorTowardCurrent = findConnector(connectorTemplate, connectorRotation, neighborToCurrent);
        RoomTemplate.Connector currentConnector = findConnector(currentTemplate, currentRotation, directionToNeighbor);
        if (neighborConnector == null || connectorTowardNeighbor == null
                || connectorTowardCurrent == null || currentConnector == null) {
            return null;
        }

        Location neighborConnectorLoc = connectorWorldLocation(
                neighbor.center, neighbor.template, neighbor.rotation, neighborConnector);
        Location connectorCenter = centerFromConnector(
                neighborConnectorLoc, connectorTemplate, connectorRotation, connectorTowardNeighbor);
        Location connectorTowardCurrentLoc = connectorWorldLocation(
                connectorCenter, connectorTemplate, connectorRotation, connectorTowardCurrent);
        return centerFromConnector(connectorTowardCurrentLoc, currentTemplate, currentRotation, currentConnector);
    }

    private RoomTemplate.Connector findConnector(RoomTemplate template, int rotation, Direction worldFacing) {
        for (RoomTemplate.Connector connector : template.getConnectors()) {
            Direction rotated = rotate(connector.facing, rotation);
            if (rotated == worldFacing) {
                return connector;
            }
        }
        return null;
    }

    private Location connectorWorldLocation(Location center, RoomTemplate template, int rotation, RoomTemplate.Connector connector) {
        int[] vec = RoomTemplate.rotate(
                connector.x - (int) Math.round(template.getCenterX()),
                connector.z - (int) Math.round(template.getCenterZ()),
                rotation);
        int y = center.getBlockY() + (connector.bottomY - template.getConnectorMinY());
        return new Location(center.getWorld(), center.getBlockX() + vec[0], y, center.getBlockZ() + vec[1]);
    }

    private Location centerFromConnector(Location connectorLoc, RoomTemplate template, int rotation, RoomTemplate.Connector connector) {
        int[] vec = RoomTemplate.rotate(
                connector.x - (int) Math.round(template.getCenterX()),
                connector.z - (int) Math.round(template.getCenterZ()),
                rotation);
        int y = connectorLoc.getBlockY() - (connector.bottomY - template.getConnectorMinY());
        return new Location(connectorLoc.getWorld(), connectorLoc.getBlockX() - vec[0], y, connectorLoc.getBlockZ() - vec[1]);
    }

    private boolean sameBlock(Location a, Location b) {
        if (a == null || b == null) return false;
        if (a.getWorld() != b.getWorld()) return false;
        return a.getBlockX() == b.getBlockX() && a.getBlockY() == b.getBlockY() && a.getBlockZ() == b.getBlockZ();
    }

    private Direction rotate(Direction direction, int rotation) {
        int ord = (direction.ordinal() + rotation) & 3;
        return Direction.values()[ord];
    }

    public record SpawnResult(boolean success, String message, int piecesPlaced, int step) {
        public static SpawnResult success(int piecesPlaced, int step) {
            return new SpawnResult(true, null, piecesPlaced, step);
        }

        public static SpawnResult error(String message) {
            return new SpawnResult(false, message, 0, 0);
        }
    }

    private Map<GridPoint, Set<Direction>> generateSnakeGraph(int pieces, UUID seedSource) {
        Map<GridPoint, Set<Direction>> graph = new LinkedHashMap<>();
        Set<GridPoint> occupied = new HashSet<>();
        List<GridPoint> path = new ArrayList<>();
        Random random = new Random(seedSource.getMostSignificantBits() ^ System.nanoTime());

        GridPoint current = new GridPoint(0, 0);
        occupied.add(current);
        path.add(current);

        while (path.size() < pieces) {
            Direction nextDirection = chooseNextDirection(path, occupied, random);
            if (nextDirection == null) {
                break;
            }
            current = current.move(nextDirection);
            occupied.add(current);
            path.add(current);
        }

        if (path.isEmpty()) {
            return graph;
        }

        for (int i = 0; i < path.size(); i++) {
            GridPoint point = path.get(i);
            EnumSet<Direction> dirs = EnumSet.noneOf(Direction.class);
            if (i > 0) {
                GridPoint previous = path.get(i - 1);
                dirs.add(point.directionTo(previous));
            }
            if (i + 1 < path.size()) {
                GridPoint next = path.get(i + 1);
                dirs.add(point.directionTo(next));
            }
            if (dirs.isEmpty()) {
                dirs.add(Direction.NORTH);
            }
            graph.put(point, dirs);
        }

        addOptionalDeadEndCuts(graph, occupied, random, pieces);
        return graph;
    }

    private Direction chooseNextDirection(List<GridPoint> path, Set<GridPoint> occupied, Random random) {
        GridPoint current = path.get(path.size() - 1);
        Direction previousDir = null;
        if (path.size() > 1) {
            previousDir = path.get(path.size() - 1).directionTo(path.get(path.size() - 2)).opposite();
        }

        List<Direction> options = new ArrayList<>(List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST));
        Collections.shuffle(options, random);
        if (previousDir != null && random.nextDouble() < 0.62) {
            options.remove(previousDir);
            options.add(0, previousDir);
        }

        for (Direction dir : options) {
            GridPoint next = current.move(dir);
            if (!occupied.contains(next)) {
                return dir;
            }
        }
        return null;
    }

    private void addOptionalDeadEndCuts(Map<GridPoint, Set<Direction>> graph, Set<GridPoint> occupied,
                                        Random random, int targetPieces) {
        if (graph.size() >= targetPieces) {
            return;
        }
        List<GridPoint> keys = new ArrayList<>(graph.keySet());
        Collections.shuffle(keys, random);
        int attempts = Math.min(keys.size(), Math.max(1, targetPieces / 3));
        for (int i = 0; i < attempts && graph.size() < targetPieces; i++) {
            GridPoint base = keys.get(i);
            if (graph.get(base).size() >= 2) {
                continue;
            }
            List<Direction> freeDirs = new ArrayList<>();
            for (Direction direction : Direction.values()) {
                if (graph.get(base).contains(direction)) {
                    continue;
                }
                GridPoint candidate = base.move(direction);
                if (!occupied.contains(candidate)) {
                    freeDirs.add(direction);
                }
            }
            if (freeDirs.isEmpty()) {
                continue;
            }
            Direction selected = freeDirs.get(random.nextInt(freeDirs.size()));
            GridPoint child = base.move(selected);
            occupied.add(child);
            graph.get(base).add(selected);
            graph.put(child, new LinkedHashSet<>(Set.of(selected.opposite())));
        }
    }

    private record GridPoint(int x, int z) {
        private GridPoint move(Direction direction) {
            return switch (direction) {
                case NORTH -> new GridPoint(x, z - 1);
                case EAST -> new GridPoint(x + 1, z);
                case SOUTH -> new GridPoint(x, z + 1);
                case WEST -> new GridPoint(x - 1, z);
            };
        }

        private Direction directionTo(GridPoint other) {
            int dx = other.x - x;
            int dz = other.z - z;
            if (dx > 0) return Direction.EAST;
            if (dx < 0) return Direction.WEST;
            if (dz > 0) return Direction.SOUTH;
            return Direction.NORTH;
        }
    }

    private record PlacedPiece(RoomTemplate template, int rotation, Location center) {}

    private static final class PlacementResult {
        private final boolean success;
        private final String errorMessage;
        private final int piecesPlaced;

        private PlacementResult(boolean success, String errorMessage, int piecesPlaced) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.piecesPlaced = piecesPlaced;
        }

        private static PlacementResult success(int piecesPlaced) {
            return new PlacementResult(true, null, piecesPlaced);
        }

        private static PlacementResult error(String errorMessage) {
            return new PlacementResult(false, errorMessage, 0);
        }
    }
}
