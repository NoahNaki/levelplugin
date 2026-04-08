package me.nakilex.levelplugin.debug.stronghold;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.Direction;
import me.nakilex.levelplugin.dungeon.RoomTemplate;
import me.nakilex.levelplugin.dungeon.generation.BranchingRandomGraphGenerator;
import me.nakilex.levelplugin.dungeon.generation.DungeonGraphGenerator;
import me.nakilex.levelplugin.dungeon.generation.GridNode;
import me.nakilex.levelplugin.dungeon.generation.SnakeGraphGenerator;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.*;
import java.util.stream.Collectors;

public final class StrongholdDebugService {
    private static final int CONNECTOR_SPACING = 11;
    private static final Set<Material> PASTE_IGNORED = EnumSet.of(Material.AIR, Material.WHITE_CONCRETE,
            Material.LIGHT_BLUE_CONCRETE, Material.REDSTONE_BLOCK);

    private static final List<TemplateCapture> CATALOG = List.of(
            cap("corner_1", 473, -38, -5346, 543, -61, -5276, Tag.CORNER),
            cap("corner_2", 544, -38, -5631, 614, -61, -5701, Tag.CORNER),
            cap("corner_3", 614, -61, -5630, 544, -38, -5560, Tag.CORNER),
            cap("straight_1", 402, -38, -5276, 472, -61, -5346, Tag.STRAIGHT),
            cap("straight_2", 472, -61, -5347, 402, -38, -5417, Tag.STRAIGHT),
            cap("straight_3", 402, -38, -5418, 472, -61, -5488, Tag.STRAIGHT),
            cap("straight_4", 472, -61, -5489, 402, -38, -5559, Tag.STRAIGHT),
            cap("straight_5", 402, -38, -5560, 472, -61, -5630, Tag.STRAIGHT),
            cap("straight_6", 472, -61, -5631, 402, -38, -5701, Tag.STRAIGHT),
            cap("straight_7", 473, -38, -5701, 543, -61, -5631, Tag.STRAIGHT),
            cap("straight_8", 543, -61, -5630, 473, -38, -5560, Tag.STRAIGHT),
            cap("straight_9", 473, -38, -5417, 543, -61, -5347, Tag.STRAIGHT),
            cap("deadend_1", 543, -38, -5418, 473, -61, -5488, Tag.DEADEND),
            cap("deadend_2", 473, -61, -5489, 543, -38, -5559, Tag.DEADEND),
            cap("connector_1", 412, -61, -5711, 402, -38, -5701, Tag.CONNECTOR),
            cap("connector_2", 402, -38, -5721, 412, -61, -5711, Tag.CONNECTOR),
            cap("tower_1", 615, -61, -5488, 685, -7, -5418, Tag.TOWER, Tag.LARGE, Tag.LANDMARK),
            cap("tower_2", 615, -61, -5276, 685, -7, -5206, Tag.TOWER, Tag.LARGE, Tag.LANDMARK),
            cap("gate_1", 686, -61, -5346, 614, -10, -5418, Tag.GATE, Tag.LARGE, Tag.LANDMARK),
            cap("gate_2", 686, -61, -5276, 614, -10, -5346, Tag.GATE, Tag.LARGE, Tag.LANDMARK)
    );

    public void run(Player player, String[] args) {
        if (args.length == 0) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Usage: /debug stronghold <generate|templates|validate> ...");
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "templates" -> listTemplates(player);
            case "validate" -> validateTemplates(player);
            case "generate" -> generate(player, args);
            default -> ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Unknown stronghold debug action: " + args[0]);
        }
    }

    public List<String> tabComplete(String[] args) {
        if (args.length == 2) return filter(List.of("generate", "templates", "validate"), args[1]);
        if (args.length == 3 && "generate".equalsIgnoreCase(args[1])) {
            return filter(Arrays.stream(Mode.values()).map(Enum::name).map(String::toLowerCase).toList(), args[2]);
        }
        if (args.length == 4 && "generate".equalsIgnoreCase(args[1])) {
            return filter(List.of(String.valueOf(System.currentTimeMillis())), args[3]);
        }
        if (args.length == 5 && "generate".equalsIgnoreCase(args[1])) return filter(List.of("2", "8", "12", "20"), args[4]);
        return List.of();
    }

    private void generate(Player player, String[] args) {
        Mode mode = args.length >= 2 ? Mode.parse(args[1]) : Mode.TEST;
        long seed = args.length >= 3 ? parseLong(args[2], 1337L) : 1337L;
        int rooms = args.length >= 4 ? (int) Math.max(2, parseLong(args[3], mode == Mode.TEST ? 2 : 12)) : (mode == Mode.TEST ? 2 : 12);
        Main.getInstance().getLogger().info("[Stronghold] generate seed=" + seed + " mode=" + mode + " rooms=" + rooms);

        List<CapturedTemplate> catalog = captureAll(player.getWorld());
        List<CapturedTemplate> roomCatalog = catalog.stream().filter(t -> !t.tags.contains(Tag.CONNECTOR)).toList();
        List<CapturedTemplate> connectorCatalog = catalog.stream().filter(t -> t.tags.contains(Tag.CONNECTOR)).toList();

        Random random = new Random(seed);
        List<GridNode> nodes = buildGraph(mode, rooms, random);
        if (nodes.isEmpty()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "No graph nodes generated.");
            return;
        }

        Map<Integer, PlacedRoom> byId = new HashMap<>();
        List<PlacedRoom> placedRooms = new ArrayList<>();
        List<PlacedConnector> placedConnectors = new ArrayList<>();

        CapturedTemplate rootTemplate = chooseTemplateForDegree(roomCatalog, nodes.get(0).directions().size(), random);
        if (rootTemplate == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "No root template available.");
            return;
        }
        PlacedRoom root = new PlacedRoom(0, rootTemplate, 0, player.getLocation().toBlockLocation(),
                computeBounds(rootTemplate.template, player.getLocation().toBlockLocation(), 0));
        byId.put(0, root);
        placedRooms.add(root);

        for (GridNode node : nodes) {
            if (node.id() == 0) continue;
            int parentId = node.neighbors().values().stream().filter(byId::containsKey).findFirst().orElse(-1);
            if (parentId < 0) {
                Main.getInstance().getLogger().warning("[Stronghold] failure node=" + node.id() + " requiredDegree=" + node.directions().size() + " candidateCount=0");
                return;
            }
            Direction parentDir = directionToNeighbor(nodes.get(parentId), node.id());
            PlacementAttempt attempt = placeChild(node, byId.get(parentId), parentDir, roomCatalog, placedRooms, random);
            if (attempt == null) {
                Main.getInstance().getLogger().warning("[Stronghold] failure node=" + node.id() + " requiredDegree=" + node.directions().size() + " candidateCount=" + roomCatalog.size());
                return;
            }
            byId.put(node.id(), attempt.room);
            placedRooms.add(attempt.room);
        }

        Set<String> seenEdges = new HashSet<>();
        for (GridNode node : nodes) {
            PlacedRoom a = byId.get(node.id());
            for (Integer neighborId : node.neighbors().values()) {
                String edge = Math.min(node.id(), neighborId) + ":" + Math.max(node.id(), neighborId);
                if (!seenEdges.add(edge)) continue;
                PlacedRoom b = byId.get(neighborId);
                placeConnectorBetween(a, b, connectorCatalog, placedRooms, placedConnectors);
            }
        }

        materialize(placedRooms, placedConnectors, player.getWorld());
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Stronghold generated seed=" + seed + " mode=" + mode + " rooms=" + placedRooms.size() + " connectors=" + placedConnectors.size());
    }

    private PlacementAttempt placeChild(GridNode node, PlacedRoom parent, Direction parentDir, List<CapturedTemplate> roomCatalog,
                                        List<PlacedRoom> existing, Random random) {
        List<CapturedTemplate> candidates = degreeCandidates(roomCatalog, node.directions().size());
        Collections.shuffle(candidates, random);
        for (CapturedTemplate candidate : candidates) {
            for (int rotation = 0; rotation < 4; rotation++) {
                List<RoomTemplate.Connector> parentConnectors = connectorsFacing(parent.template.template, parent.rotation, parentDir);
                if (parentConnectors.isEmpty()) continue;
                for (RoomTemplate.Connector parentConnector : parentConnectors) {
                    for (RoomTemplate.Connector childConnector : candidate.template.getConnectors()) {
                        if (rotate(childConnector.facing, rotation) != parentDir.opposite()) continue;
                        if (parentConnector.type != childConnector.type) continue;
                        Location parentPoint = connectorWorld(parent.template.template, parent.center, parent.rotation, parentConnector);
                        Location solved = solveCenter(candidate.template, rotation, childConnector, parentPoint);
                        BoundingBox solvedBounds = computeBounds(candidate.template, solved, rotation);
                        int maxPush = Math.max(32, parent.template.template.getWidth() + parent.template.template.getDepth()
                                + candidate.template.getWidth() + candidate.template.getDepth() + CONNECTOR_SPACING);
                        int push = 0;
                        while (push <= maxPush && overlapsRooms(solvedBounds, existing)) {
                            Main.getInstance().getLogger().info("[StrongholdPlacement] reject overlap template=" + candidate.id + " rotation=" + rotation
                                    + " connectorPair=" + rotate(parentConnector.facing, parent.rotation) + "->" + rotate(childConnector.facing, rotation)
                                    + " solved=" + solved.getBlockX() + "," + solved.getBlockY() + "," + solved.getBlockZ()
                                    + " pushTry=" + push + " overlap=" + solvedBounds);
                            push++;
                            solved = solved.clone().add(parentDir == Direction.EAST ? 1 : parentDir == Direction.WEST ? -1 : 0,
                                    0,
                                    parentDir == Direction.SOUTH ? 1 : parentDir == Direction.NORTH ? -1 : 0);
                            solvedBounds = computeBounds(candidate.template, solved, rotation);
                        }
                        if (overlapsRooms(solvedBounds, existing)) continue;
                        Main.getInstance().getLogger().info("[StrongholdPlacement] template=" + candidate.id + " rotation=" + rotation
                                + " connectorPair=" + rotate(parentConnector.facing, parent.rotation) + "->" + rotate(childConnector.facing, rotation)
                                + " solved=" + solved.getBlockX() + "," + solved.getBlockY() + "," + solved.getBlockZ());
                        return new PlacementAttempt(new PlacedRoom(node.id(), candidate, rotation, solved, solvedBounds));
                    }
                }
            }
        }
        return null;
    }

    private void placeConnectorBetween(PlacedRoom roomA, PlacedRoom roomB, List<CapturedTemplate> connectorCatalog,
                                       List<PlacedRoom> existingRooms, List<PlacedConnector> out) {
        ConnectorEndpoint endpointA = closest(roomA, roomB.center);
        ConnectorEndpoint endpointB = closest(roomB, roomA.center);
        if (endpointA == null || endpointB == null) return;

        ConnectorCandidate best = null;
        for (CapturedTemplate connectorTemplate : connectorCatalog) {
            for (int rotation = 0; rotation < 4; rotation++) {
                for (RoomTemplate.Connector in : connectorTemplate.template.getConnectors()) {
                    for (RoomTemplate.Connector outConn : connectorTemplate.template.getConnectors()) {
                        if (in == outConn) continue;
                        if (rotate(in.facing, rotation) != endpointA.facing.opposite()) continue;
                        if (rotate(outConn.facing, rotation) != endpointB.facing.opposite()) continue;
                        if (in.type != endpointA.type || outConn.type != endpointB.type) continue;
                        Location center = solveCenter(connectorTemplate.template, rotation, in, endpointA.position);
                        Location solvedOut = connectorWorld(connectorTemplate.template, center, rotation, outConn);
                        double score = solvedOut.distanceSquared(endpointB.position);
                        BoundingBox bounds = computeBounds(connectorTemplate.template, center, rotation);
                        if (overlapsRooms(bounds, existingRooms)) continue;
                        if (best == null || score < best.score) {
                            best = new ConnectorCandidate(connectorTemplate, rotation, center, bounds, score);
                        }
                    }
                }
            }
        }
        if (best != null) {
            Main.getInstance().getLogger().info("[StrongholdConnector] chosen=" + best.template.id + " rotation=" + best.rotation + " score=" + best.score);
            out.add(new PlacedConnector(best.template, best.rotation, best.center, best.bounds));
        }
    }

    private void materialize(List<PlacedRoom> rooms, List<PlacedConnector> connectors, World world) {
        for (PlacedRoom room : rooms) paste(room.template.template, room.center, room.rotation, world);
        for (PlacedConnector connector : connectors) paste(connector.template.template, connector.center, connector.rotation, world);
    }

    private void paste(RoomTemplate template, Location center, int rotation, World world) {
        for (RoomTemplate.BlockDef block : template.getBlocks()) {
            if (PASTE_IGNORED.contains(block.data.getMaterial())) continue;
            int[] vec = RoomTemplate.rotate(block.x - (int) Math.round(template.getCenterX()),
                    block.z - (int) Math.round(template.getCenterZ()), rotation);
            int wx = center.getBlockX() + vec[0];
            int wy = center.getBlockY() + (block.y - template.getConnectorMinY());
            int wz = center.getBlockZ() + vec[1];
            Block target = world.getBlockAt(wx, wy, wz);
            target.setBlockData(RoomTemplate.rotateBlockData(block.data, rotation), false);
        }
    }

    private void listTemplates(Player player) {
        for (CapturedTemplate template : captureAll(player.getWorld())) {
            String tags = template.tags.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    template.id + " tags=[" + tags + "] connectors=" + template.template.getConnectors().size());
        }
    }

    private void validateTemplates(Player player) {
        for (CapturedTemplate template : captureAll(player.getWorld())) {
            String connectors = template.template.getConnectors().stream()
                    .map(c -> c.facing + "/" + c.type + "@" + c.x + "," + c.bottomY + "," + c.z)
                    .collect(Collectors.joining(" | "));
            Main.getInstance().getLogger().info("[StrongholdValidate] " + template.id + " connectors=" + connectors);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, template.id + " -> " + connectors);
        }
    }

    private List<CapturedTemplate> captureAll(World world) {
        List<CapturedTemplate> captured = new ArrayList<>();
        for (TemplateCapture capture : CATALOG) {
            RoomTemplate template = RoomTemplate.capture(world, capture.x1, capture.y1, capture.z1,
                    capture.x2, capture.y2, capture.z2, false);
            captured.add(new CapturedTemplate(capture.id, capture.tags, template));
        }
        return captured;
    }

    private List<GridNode> buildGraph(Mode mode, int rooms, Random random) {
        if (mode == Mode.TEST) {
            GridNode a = new GridNode(0, 0, 0);
            GridNode b = new GridNode(1, 1, 0);
            a.link(Direction.EAST, 1);
            b.link(Direction.WEST, 0);
            return List.of(a, b);
        }
        DungeonGraphGenerator generator = mode == Mode.SNAKE ? new SnakeGraphGenerator() : new BranchingRandomGraphGenerator(2);
        return generator.generate(rooms, random);
    }

    private List<CapturedTemplate> degreeCandidates(List<CapturedTemplate> roomCatalog, int degree) {
        List<CapturedTemplate> candidates = new ArrayList<>();
        for (CapturedTemplate t : roomCatalog) {
            if (degree <= 1 && t.tags.contains(Tag.DEADEND)) candidates.add(t);
            else if (degree == 2 && (t.tags.contains(Tag.STRAIGHT) || t.tags.contains(Tag.CORNER))) candidates.add(t);
            else if (degree >= 3 && t.tags.contains(Tag.LANDMARK)) candidates.add(t);
        }
        if (candidates.isEmpty()) candidates.addAll(roomCatalog);
        return candidates;
    }

    private CapturedTemplate chooseTemplateForDegree(List<CapturedTemplate> roomCatalog, int degree, Random random) {
        List<CapturedTemplate> candidates = degreeCandidates(roomCatalog, degree);
        return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
    }

    private static List<RoomTemplate.Connector> connectorsFacing(RoomTemplate template, int rotation, Direction target) {
        List<RoomTemplate.Connector> out = new ArrayList<>();
        for (RoomTemplate.Connector connector : template.getConnectors()) {
            if (rotate(connector.facing, rotation) == target) {
                out.add(connector);
            }
        }
        return out;
    }

    private static Direction directionToNeighbor(GridNode node, int neighborId) {
        for (Map.Entry<Direction, Integer> entry : node.neighbors().entrySet()) {
            if (entry.getValue() == neighborId) return entry.getKey();
        }
        return Direction.NORTH;
    }

    private static ConnectorEndpoint closest(PlacedRoom source, Location targetCenter) {
        ConnectorEndpoint best = null;
        for (RoomTemplate.Connector connector : source.template.template.getConnectors()) {
            Location point = connectorWorld(source.template.template, source.center, source.rotation, connector);
            double score = point.distanceSquared(targetCenter);
            ConnectorEndpoint candidate = new ConnectorEndpoint(point, rotate(connector.facing, source.rotation), connector.type, score);
            if (best == null || candidate.score < best.score) best = candidate;
        }
        return best;
    }

    private static Location solveCenter(RoomTemplate template, int rotation, RoomTemplate.Connector connector, Location target) {
        int[] vec = RoomTemplate.rotate(connector.x - (int) Math.round(template.getCenterX()),
                connector.z - (int) Math.round(template.getCenterZ()), rotation);
        return new Location(target.getWorld(), target.getBlockX() - vec[0],
                target.getBlockY() - (connector.bottomY - template.getConnectorMinY()), target.getBlockZ() - vec[1]);
    }

    private static Location connectorWorld(RoomTemplate template, Location center, int rotation, RoomTemplate.Connector connector) {
        int[] vec = RoomTemplate.rotate(connector.x - (int) Math.round(template.getCenterX()),
                connector.z - (int) Math.round(template.getCenterZ()), rotation);
        return center.clone().add(vec[0], connector.bottomY - template.getConnectorMinY(), vec[1]);
    }

    private static BoundingBox computeBounds(RoomTemplate template, Location center, int rotation) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (RoomTemplate.BlockDef block : template.getBlocks()) {
            int[] vec = RoomTemplate.rotate(block.x - (int) Math.round(template.getCenterX()),
                    block.z - (int) Math.round(template.getCenterZ()), rotation);
            int wx = center.getBlockX() + vec[0];
            int wy = center.getBlockY() + (block.y - template.getConnectorMinY());
            int wz = center.getBlockZ() + vec[1];
            minX = Math.min(minX, wx);
            minY = Math.min(minY, wy);
            minZ = Math.min(minZ, wz);
            maxX = Math.max(maxX, wx);
            maxY = Math.max(maxY, wy);
            maxZ = Math.max(maxZ, wz);
        }
        return new BoundingBox(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    private static boolean overlapsRooms(BoundingBox candidate, List<PlacedRoom> rooms) {
        for (PlacedRoom room : rooms) if (room.bounds.overlaps(candidate)) return true;
        return false;
    }

    private static Direction rotate(Direction direction, int rotation) {
        return Direction.values()[(direction.ordinal() + rotation) & 3];
    }

    private static TemplateCapture cap(String id, int x1, int y1, int z1, int x2, int y2, int z2, Tag... tags) {
        return new TemplateCapture(id, x1, y1, z1, x2, y2, z2, EnumSet.copyOf(Arrays.asList(tags)));
    }

    private static List<String> filter(List<String> entries, String prefix) {
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return entries.stream().filter(v -> v.toLowerCase(Locale.ROOT).startsWith(p)).toList();
    }

    private static long parseLong(String value, long fallback) {
        try { return Long.parseLong(value); } catch (Exception ignored) { return fallback; }
    }

    private enum Tag { CORNER, STRAIGHT, DEADEND, CONNECTOR, TOWER, GATE, LARGE, LANDMARK }

    private enum Mode {
        TEST, SNAKE, BRANCHING;

        static Mode parse(String input) {
            if (input == null) return TEST;
            return switch (input.toLowerCase(Locale.ROOT)) {
                case "snake" -> SNAKE;
                case "branching" -> BRANCHING;
                default -> TEST;
            };
        }
    }

    private record TemplateCapture(String id, int x1, int y1, int z1, int x2, int y2, int z2, EnumSet<Tag> tags) {}

    private record CapturedTemplate(String id, EnumSet<Tag> tags, RoomTemplate template) {}

    private record PlacedRoom(int nodeId, CapturedTemplate template, int rotation, Location center, BoundingBox bounds) {}

    private record PlacedConnector(CapturedTemplate template, int rotation, Location center, BoundingBox bounds) {}

    private record PlacementAttempt(PlacedRoom room) {}

    private record ConnectorEndpoint(Location position, Direction facing, RoomTemplate.ConnectorType type, double score) {}

    private record ConnectorCandidate(CapturedTemplate template, int rotation, Location center, BoundingBox bounds, double score) {}
}
