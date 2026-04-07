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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Debug helper that spawns a square stronghold perimeter from flatland templates.
 */
public class StrongholdDebugManager {
    private final Main plugin;
    private final DungeonManager dungeonManager;
    private final Map<UUID, Dungeon> activeStrongholds = new HashMap<>();

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
        if (sideLength < 2) {
            return SpawnResult.error("Size must be at least 2.");
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
        int pieces = 0;

        for (int x = 0; x < sideLength; x++) {
            for (int z = 0; z < sideLength; z++) {
                boolean perimeter = x == 0 || z == 0 || x == sideLength - 1 || z == sideLength - 1;
                if (!perimeter) {
                    continue;
                }

                Set<Direction> openSides = openSidesForCell(x, z, sideLength);
                RoomTemplate template = selectTemplate(openSides);
                if (template == null) {
                    return SpawnResult.error("No template matched connector pattern " + openSides + ".");
                }

                int rotation = dungeonManager.findRotation(template, openSides);
                Location center = origin.clone().add(x * step, 0, z * step);
                DungeonManager.PasteResult result = dungeonManager.pasteRoom(stronghold, template, rotation, center);
                if (result.instance() == null) {
                    stronghold.delete();
                    return SpawnResult.error("Failed to paste piece at grid " + x + "," + z + ".");
                }
                pieces++;
            }
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

    private Set<Direction> openSidesForCell(int x, int z, int sideLength) {
        EnumSet<Direction> dirs = EnumSet.noneOf(Direction.class);
        if (x > 0 && (z == 0 || z == sideLength - 1)) {
            dirs.add(Direction.WEST);
        }
        if (x < sideLength - 1 && (z == 0 || z == sideLength - 1)) {
            dirs.add(Direction.EAST);
        }
        if (z > 0 && (x == 0 || x == sideLength - 1)) {
            dirs.add(Direction.NORTH);
        }
        if (z < sideLength - 1 && (x == 0 || x == sideLength - 1)) {
            dirs.add(Direction.SOUTH);
        }
        return dirs;
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
}
