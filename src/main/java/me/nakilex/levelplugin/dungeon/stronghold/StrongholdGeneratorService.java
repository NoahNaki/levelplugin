package me.nakilex.levelplugin.dungeon.stronghold;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.Direction;
import me.nakilex.levelplugin.dungeon.generation.BranchingRandomGraphGenerator;
import me.nakilex.levelplugin.dungeon.generation.DungeonGraphGenerator;
import me.nakilex.levelplugin.dungeon.generation.GridNode;
import me.nakilex.levelplugin.dungeon.generation.SnakeGraphGenerator;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Stronghold graph-first generator that realizes nodes through templates and
 * canonical connector transforms.
 */
public class StrongholdGeneratorService {
    private static final int DEFAULT_MARGIN = 0;
    private static final int BRIDGE_GAP = 10;

    private final Main plugin;
    private final List<Template> catalog;

    public StrongholdGeneratorService(Main plugin) {
        this.plugin = plugin;
        this.catalog = buildCatalog();
    }

    public GenerationResult generate(Player player, GraphMode mode, long seed, int nodeCount, int maxAttempts) {
        if (mode == GraphMode.TEST) {
            return generateSimplePair(player, seed);
        }
        Random random = new Random(seed);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            List<NodeSpec> graph = generateGraph(mode, nodeCount, random);
            GenerationContext context = new GenerationContext(graph, seed, attempt);
            if (placeGraph(context, random)) {
                insertConnectors(context, random);
                renderDebug(context, player.getWorld(), player.getLocation().toVector());
                sendSummary(player, context);
                return new GenerationResult(true, "ok", context);
            }
            plugin.getLogger().info("[Stronghold] Generation attempt " + attempt + " failed; regenerating graph.");
        }
        if (mode == GraphMode.BRANCHING) {
            plugin.getLogger().info("[Stronghold] Branching mode failed, falling back to snake topology for stability.");
            return generate(player, GraphMode.SNAKE, seed, nodeCount, maxAttempts);
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                "Stronghold generation failed after " + maxAttempts + " graph attempts.");
        return new GenerationResult(false, "placement_failed", null);
    }

    private GenerationResult generateSimplePair(Player player, long seed) {
        Template room = catalog.stream()
                .filter(t -> t.tags.contains(StrongholdTemplateTag.STRAIGHT))
                .findFirst()
                .orElse(null);
        Template bridgeTemplate = catalog.stream()
                .filter(t -> t.tags.contains(StrongholdTemplateTag.CONNECTOR))
                .findFirst()
                .orElse(null);
        if (room == null || bridgeTemplate == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Stronghold test mode is missing room/connector templates.");
            return new GenerationResult(false, "missing_templates", null);
        }

        GenerationContext context = new GenerationContext(List.of(), seed, 1);
        Placement roomA = new Placement(room, new Transform(new Vec3(0, 0, 0), StrongholdRotation.R0), 0);
        context.placements.put(0, roomA);

        Connector roomAOut = connectorFacing(roomA, Direction.EAST);
        Connector bridgeIn = connectorFacing(new Placement(bridgeTemplate,
                new Transform(new Vec3(0, 0, 0), StrongholdRotation.R0), -1), Direction.WEST);
        Transform bridgeTransform = solveTransform(roomA.transform, roomAOut, bridgeIn, StrongholdRotation.R0);
        Placement bridge = new Placement(bridgeTemplate, bridgeTransform, -1);
        context.bridges.add(bridge);

        Connector bridgeOut = connectorFacing(bridge, Direction.EAST);
        Connector roomBIn = connectorFacing(new Placement(room,
                new Transform(new Vec3(0, 0, 0), StrongholdRotation.R0), 1), Direction.WEST);
        Transform roomBTransform = solveTransform(bridge.transform, bridgeOut, roomBIn, StrongholdRotation.R0);
        Placement roomB = new Placement(room, roomBTransform, 1);
        context.placements.put(1, roomB);

        context.logs.add("TEST mode simple pair: room -> connector -> room");
        renderDebug(context, player.getWorld(), player.getLocation().toVector());
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Stronghold test generated simple room-connector-room chain.");
        return new GenerationResult(true, "ok", context);
    }

    private List<NodeSpec> generateGraph(GraphMode mode, int nodeCount, Random random) {
        DungeonGraphGenerator generator = switch (mode) {
            case SNAKE -> new SnakeGraphGenerator();
            case BRANCHING -> new BranchingRandomGraphGenerator(2);
            case TEST -> (size, rng) -> {
                List<GridNode> out = new ArrayList<>();
                out.add(new GridNode(0, 0, 0));
                out.add(new GridNode(1, 1, 0));
                out.add(new GridNode(2, 2, 0));
                out.get(0).link(Direction.EAST, 1);
                out.get(1).link(Direction.WEST, 0);
                out.get(1).link(Direction.EAST, 2);
                out.get(2).link(Direction.WEST, 1);
                return out;
            };
        };

        List<GridNode> graph = generator.generate(Math.max(2, nodeCount), random);
        Map<Integer, NodeSpec> byId = new HashMap<>();
        for (GridNode node : graph) {
            byId.put(node.id(), new NodeSpec(node.id(), node.neighbors()));
        }
        return new ArrayList<>(byId.values());
    }

    private boolean placeGraph(GenerationContext context, Random random) {
        if (context.nodes.isEmpty()) {
            return false;
        }

        NodeSpec root = context.nodes.get(0);
        Template rootTemplate = selectTemplateForDegree(root.requiredDegree(), Set.of(), random, Collections.emptyList());
        if (rootTemplate == null) {
            return false;
        }
        Placement rootPlacement = new Placement(rootTemplate, new Transform(new Vec3(0, 0, 0), StrongholdRotation.R0), 0);
        context.placements.put(root.id, rootPlacement);
        context.useCount.merge(rootTemplate.id, 1, Integer::sum);
        context.logs.add("ROOT template=" + rootTemplate.id + " rot=R0 at origin");

        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(root.id);
        Set<String> visitedEdges = new HashSet<>();

        while (!queue.isEmpty()) {
            int idA = queue.removeFirst();
            NodeSpec nodeA = context.byId.get(idA);
            Placement placementA = context.placements.get(idA);
            if (placementA == null) {
                return false;
            }
            for (Map.Entry<Direction, Integer> edge : nodeA.neighbors.entrySet()) {
                int idB = edge.getValue();
                String edgeKey = edgeKey(idA, idB);
                if (!visitedEdges.add(edgeKey)) {
                    continue;
                }
                if (context.placements.containsKey(idB)) {
                    continue;
                }
                NodeSpec nodeB = context.byId.get(idB);
                if (nodeB == null) {
                    return false;
                }

                Placement placed = tryPlaceNode(context, placementA, nodeB, random);
                if (placed == null) {
                    context.logs.add("REJECT node=" + idB + " reason=no_valid_candidate");
                    return false;
                }
                context.placements.put(idB, placed);
                context.useCount.merge(placed.template.id, 1, Integer::sum);
                context.logs.add("PLACE node=" + idB + " template=" + placed.template.id + " rot=" + placed.transform.rotation);
                queue.addLast(idB);
            }
        }

        return context.placements.size() == context.nodes.size();
    }

    private Placement tryPlaceNode(GenerationContext context, Placement placementA, NodeSpec nodeB, Random random) {
        List<Template> candidates = templatesByDegree(nodeB.requiredDegree());
        candidates.sort(Comparator.comparing(Template::id));
        for (Template templateB : sortByVariety(candidates, context.useCount, random)) {
            for (StrongholdRotation rotationB : StrongholdRotation.values()) {
                List<Connector> bConnectors = templateB.connectors(rotationB);
                for (Connector aConn : placementA.template.connectors(placementA.transform.rotation)) {
                    for (Connector bConn : bConnectors) {
                        if (!compatible(aConn, bConn)) {
                            continue;
                        }
                        Transform solved = solveTransform(placementA.transform, aConn, bConn, rotationB);
                        Placement placementB = new Placement(templateB, solved, nodeB.id);
                        ValidationResult validation = validatePlacement(context.placements.values(), placementB, DEFAULT_MARGIN);
                        if (validation.valid) {
                            context.logs.add("PAIR A=" + placementA.template.id + ":" + aConn.facing + " B=" + templateB.id + ":" + bConn.facing);
                            return placementB;
                        }
                        context.logs.add("REJECT template=" + templateB.id + " rot=" + rotationB + " reason=" + validation.reason);
                    }
                }
            }
        }
        return null;
    }

    private void insertConnectors(GenerationContext context, Random random) {
        Template bridge = catalog.stream().filter(t -> t.tags.contains(StrongholdTemplateTag.CONNECTOR)).findFirst().orElse(null);
        if (bridge == null) {
            return;
        }
        Set<String> visited = new HashSet<>();
        for (NodeSpec node : context.nodes) {
            for (Integer neighborId : node.neighbors.values()) {
                String edgeKey = edgeKey(node.id, neighborId);
                if (!visited.add(edgeKey)) {
                    continue;
                }
                Placement a = context.placements.get(node.id);
                Placement b = context.placements.get(neighborId);
                if (a == null || b == null) {
                    continue;
                }
                Connector aOut = outwardConnectorToward(a, b);
                Connector bridgeIn = bridge.connectors(StrongholdRotation.R0).get(0);
                Transform first = solveTransform(a.transform, aOut, bridgeIn, StrongholdRotation.R0);
                Placement bp = new Placement(bridge, new Transform(first.position.add(aOut.directionVector().multiply(BRIDGE_GAP)), first.rotation), -1);
                if (validatePlacement(context.placements.values(), bp, 0).valid) {
                    context.bridges.add(bp);
                }
            }
        }
        if (!context.bridges.isEmpty()) {
            context.logs.add("Inserted connector bridges=" + context.bridges.size());
        }
    }

    private ValidationResult validatePlacement(Collection<Placement> existing, Placement candidate, int margin) {
        BoundingBox bounds = candidate.worldBounds();
        for (Placement p : existing) {
            BoundingBox other = p.worldBounds();
            if (!bounds.intersectsExpanded(other, margin)) {
                continue;
            }
            return new ValidationResult(false, "aabb_overlap:" + p.template.id);
        }
        return new ValidationResult(true, "ok");
    }

    private static boolean compatible(Connector a, Connector b) {
        return a.type == b.type && a.facing == b.facing.opposite();
    }

    /**
     * Canonical transform solver: B = A * aConn * inverse(bConn rotated).
     */
    public static Transform solveTransform(Transform transformA,
                                           Connector aConnWorld,
                                           Connector bConnRotated,
                                           StrongholdRotation rotationB) {
        Vec3 target = transformA.position.add(aConnWorld.localPosition);
        Vec3 bAtOrigin = bConnRotated.localPosition;
        Vec3 translation = target.subtract(bAtOrigin);
        return new Transform(translation, rotationB);
    }

    private void renderDebug(GenerationContext context, World world, Vector anchor) {
        Location base = new Location(world, anchor.getX(), anchor.getY() + 1.0, anchor.getZ());
        for (Placement placement : context.placements.values()) {
            drawBox(world, base, placement.worldBounds(), Particle.END_ROD);
            for (Connector conn : placement.template.connectors(placement.transform.rotation)) {
                Vec3 pos = placement.transform.position.add(conn.localPosition);
                Location c = base.clone().add(pos.x, pos.y + 1.2, pos.z);
                world.spawnParticle(Particle.CRIT, c, 4, 0.05, 0.05, 0.05, 0);
                Vector dir = conn.directionVector();
                world.spawnParticle(Particle.DUST, c.clone().add(dir.clone().multiply(0.7)), 1,
                        0, 0, 0, 0, new Particle.DustOptions(org.bukkit.Color.AQUA, 1.2f));
            }
        }
        for (Placement bridge : context.bridges) {
            drawBox(world, base, bridge.worldBounds(), Particle.HAPPY_VILLAGER);
        }
    }

    private void drawBox(World world, Location base, BoundingBox box, Particle particle) {
        for (double x = box.min.x; x <= box.max.x; x += Math.max(1, box.sizeX() / 4.0)) {
            for (double z = box.min.z; z <= box.max.z; z += Math.max(1, box.sizeZ() / 4.0)) {
                world.spawnParticle(particle, base.clone().add(x, box.min.y, z), 1, 0, 0, 0, 0);
                world.spawnParticle(particle, base.clone().add(x, box.max.y, z), 1, 0, 0, 0, 0);
            }
        }
    }

    private void sendSummary(Player player, GenerationContext context) {
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Stronghold generated with seed " + context.seed + " (attempt " + context.attempt + ").");
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                ChatColor.GRAY + "Rooms: " + ChatColor.WHITE + context.placements.size()
                        + ChatColor.GRAY + " | Bridges: " + ChatColor.WHITE + context.bridges.size());
        for (String line : context.logs.stream().limit(8).toList()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + line);
        }
    }

    private Template selectTemplateForDegree(int degree, Set<StrongholdTemplateTag> requiredTags, Random random, List<String> recent) {
        List<Template> candidates = new ArrayList<>();
        for (Template template : catalog) {
            if (template.connectors.size() != degree) {
                continue;
            }
            if (!template.tags.containsAll(requiredTags)) {
                continue;
            }
            candidates.add(template);
        }
        if (candidates.isEmpty()) {
            return null;
        }
        candidates = sortByVariety(candidates, Collections.emptyMap(), random);
        return candidates.get(0);
    }

    private List<Template> templatesByDegree(int degree) {
        List<Template> out = new ArrayList<>();
        for (Template template : catalog) {
            if (template.tags.contains(StrongholdTemplateTag.CONNECTOR)) {
                continue;
            }
            if (template.connectors.size() == degree) {
                out.add(template);
            }
        }
        return out;
    }

    private List<Template> sortByVariety(List<Template> candidates, Map<String, Integer> useCount, Random random) {
        List<Template> out = new ArrayList<>(candidates);
        out.sort(Comparator.comparingInt(t -> useCount.getOrDefault(t.id, 0)));
        if (out.size() > 1 && random.nextBoolean()) {
            Collections.swap(out, 0, random.nextInt(out.size()));
        }
        return out;
    }

    private Connector outwardConnectorToward(Placement from, Placement to) {
        Vec3 delta = to.transform.position.subtract(from.transform.position);
        Direction desired = Math.abs(delta.x) > Math.abs(delta.z)
                ? (delta.x >= 0 ? Direction.EAST : Direction.WEST)
                : (delta.z >= 0 ? Direction.SOUTH : Direction.NORTH);
        for (Connector connector : from.template.connectors(from.transform.rotation)) {
            if (connector.facing == desired) {
                return connector;
            }
        }
        return from.template.connectors(from.transform.rotation).get(0);
    }

    private Connector connectorFacing(Placement placement, Direction direction) {
        for (Connector connector : placement.template.connectors(placement.transform.rotation)) {
            if (connector.facing == direction) {
                return connector;
            }
        }
        return placement.template.connectors(placement.transform.rotation).get(0);
    }

    private String edgeKey(int a, int b) {
        int min = Math.min(a, b);
        int max = Math.max(a, b);
        return min + ":" + max;
    }

    private List<Template> buildCatalog() {
        List<Template> out = new ArrayList<>();
        out.add(template("corner_1", dims(71, 24, 71), EnumSet.of(StrongholdTemplateTag.CORNER),
                connector(0, 0, 35, Direction.WEST), connector(35, 0, 0, Direction.NORTH)));
        out.add(template("corner_2", dims(71, 24, 71), EnumSet.of(StrongholdTemplateTag.CORNER),
                connector(0, 0, 35, Direction.WEST), connector(35, 0, 0, Direction.NORTH)));
        out.add(template("corner_3", dims(71, 24, 71), EnumSet.of(StrongholdTemplateTag.CORNER),
                connector(0, 0, 35, Direction.WEST), connector(35, 0, 0, Direction.NORTH)));

        for (int i = 1; i <= 9; i++) {
            out.add(template("straight_" + i, dims(71, 24, 71), EnumSet.of(StrongholdTemplateTag.STRAIGHT),
                    connector(0, 0, 35, Direction.WEST), connector(70, 0, 35, Direction.EAST)));
        }

        out.add(template("deadend_1", dims(71, 24, 71), EnumSet.of(StrongholdTemplateTag.DEADEND),
                connector(0, 0, 35, Direction.WEST)));
        out.add(template("deadend_2", dims(71, 24, 71), EnumSet.of(StrongholdTemplateTag.DEADEND),
                connector(0, 0, 35, Direction.WEST)));

        out.add(template("tjunction_1", dims(71, 24, 71), EnumSet.of(StrongholdTemplateTag.FLAT),
                connector(0, 0, 35, Direction.WEST),
                connector(70, 0, 35, Direction.EAST),
                connector(35, 0, 0, Direction.NORTH)));
        out.add(template("tjunction_2", dims(71, 24, 71), EnumSet.of(StrongholdTemplateTag.FLAT),
                connector(0, 0, 35, Direction.WEST),
                connector(70, 0, 35, Direction.EAST),
                connector(35, 0, 70, Direction.SOUTH)));

        out.add(template("crossroad_1", dims(71, 24, 71), EnumSet.of(StrongholdTemplateTag.FLAT),
                connector(0, 0, 35, Direction.WEST),
                connector(70, 0, 35, Direction.EAST),
                connector(35, 0, 0, Direction.NORTH),
                connector(35, 0, 70, Direction.SOUTH)));

        out.add(template("connector_1", dims(11, 24, 11), EnumSet.of(StrongholdTemplateTag.CONNECTOR),
                connector(0, 0, 5, Direction.WEST), connector(10, 0, 5, Direction.EAST)));
        out.add(template("connector_2", dims(11, 24, 11), EnumSet.of(StrongholdTemplateTag.CONNECTOR),
                connector(0, 0, 5, Direction.WEST), connector(10, 0, 5, Direction.EAST)));

        out.add(template("tower_1", dims(71, 55, 71), EnumSet.of(StrongholdTemplateTag.TOWER, StrongholdTemplateTag.LARGE, StrongholdTemplateTag.LANDMARK),
                connector(0, 0, 35, Direction.WEST), connector(70, 0, 35, Direction.EAST)));
        out.add(template("tower_2", dims(71, 55, 71), EnumSet.of(StrongholdTemplateTag.TOWER, StrongholdTemplateTag.LARGE, StrongholdTemplateTag.LANDMARK),
                connector(0, 0, 35, Direction.WEST), connector(70, 0, 35, Direction.EAST)));

        out.add(template("gate_1", dims(73, 52, 73), EnumSet.of(StrongholdTemplateTag.GATE, StrongholdTemplateTag.LARGE, StrongholdTemplateTag.LANDMARK),
                connector(0, 0, 36, Direction.WEST), connector(72, 0, 36, Direction.EAST)));
        out.add(template("gate_2", dims(73, 52, 73), EnumSet.of(StrongholdTemplateTag.GATE, StrongholdTemplateTag.LARGE, StrongholdTemplateTag.LANDMARK),
                connector(0, 0, 36, Direction.WEST), connector(72, 0, 36, Direction.EAST)));

        return out;
    }

    private Template template(String id, BoundingBox bounds, Set<StrongholdTemplateTag> tags, Connector... connectors) {
        return new Template(id, bounds, List.of(connectors), EnumSet.copyOf(tags));
    }

    private BoundingBox dims(int sx, int sy, int sz) {
        return new BoundingBox(new Vec3(0, 0, 0), new Vec3(sx - 1, sy - 1, sz - 1));
    }

    private Connector connector(int x, int y, int z, Direction facing) {
        return new Connector(new Vec3(x, y, z), facing, StrongholdConnectorType.CORRIDOR);
    }

    public enum GraphMode {
        SNAKE,
        BRANCHING,
        TEST;

        public static GraphMode parse(String value) {
            if (value == null || value.isBlank()) {
                return SNAKE;
            }
            try {
                return GraphMode.valueOf(value.trim().toUpperCase(Locale.ENGLISH));
            } catch (IllegalArgumentException ignored) {
                return SNAKE;
            }
        }
    }

    public record GenerationResult(boolean success, String reason, GenerationContext context) {
    }

    public static class GenerationContext {
        private final List<NodeSpec> nodes;
        private final Map<Integer, NodeSpec> byId;
        private final Map<Integer, Placement> placements = new HashMap<>();
        private final List<Placement> bridges = new ArrayList<>();
        private final List<String> logs = new ArrayList<>();
        private final Map<String, Integer> useCount = new HashMap<>();
        private final long seed;
        private final int attempt;

        private GenerationContext(List<NodeSpec> nodes, long seed, int attempt) {
            this.nodes = nodes;
            this.seed = seed;
            this.attempt = attempt;
            this.byId = new HashMap<>();
            for (NodeSpec node : nodes) {
                byId.put(node.id, node);
            }
        }
    }

    public record NodeSpec(int id, Map<Direction, Integer> neighbors) {
        public int requiredDegree() {
            return neighbors.size();
        }
    }

    public record Placement(Template template, Transform transform, int nodeId) {
        public BoundingBox worldBounds() {
            return template.localBounds.transform(transform.rotation).translate(transform.position);
        }
    }

    public record Template(String id, BoundingBox localBounds, List<Connector> connectors, Set<StrongholdTemplateTag> tags) {
        public List<Connector> connectors(StrongholdRotation rotation) {
            List<Connector> out = new ArrayList<>(connectors.size());
            for (Connector connector : connectors) {
                Vec3 rotated = connector.localPosition.rotateY(rotation);
                out.add(new Connector(rotated, rotation.rotate(connector.facing), connector.type));
            }
            return out;
        }
    }

    public record Connector(Vec3 localPosition, Direction facing, StrongholdConnectorType type) {
        public Vector directionVector() {
            return switch (facing) {
                case NORTH -> new Vector(0, 0, -1);
                case SOUTH -> new Vector(0, 0, 1);
                case EAST -> new Vector(1, 0, 0);
                case WEST -> new Vector(-1, 0, 0);
            };
        }
    }

    public record Transform(Vec3 position, StrongholdRotation rotation) {
    }

    public record BoundingBox(Vec3 min, Vec3 max) {
        public BoundingBox translate(Vec3 delta) {
            return new BoundingBox(min.add(delta), max.add(delta));
        }

        public BoundingBox transform(StrongholdRotation rotation) {
            List<Vec3> points = List.of(
                    new Vec3(min.x, min.y, min.z),
                    new Vec3(min.x, min.y, max.z),
                    new Vec3(max.x, min.y, min.z),
                    new Vec3(max.x, min.y, max.z),
                    new Vec3(min.x, max.y, min.z),
                    new Vec3(min.x, max.y, max.z),
                    new Vec3(max.x, max.y, min.z),
                    new Vec3(max.x, max.y, max.z)
            );
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
            for (Vec3 point : points) {
                Vec3 r = point.rotateY(rotation);
                minX = Math.min(minX, r.x);
                minY = Math.min(minY, r.y);
                minZ = Math.min(minZ, r.z);
                maxX = Math.max(maxX, r.x);
                maxY = Math.max(maxY, r.y);
                maxZ = Math.max(maxZ, r.z);
            }
            return new BoundingBox(new Vec3(minX, minY, minZ), new Vec3(maxX, maxY, maxZ));
        }

        public boolean intersectsExpanded(BoundingBox other, int margin) {
            return overlaps(min.x - margin, max.x + margin, other.min.x, other.max.x)
                    && overlaps(min.y, max.y, other.min.y, other.max.y)
                    && overlaps(min.z - margin, max.z + margin, other.min.z, other.max.z);
        }

        private boolean overlaps(double aMin, double aMax, double bMin, double bMax) {
            return aMin <= bMax && bMin <= aMax;
        }

        public double sizeX() { return Math.max(1, max.x - min.x); }
        public double sizeZ() { return Math.max(1, max.z - min.z); }
    }

    public record Vec3(double x, double y, double z) {
        public Vec3 add(Vec3 other) {
            return new Vec3(x + other.x, y + other.y, z + other.z);
        }

        public Vec3 add(Vector vector) {
            return new Vec3(x + vector.getX(), y + vector.getY(), z + vector.getZ());
        }

        public Vec3 subtract(Vec3 other) {
            return new Vec3(x - other.x, y - other.y, z - other.z);
        }

        public Vec3 rotateY(StrongholdRotation rotation) {
            return switch (rotation) {
                case R0 -> this;
                case R90 -> new Vec3(-z, y, x);
                case R180 -> new Vec3(-x, y, -z);
                case R270 -> new Vec3(z, y, -x);
            };
        }
    }

    public record ValidationResult(boolean valid, String reason) {
    }
}
