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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class StrongholdDebugManager {
    private static final String TEMPLATE_WORLD = "flatland";
    private static final Set<Material> IGNORED_TEMPLATE_MATERIALS = Set.of(Material.WHITE_CONCRETE, Material.LIGHT_BLUE_CONCRETE);

    private final Main plugin;
    private final DungeonManager dungeonManager;
    private final Random random = new Random();

    private final List<TemplateVariant> corners = new ArrayList<>();
    private final List<TemplateVariant> straights = new ArrayList<>();
    private final List<TemplateVariant> deadEnds = new ArrayList<>();
    private final List<TemplateVariant> connectors = new ArrayList<>();
    private final List<TemplateVariant> towers = new ArrayList<>();
    private final List<TemplateVariant> gates = new ArrayList<>();

    private final Map<UUID, ActiveDebugStronghold> activeByPlayer = new HashMap<>();

    public StrongholdDebugManager(Main plugin, DungeonManager dungeonManager) {
        this.plugin = plugin;
        this.dungeonManager = dungeonManager;
        loadTemplates();
    }

    public void spawn(Player player, int size) {
        placeStronghold(player, size, false, 8L);
    }

    public void spawnStep(Player player, int size, long delayTicks) {
        placeStronghold(player, size, true, Math.max(1L, delayTicks));
    }

    public void despawn(Player player) {
        ActiveDebugStronghold active = activeByPlayer.remove(player.getUniqueId());
        if (active == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "No active stronghold debug instance.");
            return;
        }
        if (active.progressTask != null) {
            active.progressTask.cancel();
        }
        restoreSnapshot(active.restoreSnapshot);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Stronghold debug instance removed.");
    }

    private void placeStronghold(Player player, int size, boolean stepByStep, long delayTicks) {
        if (size < 1) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Size must be at least 1.");
            return;
        }
        if (!hasTemplates()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Stronghold templates unavailable in world 'flatland'.");
            return;
        }
        ActiveDebugStronghold previous = activeByPlayer.remove(player.getUniqueId());
        if (previous != null) {
            if (previous.progressTask != null) previous.progressTask.cancel();
            restoreSnapshot(previous.restoreSnapshot);
        }

        List<Node> nodes = generateSnakeGraph(size);
        if (nodes.isEmpty()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Failed to generate graph.");
            return;
        }

        Dungeon sandboxDungeon = new Dungeon(player.getWorld(), "stronghold_debug_" + player.getUniqueId());
        Map<Location, BlockData> snapshot = new LinkedHashMap<>();
        ActiveDebugStronghold active = new ActiveDebugStronghold(player.getUniqueId(), snapshot, sandboxDungeon);
        activeByPlayer.put(player.getUniqueId(), active);

        Node root = nodes.get(0);
        root.placedCenter = player.getLocation().getBlock().getLocation();

        Deque<Node> queue = new ArrayDeque<>();
        queue.add(root);
        Set<Integer> visited = new HashSet<>();

        Runnable step = () -> {
            int perTick = stepByStep ? 1 : Integer.MAX_VALUE;
            for (int i = 0; i < perTick; i++) {
                if (queue.isEmpty()) {
                    if (active.progressTask != null) active.progressTask.cancel();
                    placeConnectorBridges(nodes, sandboxDungeon, snapshot);
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                            "Stronghold debug spawned (" + size + " nodes" + (stepByStep ? ", step mode" : "") + ").");
                    return;
                }
                Node node = queue.poll();
                if (!visited.add(node.id)) continue;

                if (node != root) {
                    boolean ok = placeNode(node, nodes, sandboxDungeon, snapshot);
                    if (!ok) {
                        if (active.progressTask != null) active.progressTask.cancel();
                        restoreSnapshot(snapshot);
                        activeByPlayer.remove(player.getUniqueId());
                        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                                "Stronghold placement failed. Restored previous blocks.");
                        return;
                    }
                }
                for (int nextId : node.neighbors) {
                    queue.add(nodes.get(nextId));
                }
            }
        };

        if (stepByStep) {
            active.progressTask = Bukkit.getScheduler().runTaskTimer(plugin, step, 1L, delayTicks);
        } else {
            step.run();
        }
    }

    private boolean placeNode(Node node, List<Node> nodes, Dungeon dungeon, Map<Location, BlockData> snapshot) {
        Set<Direction> requiredDirs = node.openDirections();
        List<TemplateVariant> variants = selectCandidates(requiredDirs, nodes, node);

        for (TemplateVariant variant : variants) {
            for (int rotation = 0; rotation < 4; rotation++) {
                if (!variant.template.getRotatedDirections(rotation).equals(requiredDirs)) continue;
                Location center = solveCenter(node, nodes, variant.template, rotation);
                if (center == null) continue;
                if (!validateNeighborAlignment(node, nodes, variant.template, rotation, center)) continue;

                DungeonManager.PasteResult result = dungeonManager.pasteRoom(
                        dungeon,
                        variant.template,
                        rotation,
                        center,
                        null,
                        true,
                        IGNORED_TEMPLATE_MATERIALS
                );
                if (!result.success()) continue;
                mergeSnapshot(snapshot, result.replaced());
                node.placedCenter = center;
                node.placedTemplate = variant;
                node.rotation = rotation;
                return true;
            }
        }
        return false;
    }

    private void placeConnectorBridges(List<Node> nodes, Dungeon dungeon, Map<Location, BlockData> snapshot) {
        if (connectors.isEmpty()) return;
        Set<String> seen = new HashSet<>();
        for (Node node : nodes) {
            for (Map.Entry<Direction, Integer> edge : node.edges.entrySet()) {
                int a = node.id;
                int b = edge.getValue();
                String key = a < b ? a + ":" + b : b + ":" + a;
                if (!seen.add(key)) continue;
                Node other = nodes.get(b);
                if (node.placedCenter == null || other.placedCenter == null) continue;

                Location aLoc = connectorWorld(node, edge.getKey());
                Location bLoc = connectorWorld(other, edge.getKey().opposite());
                if (aLoc == null || bLoc == null) continue;

                Location midpoint = new Location(aLoc.getWorld(),
                        Math.floor((aLoc.getX() + bLoc.getX()) / 2.0),
                        Math.floor((aLoc.getY() + bLoc.getY()) / 2.0),
                        Math.floor((aLoc.getZ() + bLoc.getZ()) / 2.0));

                Set<Direction> dirs = EnumSet.of(edge.getKey(), edge.getKey().opposite());
                TemplateVariant bridge = selectTemplate(dirs);
                if (bridge == null) {
                    bridge = connectors.get(0);
                }
                if (bridge == null) continue;
                int rotation = findRotationForDirs(bridge.template, dirs);
                DungeonManager.PasteResult result = dungeonManager.pasteRoom(
                        dungeon, bridge.template, rotation, midpoint, null, true, IGNORED_TEMPLATE_MATERIALS);
                if (result.success()) {
                    mergeSnapshot(snapshot, result.replaced());
                }
            }
        }
    }

    private int findRotationForDirs(RoomTemplate template, Set<Direction> dirs) {
        for (int r = 0; r < 4; r++) {
            if (template.getRotatedDirections(r).equals(dirs)) return r;
        }
        return 0;
    }

    private boolean validateNeighborAlignment(Node node, List<Node> nodes, RoomTemplate template, int rotation, Location center) {
        for (Map.Entry<Direction, Integer> edge : node.edges.entrySet()) {
            Node other = nodes.get(edge.getValue());
            if (other.placedCenter == null) continue;
            Location own = connectorWorld(template, rotation, center, edge.getKey());
            Location their = connectorWorld(other, edge.getKey().opposite());
            if (own == null || their == null) return false;
            if (own.getBlockX() != their.getBlockX() || own.getBlockY() != their.getBlockY() || own.getBlockZ() != their.getBlockZ()) {
                return false;
            }
        }
        return true;
    }

    private Location solveCenter(Node node, List<Node> nodes, RoomTemplate template, int rotation) {
        for (Map.Entry<Direction, Integer> edge : node.edges.entrySet()) {
            Node neighbor = nodes.get(edge.getValue());
            if (neighbor.placedCenter == null) continue;
            Location target = connectorWorld(neighbor, edge.getKey().opposite());
            RoomTemplate.Connector ownConnector = findConnector(template, rotation, edge.getKey());
            if (target == null || ownConnector == null) continue;
            int[] vec = RoomTemplate.rotate(ownConnector.x - (int) Math.round(template.getCenterX()),
                    ownConnector.z - (int) Math.round(template.getCenterZ()), rotation);
            return target.clone().subtract(vec[0], ownConnector.bottomY - template.getConnectorMinY(), vec[1]);
        }
        return null;
    }

    private Location connectorWorld(Node node, Direction direction) {
        if (node.placedTemplate == null || node.placedCenter == null) return null;
        return connectorWorld(node.placedTemplate.template, node.rotation, node.placedCenter, direction);
    }

    private Location connectorWorld(RoomTemplate template, int rotation, Location center, Direction direction) {
        RoomTemplate.Connector connector = findConnector(template, rotation, direction);
        if (connector == null) return null;
        int[] vec = RoomTemplate.rotate(connector.x - (int) Math.round(template.getCenterX()),
                connector.z - (int) Math.round(template.getCenterZ()), rotation);
        return center.clone().add(vec[0], connector.bottomY - template.getConnectorMinY(), vec[1]);
    }

    private RoomTemplate.Connector findConnector(RoomTemplate template, int rotation, Direction target) {
        for (RoomTemplate.Connector connector : template.getConnectors()) {
            Direction rotated = Direction.values()[(connector.facing.ordinal() + rotation) & 3];
            if (rotated == target) return connector;
        }
        return null;
    }

    private List<TemplateVariant> selectCandidates(Set<Direction> dirs, List<Node> nodes, Node node) {
        int degree = dirs.size();
        boolean opposite = degree == 2 && (dirs.contains(Direction.NORTH) == dirs.contains(Direction.SOUTH));
        List<TemplateVariant> candidates = new ArrayList<>();

        if (degree == 1) {
            candidates.addAll(deadEnds);
            candidates.addAll(towers);
            TemplateVariant fallback = selectTemplate(dirs);
            if (fallback != null) candidates.add(fallback);
            return dedupe(candidates);
        }
        if (degree == 2 && !opposite) {
            candidates.addAll(pickCornerFirst());
            TemplateVariant fallback = selectTemplate(dirs);
            if (fallback != null) candidates.add(fallback);
            return dedupe(candidates);
        }
        if (degree == 2) {
            if (canPlaceGate(nodes, node)) candidates.addAll(gates);
            if (canPlaceTower(nodes, node)) candidates.addAll(towers);
            candidates.addAll(straights);
            TemplateVariant fallback = selectTemplate(dirs);
            if (fallback != null) candidates.add(fallback);
            return dedupe(candidates);
        }
        TemplateVariant fallback = selectTemplate(dirs);
        if (fallback != null) candidates.add(fallback);
        return dedupe(candidates);
    }

    private List<TemplateVariant> pickCornerFirst() {
        List<TemplateVariant> combined = new ArrayList<>();
        combined.addAll(towers);
        combined.addAll(corners);
        return combined;
    }

    private boolean canPlaceGate(List<Node> nodes, Node node) {
        int straightWallsSinceGate = node.id;
        if (straightWallsSinceGate < 2) return false;
        long towerCount = nodes.stream().filter(n -> n.placedTemplate != null && n.placedTemplate.kind == TemplateKind.TOWER).count();
        long gateCount = nodes.stream().filter(n -> n.placedTemplate != null && n.placedTemplate.kind == TemplateKind.GATE).count();
        if (towerCount <= gateCount) return false;
        for (int nId : node.neighbors) {
            Node neighbor = nodes.get(nId);
            if (neighbor.placedTemplate == null) continue;
            if (neighbor.placedTemplate.kind == TemplateKind.GATE || neighbor.placedTemplate.kind == TemplateKind.TOWER) return false;
        }
        return true;
    }

    private boolean canPlaceTower(List<Node> nodes, Node node) {
        for (int nId : node.neighbors) {
            Node neighbor = nodes.get(nId);
            if (neighbor.placedTemplate != null && neighbor.placedTemplate.kind == TemplateKind.GATE) return false;
        }
        return true;
    }

    private TemplateVariant selectTemplate(Set<Direction> dirs) {
        List<TemplateVariant> all = new ArrayList<>();
        all.addAll(deadEnds);
        all.addAll(corners);
        all.addAll(straights);
        all.addAll(towers);
        all.addAll(gates);
        all.addAll(connectors);
        all.sort(Comparator.comparing(v -> v.name));
        for (TemplateVariant variant : all) {
            for (int r = 0; r < 4; r++) {
                if (variant.template.getRotatedDirections(r).equals(dirs)) return variant;
            }
        }
        return null;
    }

    private List<TemplateVariant> dedupe(List<TemplateVariant> input) {
        List<TemplateVariant> deduped = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (TemplateVariant v : input) {
            if (seen.add(v.name)) deduped.add(v);
        }
        return deduped;
    }

    private void mergeSnapshot(Map<Location, BlockData> snapshot, Map<Location, BlockData> replaced) {
        for (Map.Entry<Location, BlockData> entry : replaced.entrySet()) {
            snapshot.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    private void restoreSnapshot(Map<Location, BlockData> snapshot) {
        List<Map.Entry<Location, BlockData>> entries = new ArrayList<>(snapshot.entrySet());
        Collections.reverse(entries);
        for (Map.Entry<Location, BlockData> entry : entries) {
            Location loc = entry.getKey();
            if (loc.getWorld() == null) continue;
            loc.getWorld().getBlockAt(loc).setBlockData(entry.getValue(), false);
        }
    }

    private List<Node> generateSnakeGraph(int size) {
        Map<String, Integer> indexByGrid = new HashMap<>();
        List<Node> nodes = new ArrayList<>();
        int x = 0;
        int z = 0;
        int width = Math.max(2, (int) Math.ceil(Math.sqrt(size)));
        boolean east = true;

        for (int i = 0; i < size; i++) {
            Node node = new Node(i, x, z);
            nodes.add(node);
            indexByGrid.put(x + ":" + z, i);

            if (east) {
                if (x < width - 1) {
                    x++;
                } else {
                    z++;
                    east = false;
                }
            } else {
                if (x > 0) {
                    x--;
                } else {
                    z++;
                    east = true;
                }
            }
        }

        for (Node node : nodes) {
            connectIfPresent(node, Direction.NORTH, node.gridX, node.gridZ - 1, indexByGrid);
            connectIfPresent(node, Direction.SOUTH, node.gridX, node.gridZ + 1, indexByGrid);
            connectIfPresent(node, Direction.EAST, node.gridX + 1, node.gridZ, indexByGrid);
            connectIfPresent(node, Direction.WEST, node.gridX - 1, node.gridZ, indexByGrid);
        }
        return nodes;
    }

    private void connectIfPresent(Node node, Direction dir, int x, int z, Map<String, Integer> indexByGrid) {
        Integer neighbor = indexByGrid.get(x + ":" + z);
        if (neighbor != null) {
            node.edges.put(dir, neighbor);
            node.neighbors.add(neighbor);
        }
    }

    private boolean hasTemplates() {
        return !(corners.isEmpty() || straights.isEmpty() || deadEnds.isEmpty());
    }

    private void loadTemplates() {
        World world = Bukkit.getWorld(TEMPLATE_WORLD);
        if (world == null) {
            plugin.getLogger().warning("[StrongholdDebug] World 'flatland' not found; templates unavailable.");
            return;
        }
        corners.clear();
        straights.clear();
        deadEnds.clear();
        connectors.clear();
        towers.clear();
        gates.clear();

        load(corners, TemplateKind.CORNER, "corner_1", world, 473, -38, -5346, 543, -61, -5276);
        load(corners, TemplateKind.CORNER, "corner_2", world, 544, -38, -5631, 614, -61, -5701);
        load(corners, TemplateKind.CORNER, "corner_3", world, 614, -61, -5630, 544, -38, -5560);

        load(straights, TemplateKind.STRAIGHT, "straight_1", world, 402, -38, -5276, 472, -61, -5346);
        load(straights, TemplateKind.STRAIGHT, "straight_2", world, 472, -61, -5347, 402, -38, -5417);
        load(straights, TemplateKind.STRAIGHT, "straight_3", world, 402, -38, -5418, 472, -61, -5488);
        load(straights, TemplateKind.STRAIGHT, "straight_4", world, 472, -61, -5489, 402, -38, -5559);
        load(straights, TemplateKind.STRAIGHT, "straight_5", world, 402, -38, -5560, 472, -61, -5630);
        load(straights, TemplateKind.STRAIGHT, "straight_6", world, 472, -61, -5631, 402, -38, -5701);
        load(straights, TemplateKind.STRAIGHT, "straight_7", world, 473, -38, -5701, 543, -61, -5631);
        load(straights, TemplateKind.STRAIGHT, "straight_8", world, 543, -61, -5630, 473, -38, -5560);
        load(straights, TemplateKind.STRAIGHT, "straight_9", world, 473, -38, -5417, 543, -61, -5347);

        load(deadEnds, TemplateKind.DEAD_END, "dead_end_1", world, 543, -38, -5418, 473, -61, -5488);
        load(deadEnds, TemplateKind.DEAD_END, "dead_end_2", world, 473, -61, -5489, 543, -38, -5559);

        load(connectors, TemplateKind.CONNECTOR, "connector_1", world, 412, -61, -5711, 402, -38, -5701);
        load(connectors, TemplateKind.CONNECTOR, "connector_2", world, 402, -38, -5721, 412, -61, -5711);

        load(towers, TemplateKind.TOWER, "tower_1", world, 615, -61, -5488, 685, -7, -5418);

        load(gates, TemplateKind.GATE, "gate_1", world, 686, -61, -5346, 614, -10, -5418);
        load(gates, TemplateKind.GATE, "gate_2", world, 686, -61, -5276, 614, -10, -5346);
    }

    private void load(List<TemplateVariant> target, TemplateKind kind, String name, World world,
                      int x1, int y1, int z1, int x2, int y2, int z2) {
        RoomTemplate captured = RoomTemplate.capture(world, x1, y1, z1, x2, y2, z2, false);
        target.add(new TemplateVariant(name, kind, captured));
    }

    private enum TemplateKind {
        CORNER,
        STRAIGHT,
        DEAD_END,
        CONNECTOR,
        TOWER,
        GATE
    }

    private record TemplateVariant(String name, TemplateKind kind, RoomTemplate template) {}

    private static class Node {
        private final int id;
        private final int gridX;
        private final int gridZ;
        private final Map<Direction, Integer> edges = new EnumMap<>(Direction.class);
        private final List<Integer> neighbors = new ArrayList<>();
        private Location placedCenter;
        private TemplateVariant placedTemplate;
        private int rotation;

        private Node(int id, int gridX, int gridZ) {
            this.id = id;
            this.gridX = gridX;
            this.gridZ = gridZ;
        }

        private Set<Direction> openDirections() {
            return edges.keySet().isEmpty() ? EnumSet.noneOf(Direction.class) : EnumSet.copyOf(edges.keySet());
        }
    }

    private static class ActiveDebugStronghold {
        private final UUID playerId;
        private final Map<Location, BlockData> restoreSnapshot;
        private final Dungeon dungeon;
        private BukkitTask progressTask;

        private ActiveDebugStronghold(UUID playerId, Map<Location, BlockData> restoreSnapshot, Dungeon dungeon) {
            this.playerId = playerId;
            this.restoreSnapshot = restoreSnapshot;
            this.dungeon = dungeon;
        }
    }
}
