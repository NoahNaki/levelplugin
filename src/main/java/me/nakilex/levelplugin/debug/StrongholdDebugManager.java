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
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StrongholdDebugManager {
    private static final Set<Material> TEMPLATE_IGNORE = EnumSet.of(Material.WHITE_CONCRETE, Material.LIGHT_BLUE_CONCRETE);
    private static final Set<Material> STRONGHOLD_SKIP = EnumSet.of(Material.REDSTONE_BLOCK, Material.PINK_WOOL, Material.LIME_WOOL);

    private final Main plugin;
    private final DungeonManager dungeonManager;
    private final Random random = new Random();

    private final List<RoomTemplate> cornerTemplates = new ArrayList<>();
    private final List<RoomTemplate> straightTemplates = new ArrayList<>();
    private final List<RoomTemplate> deadEndTemplates = new ArrayList<>();
    private final List<RoomTemplate> connectorTemplates = new ArrayList<>();
    private final List<RoomTemplate> towerTemplates = new ArrayList<>();
    private final List<RoomTemplate> gateTemplates = new ArrayList<>();

    private final Map<UUID, ActiveStronghold> activeByPlayer = new ConcurrentHashMap<>();
    private volatile boolean templatesLoaded = false;

    public StrongholdDebugManager(Main plugin, DungeonManager dungeonManager) {
        this.plugin = plugin;
        this.dungeonManager = dungeonManager;
    }

    public void spawn(Player player, int size) {
        spawnInternal(player, size, -1);
    }

    public void spawnStep(Player player, int size, long delayTicks) {
        spawnInternal(player, size, Math.max(1L, delayTicks));
    }

    public void despawn(Player player) {
        ActiveStronghold active = activeByPlayer.remove(player.getUniqueId());
        if (active == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "No active stronghold to despawn.");
            return;
        }
        if (active.task != null) {
            active.task.cancel();
        }
        restoreSnapshot(active.restoreSnapshot);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Stronghold debug instance despawned and world restored.");
    }

    private void spawnInternal(Player player, int size, long stepDelayTicks) {
        if (size < 2) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Size must be at least 2.");
            return;
        }
        if (!ensureTemplatesLoaded(player)) {
            return;
        }

        ActiveStronghold previous = activeByPlayer.remove(player.getUniqueId());
        if (previous != null) {
            if (previous.task != null) previous.task.cancel();
            restoreSnapshot(previous.restoreSnapshot);
        }

        List<Node> graph = generateSnakeGraph(size);
        if (graph.isEmpty()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Failed to generate stronghold graph.");
            return;
        }

        List<NodePlan> plans = new ArrayList<>();
        Map<Integer, NodePlan> planById = new HashMap<>();
        Map<Location, BlockData> snapshot = new HashMap<>();
        Dungeon debugDungeon = new Dungeon(player.getWorld(), "stronghold-debug-" + player.getUniqueId());

        Location rootCenter = player.getLocation().getBlock().getLocation();
        int straightWallsSinceGate = 0;
        int towerCount = 0;
        int gateCount = 0;

        for (int i = 0; i < graph.size(); i++) {
            Node node = graph.get(i);
            EnumSet<Direction> dirs = EnumSet.copyOf(node.dirs);
            RoomTemplate template = selectTemplate(dirs, straightWallsSinceGate, towerCount, gateCount, planById, node, graph);
            if (template == null) {
                rollbackAndFail(player, snapshot, "No template matched connector pattern " + dirs + ".");
                return;
            }
            int rotation = findRotation(template, dirs);
            Location center = i == 0 ? rootCenter.clone() : solveCenter(node, template, rotation, planById, rootCenter);
            if (center == null) {
                rollbackAndFail(player, snapshot, "Failed to align template connectors for node " + node.id + ".");
                return;
            }

            captureForRestore(snapshot, template, rotation, center);
            DungeonManager.PasteResult result = dungeonManager.pasteRoom(debugDungeon, template, rotation, center, null, false, TEMPLATE_IGNORE);
            if (!result.success()) {
                rollbackAndFail(player, snapshot, "Failed to paste stronghold node " + node.id + ".");
                return;
            }

            NodePlan plan = new NodePlan(node.id, node, template, rotation, center);
            plans.add(plan);
            planById.put(node.id, plan);

            Set<Direction> opposite = EnumSet.of(Direction.NORTH, Direction.SOUTH);
            if (!dirs.equals(opposite)) opposite = EnumSet.of(Direction.EAST, Direction.WEST);
            boolean isOpposite = dirs.equals(EnumSet.of(Direction.NORTH, Direction.SOUTH)) || dirs.equals(EnumSet.of(Direction.EAST, Direction.WEST));
            if (gateTemplates.contains(template)) {
                gateCount++;
                straightWallsSinceGate = 0;
            } else if (towerTemplates.contains(template)) {
                towerCount++;
                straightWallsSinceGate++;
            } else if (isOpposite) {
                straightWallsSinceGate++;
            }
        }

        List<ConnectorPlan> connectorPlans = buildConnectorPlans(plans, snapshot, debugDungeon);

        ActiveStronghold active = new ActiveStronghold(player.getWorld(), snapshot, plans, connectorPlans, debugDungeon, null);
        if (stepDelayTicks > 0) {
            restoreSnapshot(snapshot);
            BukkitTask task = runStepPlacement(player, plans, connectorPlans, snapshot, debugDungeon, stepDelayTicks);
            activeByPlayer.put(player.getUniqueId(), new ActiveStronghold(player.getWorld(), snapshot, plans, connectorPlans, debugDungeon, task));
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Stronghold step spawn started (" + plans.size() + " rooms).");
            return;
        }

        activeByPlayer.put(player.getUniqueId(), active);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Stronghold spawned with " + plans.size() + " rooms.");
    }

    private BukkitTask runStepPlacement(Player player, List<NodePlan> plans, List<ConnectorPlan> connectorPlans, Map<Location, BlockData> snapshot, Dungeon dungeon, long delayTicks) {
        List<PlacementPlan> placements = new ArrayList<>(plans.size() + connectorPlans.size());
        for (NodePlan p : plans) placements.add(new PlacementPlan(p.template, p.rotation, p.center));
        for (ConnectorPlan p : connectorPlans) placements.add(new PlacementPlan(p.template, p.rotation, p.center));
        final int[] idx = {0};
        return Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                ActiveStronghold active = activeByPlayer.remove(player.getUniqueId());
                if (active != null && active.task != null) active.task.cancel();
                restoreSnapshot(snapshot);
                return;
            }
            if (idx[0] >= placements.size()) {
                ActiveStronghold active = activeByPlayer.get(player.getUniqueId());
                if (active != null && active.task != null) {
                    active.task.cancel();
                    activeByPlayer.put(player.getUniqueId(), new ActiveStronghold(active.world, active.restoreSnapshot, active.placed, active.connectors, active.dungeon, null));
                }
                return;
            }
            PlacementPlan p = placements.get(idx[0]++);
            dungeonManager.pasteRoom(dungeon, p.template, p.rotation, p.center, null, false, TEMPLATE_IGNORE);
        }, 1L, delayTicks);
    }

    private void rollbackAndFail(Player player, Map<Location, BlockData> snapshot, String reason) {
        restoreSnapshot(snapshot);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, reason);
    }

    private void captureForRestore(Map<Location, BlockData> snapshot, RoomTemplate template, int rotation, Location center) {
        World world = center.getWorld();
        if (world == null) return;
        for (RoomTemplate.BlockDef b : template.getBlocks()) {
            Material mat = b.data.getMaterial();
            if (TEMPLATE_IGNORE.contains(mat) || STRONGHOLD_SKIP.contains(mat)) continue;
            Location loc = blockLocationFor(template, b.x, b.y, b.z, rotation, center);
            snapshot.putIfAbsent(loc, world.getBlockAt(loc).getBlockData());
        }
        for (RoomTemplate.Marker m : template.getPortals()) {
            Location loc = blockLocationFor(template, m.x, m.y, m.z, rotation, center);
            snapshot.putIfAbsent(loc, world.getBlockAt(loc).getBlockData());
        }
        if (template.getBossSpawn() != null) {
            RoomTemplate.Marker m = template.getBossSpawn();
            Location loc = blockLocationFor(template, m.x, m.y, m.z, rotation, center);
            snapshot.putIfAbsent(loc, world.getBlockAt(loc).getBlockData());
        }
    }

    private Location blockLocationFor(RoomTemplate template, int x, int y, int z, int rotation, Location center) {
        int[] vec = RoomTemplate.rotate(x - (int) Math.round(template.getCenterX()),
                z - (int) Math.round(template.getCenterZ()), rotation);
        int wx = center.getBlockX() + vec[0];
        int wy = center.getBlockY() + (y - template.getConnectorMinY());
        int wz = center.getBlockZ() + vec[1];
        return new Location(center.getWorld(), wx, wy, wz);
    }

    private List<ConnectorPlan> buildConnectorPlans(List<NodePlan> plans, Map<Location, BlockData> snapshot, Dungeon dungeon) {
        List<ConnectorPlan> connectorPlans = new ArrayList<>();
        Map<Integer, NodePlan> byId = new HashMap<>();
        for (NodePlan p : plans) byId.put(p.id, p);

        for (NodePlan p : plans) {
            for (Direction d : p.node.dirs) {
                Integer nid = p.node.neighbors.get(d);
                if (nid == null || p.id > nid) continue;
                NodePlan neighbor = byId.get(nid);
                if (neighbor == null) continue;

                ConnectorPlan connectorPlan = buildConnectorPlan(p, neighbor, d);
                if (connectorPlan != null) {
                    captureForRestore(snapshot, connectorPlan.template, connectorPlan.rotation, connectorPlan.center);
                    dungeonManager.pasteRoom(dungeon, connectorPlan.template, connectorPlan.rotation, connectorPlan.center, null, false, TEMPLATE_IGNORE);
                    connectorPlans.add(connectorPlan);
                    continue;
                }

                Location a = connectorWorldLocation(p, d);
                Location b = connectorWorldLocation(neighbor, d.opposite());
                if (a == null || b == null || a.getWorld() == null) continue;
                World world = a.getWorld();
                int dx = Integer.compare(b.getBlockX(), a.getBlockX());
                int dz = Integer.compare(b.getBlockZ(), a.getBlockZ());
                int x = a.getBlockX();
                int y = a.getBlockY();
                int z = a.getBlockZ();
                while (x != b.getBlockX() || z != b.getBlockZ()) {
                    Location loc = new Location(world, x, y, z);
                    snapshot.putIfAbsent(loc, world.getBlockAt(loc).getBlockData());
                    world.getBlockAt(loc).setType(Material.STONE_BRICKS, false);
                    if (x != b.getBlockX()) x += dx;
                    if (z != b.getBlockZ()) z += dz;
                }
            }
        }

        return connectorPlans;
    }

    private ConnectorPlan buildConnectorPlan(NodePlan a, NodePlan b, Direction directionFromA) {
        Location aTarget = connectorAnchorLocation(a, directionFromA, true);
        Location bTarget = connectorAnchorLocation(b, directionFromA.opposite(), true);
        if (aTarget == null || bTarget == null) return null;

        for (RoomTemplate connectorTemplate : connectorTemplates) {
            int rotation = findRotation(connectorTemplate, EnumSet.of(directionFromA, directionFromA.opposite()));
            if (rotation < 0) continue;

            RoomTemplate.Connector enter = findConnector(connectorTemplate, rotation, directionFromA);
            RoomTemplate.Connector exit = findConnector(connectorTemplate, rotation, directionFromA.opposite());
            if (enter == null || exit == null) continue;

            Location center = centerFromAnchor(connectorTemplate, enter, rotation, aTarget, true, a.center);
            if (center == null) continue;
            Location resolvedExit = connectorAnchorLocation(connectorTemplate, exit, rotation, center, true);
            if (resolvedExit != null
                    && resolvedExit.getBlockX() == bTarget.getBlockX()
                    && resolvedExit.getBlockY() == bTarget.getBlockY()
                    && resolvedExit.getBlockZ() == bTarget.getBlockZ()) {
                return new ConnectorPlan(connectorTemplate, rotation, center);
            }
        }
        return null;
    }

    private Location connectorWorldLocation(NodePlan plan, Direction direction) {
        for (RoomTemplate.Connector c : plan.template.getConnectors()) {
            Direction facing = rotateDirection(c.facing, plan.rotation);
            if (facing != direction) continue;
            return blockLocationFor(plan.template, c.x, c.bottomY, c.z, plan.rotation, plan.center);
        }
        return null;
    }

    private Location connectorAnchorLocation(NodePlan plan, Direction direction, boolean ignoreAlignmentMarkers) {
        for (RoomTemplate.Connector c : plan.template.getConnectors()) {
            Direction facing = rotateDirection(c.facing, plan.rotation);
            if (facing != direction) continue;
            return connectorAnchorLocation(plan.template, c, plan.rotation, plan.center, ignoreAlignmentMarkers);
        }
        return null;
    }

    private Location connectorAnchorLocation(RoomTemplate template, RoomTemplate.Connector connector, int rotation, Location center, boolean ignoreAlignmentMarkers) {
        Location marker = blockLocationFor(template, connector.x, connector.bottomY, connector.z, rotation, center);
        if (!ignoreAlignmentMarkers) return marker;
        int[] inward = directionVector(rotateDirection(connector.facing, rotation).opposite());
        return marker.add(inward[0], 0, inward[1]);
    }

    private Location centerFromAnchor(RoomTemplate template, RoomTemplate.Connector connector, int rotation, Location anchor, boolean ignoreAlignmentMarkers, Location fallback) {
        if (anchor == null || fallback == null || fallback.getWorld() == null) return null;
        int[] vec = RoomTemplate.rotate(connector.x - (int) Math.round(template.getCenterX()),
                connector.z - (int) Math.round(template.getCenterZ()), rotation);
        int shiftX = 0;
        int shiftZ = 0;
        if (ignoreAlignmentMarkers) {
            int[] inward = directionVector(rotateDirection(connector.facing, rotation).opposite());
            shiftX = inward[0];
            shiftZ = inward[1];
        }
        int cx = anchor.getBlockX() - vec[0] - shiftX;
        int cy = anchor.getBlockY() - (connector.bottomY - template.getConnectorMinY());
        int cz = anchor.getBlockZ() - vec[1] - shiftZ;
        return new Location(fallback.getWorld(), cx, cy, cz);
    }

    private int[] directionVector(Direction direction) {
        return switch (direction) {
            case NORTH -> new int[]{0, -1};
            case SOUTH -> new int[]{0, 1};
            case EAST -> new int[]{1, 0};
            case WEST -> new int[]{-1, 0};
        };
    }

    private Location solveCenter(Node node, RoomTemplate template, int rotation, Map<Integer, NodePlan> placed, Location fallback) {
        for (Map.Entry<Direction, Integer> edge : node.neighbors.entrySet()) {
            NodePlan neighbor = placed.get(edge.getValue());
            if (neighbor == null) continue;
            Direction dirToNeighbor = edge.getKey();
            RoomTemplate.Connector thisConn = findConnector(template, rotation, dirToNeighbor);
            RoomTemplate.Connector otherConn = findConnector(neighbor.template, neighbor.rotation, dirToNeighbor.opposite());
            if (thisConn == null || otherConn == null) continue;
            Location target = connectorAnchorLocation(neighbor.template, otherConn, neighbor.rotation, neighbor.center, true);
            Location center = centerFromAnchor(template, thisConn, rotation, target, true, fallback);
            if (center != null) return center;
        }
        return fallback.clone();
    }

    private RoomTemplate.Connector findConnector(RoomTemplate t, int rotation, Direction want) {
        for (RoomTemplate.Connector c : t.getConnectors()) {
            if (rotateDirection(c.facing, rotation) == want) return c;
        }
        return null;
    }

    private RoomTemplate selectTemplate(EnumSet<Direction> dirs,
                                        int straightWallsSinceGate,
                                        int towerCount,
                                        int gateCount,
                                        Map<Integer, NodePlan> placed,
                                        Node node,
                                        List<Node> graph) {
        int degree = dirs.size();
        boolean opposite = dirs.equals(EnumSet.of(Direction.NORTH, Direction.SOUTH))
                || dirs.equals(EnumSet.of(Direction.EAST, Direction.WEST));
        if (degree == 2 && opposite) {
            if (straightWallsSinceGate >= 2 && towerCount > gateCount && canPlaceGate(node, placed, graph)) {
                RoomTemplate gate = pickRandom(gateTemplates);
                if (gate != null && findRotation(gate, dirs) >= 0) return gate;
            }
            RoomTemplate tower = pickRandom(towerTemplates);
            if (tower != null && canPlaceTower(node, placed, graph) && findRotation(tower, dirs) >= 0) return tower;
            return selectTemplate(dirs);
        }
        if (degree == 2) {
            RoomTemplate tower = pickRandom(towerTemplates);
            if (tower != null && canPlaceTower(node, placed, graph) && findRotation(tower, dirs) >= 0) return tower;
            return pickRandom(cornerTemplates) != null ? pickRandom(cornerTemplates) : selectTemplate(dirs);
        }
        if (degree == 1) {
            RoomTemplate tower = pickRandom(towerTemplates);
            if (tower != null && canPlaceTower(node, placed, graph) && findRotation(tower, dirs) >= 0) return tower;
            RoomTemplate dead = pickRandom(deadEndTemplates);
            if (dead != null && findRotation(dead, dirs) >= 0) return dead;
            return selectTemplate(dirs);
        }
        return selectTemplate(dirs);
    }

    private boolean canPlaceGate(Node node, Map<Integer, NodePlan> placed, List<Node> graph) {
        for (Integer nid : node.neighbors.values()) {
            NodePlan p = placed.get(nid);
            if (p == null) continue;
            if (gateTemplates.contains(p.template) || towerTemplates.contains(p.template)) return false;
        }
        return true;
    }

    private boolean canPlaceTower(Node node, Map<Integer, NodePlan> placed, List<Node> graph) {
        for (Integer nid : node.neighbors.values()) {
            NodePlan p = placed.get(nid);
            if (p != null && gateTemplates.contains(p.template)) return false;
        }
        return true;
    }

    private RoomTemplate selectTemplate(EnumSet<Direction> dirs) {
        int degree = dirs.size();
        if (degree == 1) {
            RoomTemplate dead = pickRandom(deadEndTemplates);
            if (dead != null && findRotation(dead, dirs) >= 0) return dead;
        }
        if (degree == 2) {
            boolean opposite = dirs.equals(EnumSet.of(Direction.NORTH, Direction.SOUTH))
                    || dirs.equals(EnumSet.of(Direction.EAST, Direction.WEST));
            RoomTemplate candidate = opposite ? pickRandom(straightTemplates) : pickRandom(cornerTemplates);
            if (candidate != null && findRotation(candidate, dirs) >= 0) return candidate;
        }
        RoomTemplate fallback = pickRandom(straightTemplates);
        if (fallback != null && findRotation(fallback, dirs) >= 0) return fallback;
        for (RoomTemplate t : allTemplates()) {
            if (findRotation(t, dirs) >= 0) return t;
        }
        return null;
    }

    private List<RoomTemplate> allTemplates() {
        List<RoomTemplate> all = new ArrayList<>();
        all.addAll(cornerTemplates);
        all.addAll(straightTemplates);
        all.addAll(deadEndTemplates);
        all.addAll(connectorTemplates);
        all.addAll(towerTemplates);
        all.addAll(gateTemplates);
        return all;
    }

    private int findRotation(RoomTemplate template, Set<Direction> target) {
        for (int r = 0; r < 4; r++) {
            if (template.getRotatedDirections(r).equals(target)) return r;
        }
        return -1;
    }

    private Direction rotateDirection(Direction dir, int rotation) {
        Direction out = dir;
        for (int i = 0; i < (rotation & 3); i++) {
            out = switch (out) {
                case NORTH -> Direction.EAST;
                case EAST -> Direction.SOUTH;
                case SOUTH -> Direction.WEST;
                case WEST -> Direction.NORTH;
            };
        }
        return out;
    }

    private boolean ensureTemplatesLoaded(Player player) {
        if (templatesLoaded) return true;
        World flatland = Bukkit.getWorld("flatland");
        if (flatland == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "World 'flatland' is required for stronghold debug templates.");
            return false;
        }
        cornerTemplates.clear(); straightTemplates.clear(); deadEndTemplates.clear(); connectorTemplates.clear(); towerTemplates.clear(); gateTemplates.clear();

        load(cornerTemplates, flatland, 473, -38, -5346, 543, -61, -5276);
        load(cornerTemplates, flatland, 544, -38, -5631, 614, -61, -5701);
        load(cornerTemplates, flatland, 614, -61, -5630, 544, -38, -5560);

        load(straightTemplates, flatland, 402, -38, -5276, 472, -61, -5346);
        load(straightTemplates, flatland, 472, -61, -5347, 402, -38, -5417);
        load(straightTemplates, flatland, 402, -38, -5418, 472, -61, -5488);
        load(straightTemplates, flatland, 472, -61, -5489, 402, -38, -5559);
        load(straightTemplates, flatland, 402, -38, -5560, 472, -61, -5630);
        load(straightTemplates, flatland, 472, -61, -5631, 402, -38, -5701);
        load(straightTemplates, flatland, 473, -38, -5701, 543, -61, -5631);
        load(straightTemplates, flatland, 543, -61, -5630, 473, -38, -5560);
        load(straightTemplates, flatland, 473, -38, -5417, 543, -61, -5347);

        load(deadEndTemplates, flatland, 543, -38, -5418, 473, -61, -5488);
        load(deadEndTemplates, flatland, 473, -61, -5489, 543, -38, -5559);

        load(connectorTemplates, flatland, 412, -61, -5711, 402, -38, -5701);
        load(connectorTemplates, flatland, 402, -38, -5721, 412, -61, -5711);

        load(towerTemplates, flatland, 615, -61, -5488, 685, -7, -5418);

        load(gateTemplates, flatland, 686, -61, -5346, 614, -10, -5418);
        load(gateTemplates, flatland, 686, -61, -5276, 614, -10, -5346);

        templatesLoaded = !straightTemplates.isEmpty();
        if (!templatesLoaded) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Failed to load stronghold templates from flatland.");
        }
        return templatesLoaded;
    }

    private void load(List<RoomTemplate> target, World world, int x1, int y1, int z1, int x2, int y2, int z2) {
        target.add(RoomTemplate.capture(world, x1, y1, z1, x2, y2, z2, false));
    }

    private void restoreSnapshot(Map<Location, BlockData> snapshot) {
        for (Map.Entry<Location, BlockData> e : snapshot.entrySet()) {
            Location l = e.getKey();
            if (l.getWorld() == null) continue;
            l.getWorld().getBlockAt(l).setBlockData(e.getValue(), false);
        }
    }

    private List<Node> generateSnakeGraph(int size) {
        int width = Math.max(2, (int) Math.ceil(Math.sqrt(size)));
        List<int[]> path = new ArrayList<>();
        int z = 0;
        while (path.size() < size) {
            if ((z & 1) == 0) {
                for (int x = 0; x < width && path.size() < size; x++) path.add(new int[]{x, z});
            } else {
                for (int x = width - 1; x >= 0 && path.size() < size; x--) path.add(new int[]{x, z});
            }
            z++;
        }
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < path.size(); i++) {
            nodes.add(new Node(i, path.get(i)[0], path.get(i)[1]));
        }
        for (int i = 1; i < nodes.size(); i++) {
            Node a = nodes.get(i - 1);
            Node b = nodes.get(i);
            Direction dirAB = Direction.fromDelta(b.gx - a.gx, b.gz - a.gz);
            a.neighbors.put(dirAB, b.id);
            a.dirs.add(dirAB);
            b.neighbors.put(dirAB.opposite(), a.id);
            b.dirs.add(dirAB.opposite());
        }
        return nodes;
    }

    private <T> T pickRandom(List<T> list) {
        if (list == null || list.isEmpty()) return null;
        return list.get(random.nextInt(list.size()));
    }

    private record ActiveStronghold(World world,
                                    Map<Location, BlockData> restoreSnapshot,
                                    List<NodePlan> placed,
                                    List<ConnectorPlan> connectors,
                                    Dungeon dungeon,
                                    BukkitTask task) {}

    private record NodePlan(int id, Node node, RoomTemplate template, int rotation, Location center) {}

    private record ConnectorPlan(RoomTemplate template, int rotation, Location center) {}

    private record PlacementPlan(RoomTemplate template, int rotation, Location center) {}

    private static final class Node {
        private final int id;
        private final int gx;
        private final int gz;
        private final EnumSet<Direction> dirs = EnumSet.noneOf(Direction.class);
        private final EnumMap<Direction, Integer> neighbors = new EnumMap<>(Direction.class);

        private Node(int id, int gx, int gz) {
            this.id = id;
            this.gx = gx;
            this.gz = gz;
        }
    }
}
