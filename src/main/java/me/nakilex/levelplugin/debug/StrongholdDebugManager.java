package me.nakilex.levelplugin.debug;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.Direction;
import me.nakilex.levelplugin.dungeon.Dungeon;
import me.nakilex.levelplugin.dungeon.DungeonManager;
import me.nakilex.levelplugin.dungeon.RoomTemplate;
import me.nakilex.levelplugin.dungeon.generation.BranchingRandomGraphGenerator;
import me.nakilex.levelplugin.dungeon.generation.DungeonGraphGenerator;
import me.nakilex.levelplugin.dungeon.generation.GridNode;
import me.nakilex.levelplugin.dungeon.generation.SnakeGraphGenerator;
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
    private static final double STRONGHOLD_MAX_OVERLAP = 0.30;
    private static final int NODE_PLACEMENT_ATTEMPTS = 24;

    private final Main plugin;
    private final DungeonManager dungeonManager;
    private final Random random = new Random();

    private final List<RoomTemplate> cornerTemplates = new ArrayList<>();
    private final List<RoomTemplate> straightTemplates = new ArrayList<>();
    private final List<RoomTemplate> deadEndTemplates = new ArrayList<>();
    private final List<RoomTemplate> tSectionTemplates = new ArrayList<>();
    private final List<RoomTemplate> connectorTemplates = new ArrayList<>();
    private final List<RoomTemplate> towerTemplates = new ArrayList<>();
    private final List<RoomTemplate> gateTemplates = new ArrayList<>();

    private final Map<UUID, ActiveStronghold> activeByPlayer = new ConcurrentHashMap<>();
    private volatile boolean templatesLoaded = false;

    public StrongholdDebugManager(Main plugin, DungeonManager dungeonManager) {
        this.plugin = plugin;
        this.dungeonManager = dungeonManager;
    }

    public void spawn(Player player, int size, GraphMode mode) {
        spawnInternal(player, size, -1, mode);
    }

    public void spawnStep(Player player, int size, long delayTicks, GraphMode mode) {
        spawnInternal(player, size, Math.max(1L, delayTicks), mode);
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

    private void spawnInternal(Player player, int size, long stepDelayTicks, GraphMode graphMode) {
        if (size < 2) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Size must be at least 2.");
            return;
        }
        if (!ensureTemplatesLoaded(player)) {
            return;
        }
        if (graphMode == GraphMode.TEST) {
            reportTemplateConnectorCounts(player);
        }

        ActiveStronghold previous = activeByPlayer.remove(player.getUniqueId());
        if (previous != null) {
            if (previous.task != null) previous.task.cancel();
            restoreSnapshot(previous.restoreSnapshot);
        }
        if (graphMode == GraphMode.TSECTION) {
            spawnTSectionVariants(player);
            return;
        }

        List<GridNode> graph = generateGraphForTemplates(graphMode, size);
        if (graph.isEmpty() || graph.size() < size) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Failed to generate stronghold graph for mode '" + graphMode.id() + "'.");
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
            GridNode node = graph.get(i);
            EnumSet<Direction> dirs = node.directions();
            CandidatePlacement candidate = selectTemplateWithOverlapBudget(node, dirs, straightWallsSinceGate, towerCount, gateCount,
                    planById, graphMode, graph, i == 0 ? rootCenter.clone() : rootCenter, debugDungeon, i == 0);
            if (candidate == null) {
                rollbackAndFail(player, snapshot, "No template matched connector pattern " + dirs + ".");
                return;
            }
            RoomTemplate template = candidate.template;
            int rotation = candidate.rotation;
            Location center = candidate.center;

            captureForRestore(snapshot, template, rotation, center);
            DungeonManager.PasteResult result = dungeonManager.pasteRoom(debugDungeon, template, rotation, center, null, false,
                    TEMPLATE_IGNORE, STRONGHOLD_MAX_OVERLAP);
            if (!result.success()) {
                rollbackAndFail(player, snapshot, "Failed to paste stronghold node " + node.id() + ".");
                return;
            }

            NodePlan plan = new NodePlan(node.id(), node, template, rotation, center);
            plans.add(plan);
            planById.put(node.id(), plan);

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
        if (connectorPlans == null) {
            rollbackAndFail(player, snapshot, "Failed to align stronghold connectors without overlap.");
            return;
        }

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

    private void spawnTSectionVariants(Player player) {
        RoomTemplate tSection = pickRandom(tSectionTemplates);
        if (tSection == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "No T-section template is loaded.");
            return;
        }

        Map<Location, BlockData> snapshot = new HashMap<>();
        Dungeon debugDungeon = new Dungeon(player.getWorld(), "stronghold-tsection-debug-" + player.getUniqueId());
        Location origin = player.getLocation().getBlock().getLocation();
        int spacing = Math.max(tSection.getWidth(), tSection.getDepth()) + 12;

        for (int rotation = 0; rotation < 4; rotation++) {
            Location center = origin.clone().add(rotation * spacing, 0, 0);
            captureForRestore(snapshot, tSection, rotation, center);
            DungeonManager.PasteResult result = dungeonManager.pasteRoom(debugDungeon, tSection, rotation, center, null, false,
                    TEMPLATE_IGNORE, STRONGHOLD_MAX_OVERLAP);
            if (!result.success()) {
                restoreSnapshot(snapshot);
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                        "Failed to paste T-section variant rotation " + rotation + ".");
                return;
            }
        }

        activeByPlayer.put(player.getUniqueId(), new ActiveStronghold(player.getWorld(), snapshot,
                Collections.emptyList(), Collections.emptyList(), debugDungeon, null));
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Spawned all 4 T-section rotations in a row for debug.");
    }

    private CandidatePlacement selectTemplateWithOverlapBudget(GridNode node,
                                                               EnumSet<Direction> dirs,
                                                               int straightWallsSinceGate,
                                                               int towerCount,
                                                               int gateCount,
                                                               Map<Integer, NodePlan> placed,
                                                               GraphMode graphMode,
                                                               List<GridNode> graph,
                                                               Location rootCenter,
                                                               Dungeon dungeon,
                                                               boolean firstNode) {
        CandidatePlacement best = null;
        double bestOverlap = Double.MAX_VALUE;
        for (int attempt = 0; attempt < NODE_PLACEMENT_ATTEMPTS; attempt++) {
            RoomTemplate template = selectTemplate(dirs, straightWallsSinceGate, towerCount, gateCount, placed, node, graphMode, graph);
            if (template == null) {
                continue;
            }
            int rotation = findRotationForPlacement(template, dirs);
            if (rotation < 0) {
                continue;
            }
            Location center = firstNode ? rootCenter.clone() : solveCenter(node, template, rotation, placed, rootCenter);
            if (center == null) {
                continue;
            }
            DungeonManager.PasteResult preview = dungeonManager.pasteRoom(dungeon, template, rotation, center, null, true,
                    TEMPLATE_IGNORE, STRONGHOLD_MAX_OVERLAP);
            if (preview.success()) {
                return new CandidatePlacement(template, rotation, center, preview.overlap());
            }
            if (preview.overlap() < bestOverlap) {
                bestOverlap = preview.overlap();
                best = new CandidatePlacement(template, rotation, center, preview.overlap());
            }
        }
        return bestOverlap <= STRONGHOLD_MAX_OVERLAP ? best : null;
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
            for (Direction d : p.node.directions()) {
                Integer nid = p.node.neighbors().get(d);
                if (nid == null || p.id > nid) continue;
                NodePlan neighbor = byId.get(nid);
                if (neighbor == null) continue;

                ConnectorPlan connectorPlan = buildConnectorPlan(p, neighbor, d);
                if (connectorPlan == null) {
                    if (connectorsAlreadyTouching(p, neighbor, d)) {
                        continue;
                    }
                    return null;
                }
                captureForRestore(snapshot, connectorPlan.template, connectorPlan.rotation, connectorPlan.center);
                dungeonManager.pasteRoom(dungeon, connectorPlan.template, connectorPlan.rotation,
                        connectorPlan.center, null, false, TEMPLATE_IGNORE);
                connectorPlans.add(connectorPlan);
            }
        }

        return connectorPlans;
    }

    private boolean connectorsAlreadyTouching(NodePlan a, NodePlan b, Direction directionFromA) {
        Location aTarget = connectorAnchorLocation(a, directionFromA, true);
        Location bTarget = connectorAnchorLocation(b, directionFromA.opposite(), true);
        if (aTarget == null || bTarget == null) {
            return false;
        }
        if (!Objects.equals(aTarget.getWorld(), bTarget.getWorld())) {
            return false;
        }
        int dx = Math.abs(aTarget.getBlockX() - bTarget.getBlockX());
        int dy = Math.abs(aTarget.getBlockY() - bTarget.getBlockY());
        int dz = Math.abs(aTarget.getBlockZ() - bTarget.getBlockZ());
        return dy == 0 && (dx + dz) <= 1;
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

    private Location solveCenter(GridNode node, RoomTemplate template, int rotation, Map<Integer, NodePlan> placed, Location fallback) {
        for (Map.Entry<Direction, Integer> edge : node.neighbors().entrySet()) {
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
        return null;
    }

    private RoomTemplate.Connector findConnector(RoomTemplate t, int rotation, Direction want) {
        for (RoomTemplate.Connector c : t.getConnectors()) {
            if (rotateDirection(c.facing, rotation) == want) return c;
        }
        return null;
    }

    private List<GridNode> generateGraphForTemplates(GraphMode mode, int size) {
        if (mode == GraphMode.TEST) {
            return buildTestGraph();
        }
        List<GridNode> graph = mode.generator.generate(size, random);
        if (isGraphTemplateCompatible(graph)) {
            return graph;
        }
        // Branching can exceed available connector patterns; retry with conservative cap.
        if (mode == GraphMode.BRANCHING) {
            DungeonGraphGenerator fallback = new BranchingRandomGraphGenerator(2);
            for (int attempt = 0; attempt < 6; attempt++) {
                List<GridNode> candidate = fallback.generate(size, random);
                if (isGraphTemplateCompatible(candidate)) {
                    return candidate;
                }
            }
        }
        return graph;
    }

    private List<GridNode> buildTestGraph() {
        GridNode center = new GridNode(0, 0, 0);
        GridNode northArm = new GridNode(1, 0, -1);
        GridNode eastArm = new GridNode(2, 1, 0);
        GridNode southArm = new GridNode(3, 0, 1);
        GridNode westArm = new GridNode(4, -1, 0);
        GridNode northEnd = new GridNode(5, 0, -2);
        GridNode eastEnd = new GridNode(6, 2, 0);
        GridNode southEnd = new GridNode(7, 0, 2);
        GridNode westEnd = new GridNode(8, -2, 0);

        center.link(Direction.NORTH, northArm.id());
        center.link(Direction.EAST, eastArm.id());
        center.link(Direction.SOUTH, southArm.id());
        center.link(Direction.WEST, westArm.id());

        northArm.link(Direction.SOUTH, center.id());
        northArm.link(Direction.NORTH, northEnd.id());
        eastArm.link(Direction.WEST, center.id());
        eastArm.link(Direction.EAST, eastEnd.id());
        southArm.link(Direction.NORTH, center.id());
        southArm.link(Direction.SOUTH, southEnd.id());
        westArm.link(Direction.EAST, center.id());
        westArm.link(Direction.WEST, westEnd.id());

        northEnd.link(Direction.SOUTH, northArm.id());
        eastEnd.link(Direction.WEST, eastArm.id());
        southEnd.link(Direction.NORTH, southArm.id());
        westEnd.link(Direction.EAST, westArm.id());

        return List.of(center, northArm, eastArm, southArm, westArm, northEnd, eastEnd, southEnd, westEnd);
    }

    private boolean isGraphTemplateCompatible(List<GridNode> graph) {
        if (graph == null || graph.isEmpty()) {
            return false;
        }
        for (GridNode node : graph) {
            if (selectTemplate(node.directions()) == null) {
                return false;
            }
        }
        return true;
    }

    private RoomTemplate selectTemplate(EnumSet<Direction> dirs,
                                        int straightWallsSinceGate,
                                        int towerCount,
                                        int gateCount,
                                        Map<Integer, NodePlan> placed,
                                        GridNode node,
                                        GraphMode graphMode,
                                        List<GridNode> graph) {
        int degree = dirs.size();
        if (degree >= 4) {
            RoomTemplate tower = pickRandom(towerTemplates);
            if (tower != null && canPlaceTower(node, placed, graph) && findRotationForPlacement(tower, dirs) >= 0) return tower;
            return selectTemplate(dirs);
        }
        boolean opposite = dirs.equals(EnumSet.of(Direction.NORTH, Direction.SOUTH))
                || dirs.equals(EnumSet.of(Direction.EAST, Direction.WEST));
        if (degree == 2 && opposite) {
            if (graphMode == GraphMode.TEST) {
                RoomTemplate straight = pickRandom(straightTemplates);
                if (straight != null && findRotationForPlacement(straight, dirs) >= 0) return straight;
            }
            if (straightWallsSinceGate >= 2 && towerCount > gateCount && canPlaceGate(node, placed, graph)) {
                RoomTemplate gate = pickRandom(gateTemplates);
                if (gate != null && findRotationForPlacement(gate, dirs) >= 0) return gate;
            }
            RoomTemplate tower = pickRandom(towerTemplates);
            if (tower != null && canPlaceTower(node, placed, graph) && findRotationForPlacement(tower, dirs) >= 0) return tower;
            return selectTemplate(dirs);
        }
        if (degree == 2) {
            RoomTemplate tower = pickRandom(towerTemplates);
            if (tower != null && canPlaceTower(node, placed, graph) && findRotationForPlacement(tower, dirs) >= 0) return tower;
            return pickRandom(cornerTemplates) != null ? pickRandom(cornerTemplates) : selectTemplate(dirs);
        }
        if (degree == 1) {
            RoomTemplate tower = pickRandom(towerTemplates);
            if (tower != null && canPlaceTower(node, placed, graph) && findRotationForPlacement(tower, dirs) >= 0) return tower;
            RoomTemplate dead = pickRandom(deadEndTemplates);
            if (dead != null && findRotationForPlacement(dead, dirs) >= 0) return dead;
            return selectTemplate(dirs);
        }
        if (degree == 3) {
            RoomTemplate tSection = pickRandom(tSectionTemplates);
            if (tSection != null && findRotationForPlacement(tSection, dirs) >= 0) return tSection;
            RoomTemplate tower = pickRandom(towerTemplates);
            if (tower != null && canPlaceTower(node, placed, graph) && findRotationForPlacement(tower, dirs) >= 0) return tower;
            RoomTemplate gate = pickRandom(gateTemplates);
            if (gate != null && canPlaceGate(node, placed, graph) && findRotationForPlacement(gate, dirs) >= 0) return gate;
            return selectTemplate(dirs);
        }
        return selectTemplate(dirs);
    }

    private boolean canPlaceGate(GridNode node, Map<Integer, NodePlan> placed, List<GridNode> graph) {
        for (Integer nid : node.neighbors().values()) {
            NodePlan p = placed.get(nid);
            if (p == null) continue;
            if (isGateOrTower(p.template)) return false;
        }
        return true;
    }

    private boolean canPlaceTower(GridNode node, Map<Integer, NodePlan> placed, List<GridNode> graph) {
        for (Integer nid : node.neighbors().values()) {
            NodePlan p = placed.get(nid);
            if (p != null && gateTemplates.contains(p.template)) return false;
        }
        return true;
    }

    private boolean isGateOrTower(RoomTemplate template) {
        return gateTemplates.contains(template) || towerTemplates.contains(template);
    }

    private RoomTemplate selectTemplate(EnumSet<Direction> dirs) {
        int degree = dirs.size();
        if (degree == 1) {
            RoomTemplate dead = pickRandom(deadEndTemplates);
            if (dead != null && findRotationForPlacement(dead, dirs) >= 0) return dead;
        }
        if (degree == 2) {
            boolean opposite = dirs.equals(EnumSet.of(Direction.NORTH, Direction.SOUTH))
                    || dirs.equals(EnumSet.of(Direction.EAST, Direction.WEST));
            RoomTemplate candidate = opposite ? pickRandom(straightTemplates) : pickRandom(cornerTemplates);
            if (candidate != null && findRotationForPlacement(candidate, dirs) >= 0) return candidate;
        }
        if (degree == 3) {
            RoomTemplate tSection = pickRandom(tSectionTemplates);
            if (tSection != null && findRotationForPlacement(tSection, dirs) >= 0) return tSection;
        }
        RoomTemplate fallback = pickRandom(straightTemplates);
        if (fallback != null && findRotationForPlacement(fallback, dirs) >= 0) return fallback;
        for (RoomTemplate t : allTemplates()) {
            if (findRotationForPlacement(t, dirs) >= 0) return t;
        }
        return null;
    }

    private List<RoomTemplate> allTemplates() {
        List<RoomTemplate> all = new ArrayList<>();
        all.addAll(cornerTemplates);
        all.addAll(straightTemplates);
        all.addAll(deadEndTemplates);
        all.addAll(tSectionTemplates);
        all.addAll(connectorTemplates);
        all.addAll(towerTemplates);
        all.addAll(gateTemplates);
        return all;
    }

    private int findRotationForPlacement(RoomTemplate template, Set<Direction> required) {
        int exact = findRotation(template, required);
        if (exact >= 0) {
            return exact;
        }
        if (!supportsOptionalConnectors(template)) {
            return -1;
        }
        return findRotationContaining(template, required);
    }

    private boolean supportsOptionalConnectors(RoomTemplate template) {
        return towerTemplates.contains(template) || gateTemplates.contains(template);
    }

    private int findRotationContaining(RoomTemplate template, Set<Direction> required) {
        for (int r = 0; r < 4; r++) {
            Set<Direction> rotated = template.getRotatedDirections(r);
            if (rotated.containsAll(required)) {
                return r;
            }
        }
        return -1;
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
        cornerTemplates.clear(); straightTemplates.clear(); deadEndTemplates.clear(); tSectionTemplates.clear();
        connectorTemplates.clear(); towerTemplates.clear(); gateTemplates.clear();

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

        load(tSectionTemplates, flatland, 615, -61, -5276, 685, -3, -5206);

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

    private void reportTemplateConnectorCounts(Player player) {
        reportConnectorCounts(player, "corner", cornerTemplates);
        reportConnectorCounts(player, "straight", straightTemplates);
        reportConnectorCounts(player, "deadend", deadEndTemplates);
        reportConnectorCounts(player, "tsection", tSectionTemplates);
        reportConnectorCounts(player, "connector", connectorTemplates);
        reportConnectorCounts(player, "tower", towerTemplates);
        reportConnectorCounts(player, "gate", gateTemplates);
    }

    private void reportConnectorCounts(Player player, String label, List<RoomTemplate> templates) {
        if (templates == null || templates.isEmpty()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Template connector points (" + label + "): none loaded.");
            return;
        }
        for (int i = 0; i < templates.size(); i++) {
            RoomTemplate template = templates.get(i);
            int connectorPoints = template.getConnectors().size();
            int uniqueDirections = template.getRotatedDirections(0).size();
            Set<Direction> directions = new TreeSet<>(Comparator.comparingInt(Enum::ordinal));
            directions.addAll(template.getRotatedDirections(0));
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Template connector points (" + label + "#" + (i + 1) + "): "
                            + connectorPoints + " [unique dirs=" + uniqueDirections + ", dirs=" + directions + "]");
        }
    }

    private void restoreSnapshot(Map<Location, BlockData> snapshot) {
        for (Map.Entry<Location, BlockData> e : snapshot.entrySet()) {
            Location l = e.getKey();
            if (l.getWorld() == null) continue;
            l.getWorld().getBlockAt(l).setBlockData(e.getValue(), false);
        }
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

    private record NodePlan(int id, GridNode node, RoomTemplate template, int rotation, Location center) {}

    private record ConnectorPlan(RoomTemplate template, int rotation, Location center) {}

    private record PlacementPlan(RoomTemplate template, int rotation, Location center) {}
    private record CandidatePlacement(RoomTemplate template, int rotation, Location center, double overlap) {}

    public enum GraphMode {
        SNAKE(new SnakeGraphGenerator()),
        BRANCHING(new BranchingRandomGraphGenerator(3)),
        TEST(null),
        TSECTION(null);

        private final DungeonGraphGenerator generator;

        GraphMode(DungeonGraphGenerator generator) {
            this.generator = generator;
        }

        public static GraphMode fromArg(String raw) {
            if (raw == null || raw.isBlank()) {
                return SNAKE;
            }
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "branch", "branches", "branching", "random" -> BRANCHING;
                case "test" -> TEST;
                case "tsection", "tee", "t" -> TSECTION;
                case "snake", "serpentine" -> SNAKE;
                default -> null;
            };
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static List<String> ids() {
            return Arrays.stream(values()).map(GraphMode::id).toList();
        }
    }

}
