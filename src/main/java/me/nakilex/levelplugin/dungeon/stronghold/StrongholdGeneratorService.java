package me.nakilex.levelplugin.dungeon.stronghold;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.Direction;
import me.nakilex.levelplugin.dungeon.RoomTemplate;
import me.nakilex.levelplugin.dungeon.generation.BranchingRandomGraphGenerator;
import me.nakilex.levelplugin.dungeon.generation.DungeonGraphGenerator;
import me.nakilex.levelplugin.dungeon.generation.GridNode;
import me.nakilex.levelplugin.dungeon.generation.SnakeGraphGenerator;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
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
import java.util.LinkedHashMap;

/**
 * Stronghold graph-first generator that realizes nodes through templates and
 * canonical connector transforms.
 */
public class StrongholdGeneratorService {
    private static final int DEFAULT_MARGIN = 0;
    private static final int BRIDGE_GAP = 10;
    private static final int ROOM_GAP = 12;

    private final Main plugin;
    private final List<Template> catalog;
    private final Map<String, Template> baseCatalogById = new HashMap<>();
    private final List<Template> capturedCatalog = new ArrayList<>();
    private final Map<String, TemplateSource> templateSources;
    private final Map<String, RoomTemplate> templateCache = new HashMap<>();

    public StrongholdGeneratorService(Main plugin) {
        this.plugin = plugin;
        this.catalog = buildCatalog();
        for (Template template : catalog) {
            baseCatalogById.put(template.id, template);
        }
        this.templateSources = buildTemplateSources();
    }

    public GenerationResult generate(Player player, GraphMode mode, long seed, int nodeCount, int maxAttempts) {
        plugin.getLogger().info("[Stronghold] START mode=" + mode + " seed=" + seed + " nodeCount=" + nodeCount
                + " maxAttempts=" + maxAttempts + " world=" + player.getWorld().getName()
                + " player=" + player.getName());
        ensureTemplateCache(player.getWorld());
        if (mode == GraphMode.TEST) {
            return generateSimplePair(player, seed);
        }
        Random random = new Random(seed);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            List<NodeSpec> graph = generateGraph(mode, nodeCount, random);
            GenerationContext context = new GenerationContext(graph, seed, attempt);
            logGraph(context);
            if (placeGraph(context, random)) {
                insertConnectors(context, random);
                renderDebug(context, player.getWorld(), player.getLocation().toVector());
                materializePlacements(player.getWorld(), player.getLocation(), context);
                sendSummary(player, context);
                plugin.getLogger().info("[Stronghold] SUCCESS attempt=" + attempt + " placements="
                        + context.placements.size() + " bridges=" + context.bridges.size());
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
        plugin.getLogger().info("[Stronghold] TEST MODE simple pair seed=" + seed);
        Template room = activeCatalog().stream()
                .filter(t -> t.tags.contains(StrongholdTemplateTag.STRAIGHT))
                .findFirst()
                .orElse(null);
        Template bridgeTemplate = activeCatalog().stream()
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
        context.logs.forEach(line -> plugin.getLogger().info("[Stronghold] " + line));
        renderDebug(context, player.getWorld(), player.getLocation().toVector());
        materializePlacements(player.getWorld(), player.getLocation(), context);
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
        log(context, "Root node id=" + root.id + " degree=" + root.requiredDegree());
        Template rootTemplate = selectTemplateForDegree(root.requiredDegree(), Set.of(), random, Collections.emptyList());
        if (rootTemplate == null) {
            log(context, "No root template found for degree " + root.requiredDegree());
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
                log(context, "Missing placement for node " + idA);
                return false;
            }
            log(context, "Expanding node " + idA + " neighbors=" + nodeA.neighbors);
            for (Map.Entry<Direction, Integer> edge : nodeA.neighbors.entrySet()) {
                int idB = edge.getValue();
                String edgeKey = edgeKey(idA, idB);
                if (!visitedEdges.add(edgeKey)) {
                    log(context, "Skip already visited edge " + edgeKey);
                    continue;
                }
                if (context.placements.containsKey(idB)) {
                    log(context, "Neighbor " + idB + " already placed.");
                    continue;
                }
                NodeSpec nodeB = context.byId.get(idB);
                if (nodeB == null) {
                    log(context, "Missing node spec for id " + idB);
                    return false;
                }
                log(context, "Trying place node " + idB + " degree=" + nodeB.requiredDegree() + " from " + idA);

                Placement placed = tryPlaceNode(context, placementA, nodeB, random);
                if (placed == null) {
                    context.logs.add("REJECT node=" + idB + " reason=no_valid_candidate");
                    log(context, "Placement failed for node " + idB + ".");
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
        if (candidates.isEmpty()) {
            log(context, "No candidates for node " + nodeB.id + " degree=" + nodeB.requiredDegree());
        } else {
            log(context, "Candidates for node " + nodeB.id + ": "
                    + candidates.stream().map(Template::id).toList());
        }
        candidates.sort(Comparator.comparing(Template::id));
        for (Template templateB : sortByVariety(candidates, context.useCount, random)) {
            for (StrongholdRotation rotationB : StrongholdRotation.values()) {
                List<Connector> bConnectors = templateB.connectors(rotationB);
                for (Connector aConn : placementA.template.connectors(placementA.transform.rotation)) {
                    for (Connector bConn : bConnectors) {
                        if (!compatible(aConn, bConn)) {
                            continue;
                        }
                        Transform solved = solveTransform(placementA.transform, aConn, bConn, rotationB, ROOM_GAP);
                        Placement placementB = new Placement(templateB, solved, nodeB.id);
                        ValidationResult validation = validatePlacement(context.placements.values(), placementB, DEFAULT_MARGIN);
                        if (validation.valid) {
                            context.logs.add("PAIR A=" + placementA.template.id + ":" + aConn.facing + " B=" + templateB.id + ":" + bConn.facing);
                            log(context, "Accepted placement node=" + nodeB.id + " template=" + templateB.id
                                    + " rotation=" + rotationB + " position=" + solved.position);
                            return placementB;
                        }
                        context.logs.add("REJECT template=" + templateB.id + " rot=" + rotationB + " reason=" + validation.reason);
                        log(context, "Reject template=" + templateB.id + " rot=" + rotationB
                                + " aConn=" + aConn.facing + " bConn=" + bConn.facing
                                + " reason=" + validation.reason);
                    }
                }
            }
        }
        return null;
    }

    private void insertConnectors(GenerationContext context, Random random) {
        Template bridge = activeCatalog().stream().filter(t -> t.tags.contains(StrongholdTemplateTag.CONNECTOR)).findFirst().orElse(null);
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
                Connector bOut = outwardConnectorToward(b, a);
                Placement bridgePlacement = solveBridgePlacement(bridge, a, aOut, b, bOut);
                if (bridgePlacement != null && validatePlacement(context.placements.values(), bridgePlacement, 0).valid) {
                    context.bridges.add(bridgePlacement);
                }
            }
        }
        if (!context.bridges.isEmpty()) {
            context.logs.add("Inserted connector bridges=" + context.bridges.size());
        }
    }

    private ValidationResult validatePlacement(Collection<Placement> existing, Placement candidate, int margin) {
        BoundingBox bounds = candidate.worldBounds();
        plugin.getLogger().info("[Stronghold] Validate candidate template=" + candidate.template.id
                + " bounds=[" + bounds.min + " -> " + bounds.max + "] margin=" + margin);
        for (Placement p : existing) {
            BoundingBox other = p.worldBounds();
            if (!bounds.intersectsExpanded(other, margin)) {
                continue;
            }
            plugin.getLogger().info("[Stronghold] OVERLAP candidate=" + candidate.template.id
                    + " with=" + p.template.id + " candidateBounds=[" + bounds.min + " -> " + bounds.max
                    + "] otherBounds=[" + other.min + " -> " + other.max + "]");
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
        return solveTransform(transformA, aConnWorld, bConnRotated, rotationB, 0);
    }

    public static Transform solveTransform(Transform transformA,
                                           Connector aConnWorld,
                                           Connector bConnRotated,
                                           StrongholdRotation rotationB,
                                           double gap) {
        Vec3 target = transformA.position.add(aConnWorld.localPosition).add(aConnWorld.directionVector().multiply(gap));
        Vec3 bAtOrigin = bConnRotated.localPosition;
        Vec3 translation = target.subtract(bAtOrigin);
        return new Transform(translation, rotationB);
    }

    private Placement solveBridgePlacement(Template bridge,
                                           Placement a,
                                           Connector aOut,
                                           Placement b,
                                           Connector bOut) {
        Placement best = null;
        double bestDistance = Double.MAX_VALUE;
        for (StrongholdRotation rotation : StrongholdRotation.values()) {
            List<Connector> connectors = bridge.connectors(rotation);
            for (Connector in : connectors) {
                if (!compatible(aOut, in)) {
                    continue;
                }
                Transform transform = solveTransform(a.transform, aOut, in, rotation, BRIDGE_GAP);
                Vec3 bTarget = b.transform.position.add(bOut.localPosition);
                for (Connector out : connectors) {
                    if (out == in || !compatible(bOut, out)) {
                        continue;
                    }
                    Vec3 outWorld = transform.position.add(out.localPosition);
                    double distance = outWorld.distanceSquared(bTarget);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = new Placement(bridge, transform, -1);
                    }
                }
            }
        }
        return best;
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
        plugin.getLogger().info("[Stronghold] Summary attempt=" + context.attempt + " seed=" + context.seed
                + " placements=" + context.placements.size() + " bridges=" + context.bridges.size());
        context.logs.forEach(line -> plugin.getLogger().info("[Stronghold] " + line));
    }

    private void logGraph(GenerationContext context) {
        plugin.getLogger().info("[Stronghold] Attempt " + context.attempt + " graph nodes=" + context.nodes.size());
        for (NodeSpec node : context.nodes) {
            plugin.getLogger().info("[Stronghold] Graph node id=" + node.id + " degree=" + node.requiredDegree()
                    + " neighbors=" + node.neighbors);
        }
    }

    private void log(GenerationContext context, String message) {
        String line = "[Stronghold][Attempt " + context.attempt + "] " + message;
        plugin.getLogger().info(line);
        context.logs.add(message);
    }

    private Template selectTemplateForDegree(int degree, Set<StrongholdTemplateTag> requiredTags, Random random, List<String> recent) {
        List<Template> candidates = new ArrayList<>();
        for (Template template : activeCatalog()) {
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
        for (Template template : activeCatalog()) {
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

    public List<TemplateTeleportTarget> getTemplateTeleportTargets(World world) {
        List<TemplateTeleportTarget> out = new ArrayList<>();
        for (TemplateSource source : templateSources.values()) {
            addTarget(out, world, source.id, source.x1, source.y1, source.z1, source.x2, source.y2, source.z2);
        }
        return out;
    }

    private void materializePlacements(World world, Location anchor, GenerationContext context) {
        ensureTemplateCache(world);
        int baseX = anchor.getBlockX();
        int baseY = anchor.getBlockY();
        int baseZ = anchor.getBlockZ();
        for (Placement placement : context.placements.values()) {
            pasteTemplate(world, placement, baseX, baseY, baseZ);
        }
        for (Placement bridge : context.bridges) {
            pasteTemplate(world, bridge, baseX, baseY, baseZ);
        }
    }

    private void ensureTemplateCache(World world) {
        if (!templateCache.isEmpty()) {
            return;
        }
        for (TemplateSource source : templateSources.values()) {
            RoomTemplate captured = RoomTemplate.capture(world,
                    source.x1, source.y1, source.z1,
                    source.x2, source.y2, source.z2,
                    false);
            templateCache.put(source.id, captured);
        }
        rebuildCatalogFromCaptured();
    }

    private void rebuildCatalogFromCaptured() {
        capturedCatalog.clear();
        for (Map.Entry<String, RoomTemplate> entry : templateCache.entrySet()) {
            String id = entry.getKey();
            RoomTemplate captured = entry.getValue();
            Template base = baseCatalogById.get(id);
            if (captured == null || base == null) {
                continue;
            }
            List<Connector> connectors = new ArrayList<>();
            for (RoomTemplate.Connector connector : captured.getConnectors()) {
                connectors.add(new Connector(
                        new Vec3(connector.x, connector.bottomY, connector.z),
                        connector.facing,
                        StrongholdConnectorType.CORRIDOR));
            }
            if (connectors.isEmpty()) {
                continue;
            }
            BoundingBox bounds = new BoundingBox(
                    new Vec3(0, 0, 0),
                    new Vec3(captured.getWidth() - 1, captured.getHeight() - 1, captured.getDepth() - 1));
            capturedCatalog.add(new Template(id, bounds, connectors, base.tags));
        }
        if (!capturedCatalog.isEmpty()) {
            plugin.getLogger().info("[Stronghold] Using redstone-derived connectors from captured templates. count="
                    + capturedCatalog.size());
        }
    }

    private List<Template> activeCatalog() {
        return capturedCatalog.isEmpty() ? catalog : capturedCatalog;
    }

    private void pasteTemplate(World world, Placement placement, int baseX, int baseY, int baseZ) {
        RoomTemplate source = templateCache.get(placement.template.id);
        if (source == null) {
            return;
        }
        int rot = placement.transform.rotation.quarterTurns();
        for (RoomTemplate.BlockDef block : source.getBlocks()) {
            Material material = block.data.getMaterial();
            if (shouldIgnoreMaterial(material)) {
                continue;
            }
            Vec3 rotated = new Vec3(block.x, block.y, block.z).rotateY(placement.transform.rotation);
            int wx = baseX + (int) Math.round(placement.transform.position.x + rotated.x);
            int wy = baseY + (int) Math.round(placement.transform.position.y + rotated.y);
            int wz = baseZ + (int) Math.round(placement.transform.position.z + rotated.z);
            world.getBlockAt(wx, wy, wz).setBlockData(RoomTemplate.rotateBlockData(block.data, rot), false);
        }
    }

    private boolean shouldIgnoreMaterial(Material material) {
        return material == Material.AIR
                || material == Material.WHITE_CONCRETE
                || material == Material.LIGHT_BLUE_CONCRETE
                || material == Material.REDSTONE_BLOCK;
    }

    private Map<String, TemplateSource> buildTemplateSources() {
        Map<String, TemplateSource> out = new LinkedHashMap<>();
        putSource(out, "corner_1", 473, -38, -5346, 543, -61, -5276);
        putSource(out, "corner_2", 544, -38, -5631, 614, -61, -5701);
        putSource(out, "corner_3", 614, -61, -5630, 544, -38, -5560);
        putSource(out, "straight_1", 402, -38, -5276, 472, -61, -5346);
        putSource(out, "straight_2", 472, -61, -5347, 402, -38, -5417);
        putSource(out, "straight_3", 402, -38, -5418, 472, -61, -5488);
        putSource(out, "straight_4", 472, -61, -5489, 402, -38, -5559);
        putSource(out, "straight_5", 402, -38, -5560, 472, -61, -5630);
        putSource(out, "straight_6", 472, -61, -5631, 402, -38, -5701);
        putSource(out, "straight_7", 473, -38, -5701, 543, -61, -5631);
        putSource(out, "straight_8", 543, -61, -5630, 473, -38, -5560);
        putSource(out, "straight_9", 473, -38, -5417, 543, -61, -5347);
        putSource(out, "deadend_1", 543, -38, -5418, 473, -61, -5488);
        putSource(out, "deadend_2", 473, -61, -5489, 543, -38, -5559);
        putSource(out, "connector_1", 412, -61, -5711, 402, -38, -5701);
        putSource(out, "connector_2", 402, -38, -5721, 412, -61, -5711);
        putSource(out, "tower_1", 615, -61, -5488, 685, -7, -5418);
        putSource(out, "tower_2", 615, -61, -5276, 685, -7, -5206);
        putSource(out, "gate_1", 686, -61, -5346, 614, -10, -5418);
        putSource(out, "gate_2", 686, -61, -5276, 614, -10, -5346);
        return out;
    }

    private void putSource(Map<String, TemplateSource> out, String id,
                           int x1, int y1, int z1, int x2, int y2, int z2) {
        out.put(id, new TemplateSource(id, x1, y1, z1, x2, y2, z2));
    }

    private void addTarget(List<TemplateTeleportTarget> out, World world, String id,
                           int x1, int y1, int z1, int x2, int y2, int z2) {
        double x = (Math.min(x1, x2) + Math.max(x1, x2)) / 2.0;
        double y = Math.max(y1, y2) + 2.0;
        double z = (Math.min(z1, z2) + Math.max(z1, z2)) / 2.0;
        out.add(new TemplateTeleportTarget(id, new Location(world, x, y, z)));
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

    public record TemplateTeleportTarget(String id, Location teleportLocation) {
    }

    private record TemplateSource(String id, int x1, int y1, int z1, int x2, int y2, int z2) {
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
            // Treat face-to-face contact as valid adjacency, not overlap.
            return aMin < bMax && bMin < aMax;
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

        public double distanceSquared(Vec3 other) {
            double dx = x - other.x;
            double dy = y - other.y;
            double dz = z - other.z;
            return dx * dx + dy * dy + dz * dz;
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
