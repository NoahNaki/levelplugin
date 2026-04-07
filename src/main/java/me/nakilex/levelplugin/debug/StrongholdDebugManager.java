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

/**
 * Debug helper that spawns a square stronghold perimeter from flatland templates.
 */
public class StrongholdDebugManager {
    private final Main plugin;
    private final DungeonManager dungeonManager;
    private final Map<UUID, Dungeon> activeStrongholds = new HashMap<>();
    private static final Set<Material> STRONGHOLD_IGNORED_MATERIALS =
            Set.of(Material.WHITE_CONCRETE, Material.LIGHT_BLUE_CONCRETE);

    private final List<RoomTemplate> cornerTemplates = new ArrayList<>();
    private final List<RoomTemplate> straightTemplates = new ArrayList<>();
    private final List<RoomTemplate> deadEndTemplates = new ArrayList<>();

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

        int step = resolveStep();
        Dungeon stronghold = new Dungeon(world, "debug_stronghold_" + player.getUniqueId());
        Map<GridPoint, Set<Direction>> graph = generateSnakeGraph(sideLength, player.getUniqueId());
        if (graph.isEmpty()) {
            return SpawnResult.error("Could not generate a wall layout.");
        }

        int pieces = 0;
        for (Map.Entry<GridPoint, Set<Direction>> entry : graph.entrySet()) {
            GridPoint point = entry.getKey();
            Set<Direction> openSides = entry.getValue();
            RoomTemplate template = selectTemplate(openSides);
            if (template == null) {
                stronghold.delete();
                return SpawnResult.error("No template matched connector pattern " + openSides + ".");
            }

            int rotation = dungeonManager.findRotation(template, openSides);
            Location center = origin.clone().add(point.x * step, 0, point.z * step);
            DungeonManager.PasteResult result = dungeonManager.pasteRoom(
                    stronghold, template, rotation, center, null, false, STRONGHOLD_IGNORED_MATERIALS);
            if (result.instance() == null) {
                stronghold.delete();
                return SpawnResult.error("Failed to paste piece at grid " + point.x + "," + point.z + ".");
            }
            pieces++;
        }

        activeStrongholds.put(player.getUniqueId(), stronghold);
        return SpawnResult.success(pieces, step);
    }

    public boolean despawn(UUID playerId) {
        Dungeon existing = activeStrongholds.remove(playerId);
        if (existing == null) {
            return false;
        }
        existing.delete();
        return true;
    }

    private boolean ensureTemplatesLoaded() {
        if (templatesLoaded) {
            return !cornerTemplates.isEmpty() && !straightTemplates.isEmpty();
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

        return !cornerTemplates.isEmpty() && !straightTemplates.isEmpty();
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
}
