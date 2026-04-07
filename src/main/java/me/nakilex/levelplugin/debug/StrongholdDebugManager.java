package me.nakilex.levelplugin.debug;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.Direction;
import me.nakilex.levelplugin.dungeon.Dungeon;
import me.nakilex.levelplugin.dungeon.DungeonManager;
import me.nakilex.levelplugin.dungeon.RoomTemplate;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Spawns a stronghold-style debug structure from flatland templates and can restore
 * all replaced blocks for iterative map authoring.
 */
public class StrongholdDebugManager {
    private static final Set<Material> IGNORED_TEMPLATE_MATERIALS = Set.of(Material.WHITE_CONCRETE, Material.LIGHT_BLUE_CONCRETE);
    private static final Set<Material> SKIPPED_PASTE_MATERIALS = Set.of(
            Material.REDSTONE_BLOCK, Material.PINK_WOOL, Material.LIME_WOOL,
            Material.WHITE_CONCRETE, Material.LIGHT_BLUE_CONCRETE
    );

    private final Main plugin;
    private final DungeonManager dungeonManager;
    private final Random random = new Random();

    private final Map<UUID, ActiveStronghold> activeByPlayer = new HashMap<>();
    private final Map<UUID, BukkitTask> runningTasks = new HashMap<>();

    private final List<RoomTemplate> cornerTemplates = new ArrayList<>();
    private final List<RoomTemplate> straightTemplates = new ArrayList<>();
    private final List<RoomTemplate> deadEndTemplates = new ArrayList<>();
    private final List<RoomTemplate> connectorTemplates = new ArrayList<>();
    private final List<RoomTemplate> towerTemplates = new ArrayList<>();
    private final List<RoomTemplate> gateTemplates = new ArrayList<>();

    public StrongholdDebugManager(Main plugin, DungeonManager dungeonManager) {
        this.plugin = plugin;
        this.dungeonManager = dungeonManager;
        loadTemplates();
    }

    public boolean handleCommand(Player player, String[] args) {
        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String action = args[0].toLowerCase();
        if (action.equals("despawn")) {
            despawn(player, true);
            return true;
        }

        if (!action.equals("spawn") && !action.equals("spawnstep")) {
            sendUsage(player);
            return true;
        }

        if (args.length < 2) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Usage: /debug stronghold " + action + " <size> [delayTicks]");
            return true;
        }

        int size;
        try {
            size = Math.max(3, Integer.parseInt(args[1]));
        } catch (NumberFormatException ex) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Size must be a number.");
            return true;
        }

        int delayTicks = 8;
        if (action.equals("spawnstep") && args.length >= 3) {
            try {
                delayTicks = Math.max(1, Integer.parseInt(args[2]));
            } catch (NumberFormatException ex) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "delayTicks must be a number.");
                return true;
            }
        }

        cancelTask(player.getUniqueId());
        despawn(player, false);

        GeneratedStronghold generated = generateSnakeGraph(size);
        if (generated.nodes().isEmpty()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Stronghold generation produced no nodes.");
            return true;
        }

        BuildContext ctx = new BuildContext(player.getUniqueId(), player.getLocation().getBlock().getLocation(), generated);

        if (action.equals("spawn")) {
            boolean ok = placeAll(ctx);
            if (!ok) {
                restore(ctx);
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Stronghold placement failed; restored previous blocks.");
                return true;
            }
            activeByPlayer.put(player.getUniqueId(), new ActiveStronghold(player.getWorld(), ctx.restoreSnapshot));
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Spawned stronghold debug layout (size=" + size + ").");
            return true;
        }

        Deque<NodePlacement> queue = new ArrayDeque<>(ctx.generated.nodes().stream()
                .sorted(Comparator.comparingInt(p -> p.node().index()))
                .collect(Collectors.toList()));
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player online = Bukkit.getPlayer(player.getUniqueId());
            if (online == null || !online.isOnline()) {
                cancelTask(player.getUniqueId());
                restore(ctx);
                return;
            }
            NodePlacement next = queue.pollFirst();
            if (next == null) {
                cancelTask(player.getUniqueId());
                activeByPlayer.put(player.getUniqueId(), new ActiveStronghold(online.getWorld(), ctx.restoreSnapshot));
                ChatMessageUtil.send(online, ChatMessageUtil.MessageType.SUCCESS, "Spawned stronghold progressively.");
                return;
            }
            if (!placeNode(ctx, next)) {
                cancelTask(player.getUniqueId());
                restore(ctx);
                ChatMessageUtil.send(online, ChatMessageUtil.MessageType.ERROR, "Stronghold placement failed; restored previous blocks.");
            }
        }, 1L, delayTicks);
        runningTasks.put(player.getUniqueId(), task);
        return true;
    }

    public List<String> tabComplete(String[] args) {
        if (args.length == 1) {
            return List.of("spawn", "spawnstep", "despawn").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("spawn") || args[0].equalsIgnoreCase("spawnstep"))) {
            return List.of("8", "12", "16").stream().filter(s -> s.startsWith(args[1])).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("spawnstep")) {
            return List.of("2", "4", "8", "12").stream().filter(s -> s.startsWith(args[2])).toList();
        }
        return List.of();
    }

    public void despawn(Player player, boolean notify) {
        UUID id = player.getUniqueId();
        cancelTask(id);
        ActiveStronghold active = activeByPlayer.remove(id);
        if (active == null) {
            if (notify) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "No active stronghold to despawn.");
            }
            return;
        }
        restore(active.world(), active.restoreSnapshot());
        if (notify) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Stronghold despawned and world restored.");
        }
    }

    private void cancelTask(UUID id) {
        BukkitTask task = runningTasks.remove(id);
        if (task != null) {
            task.cancel();
        }
    }

    private boolean placeAll(BuildContext ctx) {
        List<NodePlacement> sorted = ctx.generated.nodes().stream()
                .sorted(Comparator.comparingInt(p -> p.node().index()))
                .toList();
        for (NodePlacement placement : sorted) {
            if (!placeNode(ctx, placement)) {
                return false;
            }
        }
        return placeBridges(ctx);
    }

    private boolean placeNode(BuildContext ctx, NodePlacement placement) {
        Node node = placement.node();
        RoomTemplate template = selectTemplate(node, ctx.generated.nodesByPoint());
        if (template == null) {
            return false;
        }
        int rotation = resolveRotation(template, node.requiredOpenSides());
        if (rotation < 0) {
            template = selectTemplate(node.requiredOpenSides());
            if (template == null) {
                return false;
            }
            rotation = resolveRotation(template, node.requiredOpenSides());
            if (rotation < 0) {
                return false;
            }
        }

        Location center = resolveCenter(ctx, placement, template, rotation);
        if (center == null) {
            return false;
        }

        DungeonManager.PasteResult result = dungeonManager.pasteRoom(ctx.dungeon, template, rotation, center,
                null, false, IGNORED_TEMPLATE_MATERIALS);

        if (!result.success()) {
            return false;
        }

        snapshot(center.getWorld(), template, rotation, center, ctx.restoreSnapshot);
        ctx.placedByPoint.put(placement.point(), new PlacedPiece(template, rotation, center, node));

        if (ctx.placedByPoint.size() == ctx.generated.nodes().size()) {
            return placeBridges(ctx);
        }

        return true;
    }

    private boolean placeBridges(BuildContext ctx) {
        for (NodePlacement placement : ctx.generated.nodes()) {
            for (Direction dir : placement.node().requiredOpenSides()) {
                Point neighbor = placement.point().offset(dir);
                if (placement.point().compareTo(neighbor) >= 0) continue;
                if (!ctx.generated.nodesByPoint().containsKey(neighbor)) continue;
                PlacedPiece a = ctx.placedByPoint.get(placement.point());
                PlacedPiece b = ctx.placedByPoint.get(neighbor);
                if (a == null || b == null) continue;
                placeBridgePiece(ctx, a, b, dir);
            }
        }
        return true;
    }

    private void placeBridgePiece(BuildContext ctx, PlacedPiece from, PlacedPiece to, Direction dir) {
        RoomTemplate bridge = connectorTemplates.isEmpty() ? null : connectorTemplates.get(random.nextInt(connectorTemplates.size()));
        if (bridge == null) return;
        int rotation = resolveRotation(bridge, EnumSet.of(dir, dir.opposite()));
        if (rotation < 0) rotation = 0;
        Location center = from.center().clone().add(dir == Direction.EAST ? 35 : dir == Direction.WEST ? -35 : 0, 0,
                dir == Direction.SOUTH ? 35 : dir == Direction.NORTH ? -35 : 0);
        snapshot(center.getWorld(), bridge, rotation, center, ctx.restoreSnapshot);
        dungeonManager.pasteRoom(ctx.dungeon, bridge, rotation, center,
                null, false, IGNORED_TEMPLATE_MATERIALS);
    }

    private RoomTemplate selectTemplate(Node node, Map<Point, NodePlacement> graph) {
        Set<Direction> dirs = node.requiredOpenSides();
        int degree = dirs.size();
        if (degree == 2 && areOpposite(dirs)) {
            if (canPlaceGate(node, graph)) {
                RoomTemplate gate = pick(gateTemplates);
                if (gate != null) {
                    return gate;
                }
            }
            RoomTemplate tower = pick(towerTemplates);
            if (tower != null && canPlaceTower(node, graph)) {
                return tower;
            }
            RoomTemplate straight = pick(straightTemplates);
            if (straight != null) {
                return straight;
            }
            return selectTemplate(dirs);
        }
        if (degree == 2) {
            RoomTemplate tower = pick(towerTemplates);
            if (tower != null && canPlaceTower(node, graph)) {
                return tower;
            }
            RoomTemplate corner = pick(cornerTemplates);
            if (corner != null) {
                return corner;
            }
            return selectTemplate(dirs);
        }
        if (degree == 1) {
            RoomTemplate deadEnd = pick(deadEndTemplates);
            if (deadEnd != null) return deadEnd;
            RoomTemplate tower = pick(towerTemplates);
            if (tower != null && canPlaceTower(node, graph)) return tower;
            return selectTemplate(dirs);
        }
        return selectTemplate(dirs);
    }

    private RoomTemplate selectTemplate(Set<Direction> dirs) {
        if (dirs.size() == 1) return pick(deadEndTemplates);
        if (dirs.size() == 2 && areOpposite(dirs)) return pick(straightTemplates);
        if (dirs.size() == 2) return pick(cornerTemplates);
        return pick(straightTemplates);
    }

    private boolean canPlaceGate(Node node, Map<Point, NodePlacement> graph) {
        if (node.straightWallsSinceGate() < 2 || node.towerCount() <= node.gateCount()) {
            return false;
        }
        for (Direction dir : node.requiredOpenSides()) {
            NodePlacement neighbor = graph.get(node.point().offset(dir));
            if (neighbor == null) continue;
            PieceKind neighborKind = neighbor.node().kind();
            if (neighborKind == PieceKind.GATE || neighborKind == PieceKind.TOWER) {
                return false;
            }
        }
        node.setKind(PieceKind.GATE);
        return true;
    }

    private boolean canPlaceTower(Node node, Map<Point, NodePlacement> graph) {
        for (Direction dir : node.requiredOpenSides()) {
            NodePlacement neighbor = graph.get(node.point().offset(dir));
            if (neighbor == null) continue;
            if (neighbor.node().kind() == PieceKind.GATE) {
                return false;
            }
        }
        node.setKind(PieceKind.TOWER);
        return true;
    }

    private int resolveRotation(RoomTemplate template, Set<Direction> requiredDirs) {
        for (int r = 0; r < 4; r++) {
            if (template.getRotatedDirections(r).equals(requiredDirs)) {
                return r;
            }
        }
        return -1;
    }

    private Location resolveCenter(BuildContext ctx, NodePlacement placement, RoomTemplate template, int rotation) {
        if (placement.node().index() == 0) {
            return ctx.origin;
        }

        for (Direction dir : placement.node().requiredOpenSides()) {
            Point neighborPoint = placement.point().offset(dir);
            PlacedPiece neighbor = ctx.placedByPoint.get(neighborPoint);
            if (neighbor == null) continue;

            RoomTemplate.Connector localConnector = findConnector(template, rotation, dir);
            RoomTemplate.Connector neighborConnector = findConnector(neighbor.template(), neighbor.rotation(), dir.opposite());
            if (localConnector == null || neighborConnector == null) continue;

            int[] localVec = RoomTemplate.rotate(localConnector.x - (int) Math.round(template.getCenterX()),
                    localConnector.z - (int) Math.round(template.getCenterZ()), rotation);
            int[] neighborVec = RoomTemplate.rotate(neighborConnector.x - (int) Math.round(neighbor.template().getCenterX()),
                    neighborConnector.z - (int) Math.round(neighbor.template().getCenterZ()), neighbor.rotation());

            int targetX = neighbor.center().getBlockX() + neighborVec[0] + (dir == Direction.EAST ? -1 : dir == Direction.WEST ? 1 : 0);
            int targetZ = neighbor.center().getBlockZ() + neighborVec[1] + (dir == Direction.SOUTH ? -1 : dir == Direction.NORTH ? 1 : 0);
            int centerX = targetX - localVec[0];
            int centerZ = targetZ - localVec[1];
            return new Location(ctx.origin.getWorld(), centerX, ctx.origin.getBlockY(), centerZ);
        }

        return ctx.origin.clone().add(placement.point().x() * 72, 0, placement.point().z() * 72);
    }

    private RoomTemplate.Connector findConnector(RoomTemplate template, int rotation, Direction expectedFacing) {
        for (RoomTemplate.Connector connector : template.getConnectors()) {
            Direction facing = rotate(connector.facing, rotation);
            if (facing == expectedFacing) {
                return connector;
            }
        }
        return null;
    }

    private Direction rotate(Direction direction, int rotation) {
        int ord = (direction.ordinal() + rotation) & 3;
        return Direction.values()[ord];
    }

    private GeneratedStronghold generateSnakeGraph(int size) {
        List<NodePlacement> nodes = new ArrayList<>();
        Map<Point, NodePlacement> byPoint = new HashMap<>();

        int x = 0;
        int z = 0;
        int run = Math.max(2, (int) Math.ceil(Math.sqrt(size)));
        int direction = 1;

        for (int i = 0; i < size; i++) {
            Point p = new Point(x, z);
            Node node = new Node(i, p);
            NodePlacement placement = new NodePlacement(p, node);
            nodes.add(placement);
            byPoint.put(p, placement);

            if ((i + 1) % run == 0) {
                z++;
                direction *= -1;
            } else {
                x += direction;
            }
        }

        for (NodePlacement placement : nodes) {
            for (Direction dir : Direction.values()) {
                if (byPoint.containsKey(placement.point().offset(dir))) {
                    placement.node().requiredOpenSides().add(dir);
                }
            }
            if (placement.node().requiredOpenSides().contains(Direction.NORTH)
                    && placement.node().requiredOpenSides().contains(Direction.SOUTH)
                    && placement.node().requiredOpenSides().size() == 2) {
                placement.node().setStraightWallsSinceGate(placement.node().straightWallsSinceGate() + 1);
            }
        }

        int gates = 0;
        int towers = 0;
        int straightSinceGate = 3;
        for (NodePlacement placement : nodes) {
            Node n = placement.node();
            n.setGateCount(gates);
            n.setTowerCount(towers);
            n.setStraightWallsSinceGate(straightSinceGate);
            if (n.requiredOpenSides().size() == 2 && areOpposite(n.requiredOpenSides())) {
                straightSinceGate++;
            }
        }

        return new GeneratedStronghold(nodes, byPoint);
    }

    private boolean areOpposite(Set<Direction> dirs) {
        return dirs.size() == 2
                && ((dirs.contains(Direction.NORTH) && dirs.contains(Direction.SOUTH))
                || (dirs.contains(Direction.EAST) && dirs.contains(Direction.WEST)));
    }

    private RoomTemplate pick(List<RoomTemplate> templates) {
        if (templates == null || templates.isEmpty()) {
            return null;
        }
        return templates.get(random.nextInt(templates.size()));
    }

    private void snapshot(World world, RoomTemplate template, int rotation, Location center, Map<Location, BlockData> snapshot) {
        if (world == null) return;
        int baseY = center.getBlockY();
        int connectorY = template.getConnectorMinY();
        for (RoomTemplate.BlockDef block : template.getBlocks()) {
            Material material = block.data.getMaterial();
            if (SKIPPED_PASTE_MATERIALS.contains(material)) {
                continue;
            }
            int[] vec = RoomTemplate.rotate(block.x - (int) Math.round(template.getCenterX()),
                    block.z - (int) Math.round(template.getCenterZ()), rotation);
            Location loc = new Location(world, center.getBlockX() + vec[0], baseY + (block.y - connectorY), center.getBlockZ() + vec[1]);
            snapshot.putIfAbsent(loc, world.getBlockAt(loc).getBlockData());
        }
    }

    private void restore(BuildContext ctx) {
        restore(ctx.origin.getWorld(), ctx.restoreSnapshot);
    }

    private void restore(World world, Map<Location, BlockData> snapshot) {
        if (world == null) return;
        snapshot.forEach((location, data) -> {
            Block block = world.getBlockAt(location);
            block.setBlockData(data, false);
        });
    }

    private void loadTemplates() {
        World world = Bukkit.getWorld("flatland");
        if (world == null) {
            plugin.getLogger().warning("[StrongholdDebug] flatland world not found; stronghold debug disabled.");
            return;
        }

        capture(cornerTemplates, world, 473, -38, -5346, 543, -61, -5276);
        capture(cornerTemplates, world, 544, -38, -5631, 614, -61, -5701);
        capture(cornerTemplates, world, 614, -61, -5630, 544, -38, -5560);

        capture(straightTemplates, world, 402, -38, -5276, 472, -61, -5346);
        capture(straightTemplates, world, 472, -61, -5347, 402, -38, -5417);
        capture(straightTemplates, world, 402, -38, -5418, 472, -61, -5488);
        capture(straightTemplates, world, 472, -61, -5489, 402, -38, -5559);
        capture(straightTemplates, world, 402, -38, -5560, 472, -61, -5630);
        capture(straightTemplates, world, 472, -61, -5631, 402, -38, -5701);
        capture(straightTemplates, world, 473, -38, -5701, 543, -61, -5631);
        capture(straightTemplates, world, 543, -61, -5630, 473, -38, -5560);
        capture(straightTemplates, world, 473, -38, -5417, 543, -61, -5347);

        capture(deadEndTemplates, world, 543, -38, -5418, 473, -61, -5488);
        capture(deadEndTemplates, world, 473, -61, -5489, 543, -38, -5559);

        capture(connectorTemplates, world, 412, -61, -5711, 402, -38, -5701);
        capture(connectorTemplates, world, 402, -38, -5721, 412, -61, -5711);

        capture(towerTemplates, world, 615, -61, -5488, 685, -7, -5418);

        capture(gateTemplates, world, 686, -61, -5346, 614, -10, -5418);
        capture(gateTemplates, world, 686, -61, -5276, 614, -10, -5346);
    }

    private void capture(List<RoomTemplate> target, World world,
                         int x1, int y1, int z1,
                         int x2, int y2, int z2) {
        RoomTemplate template = RoomTemplate.capture(world, x1, y1, z1, x2, y2, z2, false);
        if (template != null && !template.getBlocks().isEmpty()) {
            target.add(template);
        }
    }

    private void sendUsage(Player player) {
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Stronghold debug: /debug stronghold <spawn|spawnstep|despawn> <size> [delayTicks]");
    }

    private record ActiveStronghold(World world, Map<Location, BlockData> restoreSnapshot) {}

    private record GeneratedStronghold(List<NodePlacement> nodes, Map<Point, NodePlacement> nodesByPoint) {}

    private record NodePlacement(Point point, Node node) {}

    private record Point(int x, int z) implements Comparable<Point> {
        Point offset(Direction direction) {
            return switch (direction) {
                case NORTH -> new Point(x, z - 1);
                case SOUTH -> new Point(x, z + 1);
                case EAST -> new Point(x + 1, z);
                case WEST -> new Point(x - 1, z);
            };
        }

        @Override
        public int compareTo(Point other) {
            int zCmp = Integer.compare(this.z, other.z);
            if (zCmp != 0) return zCmp;
            return Integer.compare(this.x, other.x);
        }
    }

    private static class Node {
        private final int index;
        private final Point point;
        private final Set<Direction> requiredOpenSides = EnumSet.noneOf(Direction.class);
        private PieceKind kind = PieceKind.GENERIC;
        private int gateCount;
        private int towerCount;
        private int straightWallsSinceGate;

        private Node(int index, Point point) {
            this.index = index;
            this.point = point;
        }

        int index() { return index; }
        Point point() { return point; }
        Set<Direction> requiredOpenSides() { return requiredOpenSides; }
        PieceKind kind() { return kind; }
        void setKind(PieceKind kind) { this.kind = kind; }
        int gateCount() { return gateCount; }
        void setGateCount(int gateCount) { this.gateCount = gateCount; }
        int towerCount() { return towerCount; }
        void setTowerCount(int towerCount) { this.towerCount = towerCount; }
        int straightWallsSinceGate() { return straightWallsSinceGate; }
        void setStraightWallsSinceGate(int straightWallsSinceGate) { this.straightWallsSinceGate = straightWallsSinceGate; }
    }

    private enum PieceKind {
        GENERIC,
        GATE,
        TOWER
    }

    private record PlacedPiece(RoomTemplate template, int rotation, Location center, Node node) {}

    private static class BuildContext {
        private final UUID playerId;
        private final Location origin;
        private final GeneratedStronghold generated;
        private final Dungeon dungeon;
        private final Map<Location, BlockData> restoreSnapshot = new HashMap<>();
        private final Map<Point, PlacedPiece> placedByPoint = new HashMap<>();

        private BuildContext(UUID playerId, Location origin, GeneratedStronghold generated) {
            this.playerId = playerId;
            this.origin = origin;
            this.generated = generated;
            this.dungeon = new Dungeon(origin.getWorld(), "stronghold_debug_" + playerId);
        }
    }
}
