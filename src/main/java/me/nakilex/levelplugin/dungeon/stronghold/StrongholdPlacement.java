package me.nakilex.levelplugin.dungeon.stronghold;

import me.nakilex.levelplugin.dungeon.Direction;
import me.nakilex.levelplugin.dungeon.stronghold.Geometry.BoundingBox;
import me.nakilex.levelplugin.dungeon.stronghold.StrongholdEnums.Rotation;
import me.nakilex.levelplugin.dungeon.stronghold.StrongholdEnums.TemplateTag;
import me.nakilex.levelplugin.dungeon.stronghold.StrongholdModel.Connector;
import me.nakilex.levelplugin.dungeon.stronghold.StrongholdModel.Template;
import me.nakilex.levelplugin.dungeon.stronghold.StrongholdModel.Transform;

import java.util.*;
import java.util.logging.Logger;

/** Template selection, validation, and deterministic graph expansion pipeline. */
public final class StrongholdPlacement {
    private StrongholdPlacement() {}

    public record PlacementConfig(int spacingMargin, boolean voxelCheckEnabled) {
        public PlacementConfig {
            if (spacingMargin < 0) {
                throw new IllegalArgumentException("spacingMargin must be >= 0");
            }
        }
    }

    public record PlacedRoom(int nodeId, Template template, Transform transform) {
        public BoundingBox worldBounds() {
            return template.rotatedBounds(transform.rotation()).translate(transform.position());
        }
    }

    public record PlacementResult(boolean success, Map<Integer, PlacedRoom> rooms, List<String> logs) {}

    public static final class OverlapValidator {
        private final PlacementConfig config;

        public OverlapValidator(PlacementConfig config) {
            this.config = Objects.requireNonNull(config, "config");
        }

        public Optional<String> validate(PlacedRoom candidate, Collection<PlacedRoom> existing) {
            BoundingBox candidateBox = candidate.worldBounds();
            for (PlacedRoom placed : existing) {
                BoundingBox other = placed.worldBounds();
                if (!candidateBox.intersects(other)) {
                    continue;
                }

                // Stage 1: fast AABB rejection.
                BoundingBox candidateConnectorVolume = connectorEnvelope(candidate);
                BoundingBox otherConnectorVolume = connectorEnvelope(placed);
                if (candidateConnectorVolume.intersects(otherConnectorVolume)) {
                    continue;
                }

                // Stage 2: margin check.
                if (candidateBox.expand(config.spacingMargin()).intersects(other.expand(config.spacingMargin()))) {
                    return Optional.of("Margin overlap against node " + placed.nodeId());
                }

                // Stage 3: optional voxel-level guard (deferred here for performance).
                if (config.voxelCheckEnabled()) {
                    return Optional.of("Voxel-check required (not implemented) against node " + placed.nodeId());
                }
            }
            return Optional.empty();
        }

        private BoundingBox connectorEnvelope(PlacedRoom room) {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (Connector c : room.template().connectors()) {
                Connector rotated = c.rotated(room.transform().rotation());
                var world = room.transform().position().add(rotated.localPosition());
                minX = Math.min(minX, world.x());
                minY = Math.min(minY, world.y());
                minZ = Math.min(minZ, world.z());
                maxX = Math.max(maxX, world.x());
                maxY = Math.max(maxY, world.y());
                maxZ = Math.max(maxZ, world.z());
            }
            if (minX == Integer.MAX_VALUE) {
                return room.worldBounds();
            }
            return new BoundingBox(
                    new Geometry.Vec3(minX, minY, minZ),
                    new Geometry.Vec3(maxX, maxY, maxZ)
            );
        }
    }

    public static PlacementResult placeGraph(StrongholdGraph.Graph graph,
                                             List<Template> catalog,
                                             long seed,
                                             PlacementConfig config,
                                             Logger logger) {
        Random random = new Random(seed);
        List<String> logs = new ArrayList<>();
        if (graph.nodes().isEmpty()) {
            return new PlacementResult(true, Map.of(), logs);
        }

        OverlapValidator validator = new OverlapValidator(config);
        Map<Integer, PlacedRoom> placed = new LinkedHashMap<>();
        Queue<StrongholdGraph.Edge> queue = new ArrayDeque<>(graph.edges());

        StrongholdGraph.Node root = graph.nodes().get(0);
        Template rootTemplate = selectTemplate(root, catalog, random, Set.of(), Map.of());
        if (rootTemplate == null) {
            logs.add("No template matches root node degree=" + root.requiredDegree());
            return new PlacementResult(false, Map.of(), logs);
        }

        placed.put(root.id(), new PlacedRoom(root.id(), rootTemplate, new Transform(new Geometry.Vec3(0, 0, 0), Rotation.R0)));
        logs.add("Placed root node " + root.id() + " template=" + rootTemplate.id());

        int stalled = 0;
        while (!queue.isEmpty() && stalled <= queue.size()) {
            StrongholdGraph.Edge edge = queue.remove();
            PlacedRoom from = placed.get(edge.fromNodeId());
            PlacedRoom to = placed.get(edge.toNodeId());
            if (from == null && to == null) {
                queue.add(edge);
                stalled++;
                continue;
            }
            if (from != null && to != null) {
                continue;
            }

            int targetId = from == null ? edge.fromNodeId() : edge.toNodeId();
            PlacedRoom anchor = from == null ? to : from;
            StrongholdGraph.Node target = graph.nodes().stream().filter(n -> n.id() == targetId).findFirst().orElse(null);
            if (target == null) {
                logs.add("Missing graph node " + targetId);
                return new PlacementResult(false, Map.of(), logs);
            }

            PlacedRoom candidate = tryPlaceNode(target, anchor, catalog, validator, placed, random, logs);
            if (candidate == null) {
                logs.add("Failed placement for node " + targetId + ", regenerate graph required");
                return new PlacementResult(false, Map.of(), logs);
            }
            placed.put(targetId, candidate);
            stalled = 0;
        }

        if (!queue.isEmpty()) {
            logs.add("Graph expansion stalled before processing all edges");
            return new PlacementResult(false, Map.of(), logs);
        }

        if (logger != null) {
            logs.forEach(logger::info);
        }
        return new PlacementResult(true, Collections.unmodifiableMap(placed), List.copyOf(logs));
    }

    private static PlacedRoom tryPlaceNode(StrongholdGraph.Node target,
                                           PlacedRoom anchor,
                                           List<Template> catalog,
                                           OverlapValidator validator,
                                           Map<Integer, PlacedRoom> placed,
                                           Random random,
                                           List<String> logs) {
        Template chosen = selectTemplate(target, catalog, random, Set.of(), Map.of());
        if (chosen == null) {
            logs.add("No template candidates for degree=" + target.requiredDegree());
            return null;
        }

        for (Connector anchorConn : anchor.template().connectors()) {
            Direction outgoing = anchor.transform().rotation().rotate(anchorConn.facing());
            for (Rotation rot : Rotation.values()) {
                for (Connector targetConn : chosen.connectors()) {
                    Direction incoming = rot.rotate(targetConn.facing());
                    if (outgoing != incoming.opposite()) {
                        continue;
                    }
                    try {
                        Transform transform = TransformSolver.solveTransform(
                                anchor.template(), anchor.transform(), anchorConn,
                                chosen, rot, targetConn
                        );
                        PlacedRoom candidate = new PlacedRoom(target.id(), chosen, transform);
                        Optional<String> reason = validator.validate(candidate, placed.values());
                        if (reason.isPresent()) {
                            logs.add("Rejected node " + target.id() + " template=" + chosen.id()
                                    + " rot=" + rot + " reason=" + reason.get());
                            continue;
                        }
                        logs.add("Placed node " + target.id() + " template=" + chosen.id()
                                + " rot=" + rot + " pair=" + anchorConn.facing() + "<->" + targetConn.facing());
                        return candidate;
                    } catch (IllegalArgumentException ex) {
                        logs.add("Rejected node " + target.id() + " template=" + chosen.id() + " reason=" + ex.getMessage());
                    }
                }
            }
        }
        return null;
    }

    private static Template selectTemplate(StrongholdGraph.Node node,
                                           List<Template> catalog,
                                           Random random,
                                           Set<TemplateTag> requiredTags,
                                           Map<String, Integer> recentUsage) {
        List<Template> candidates = new ArrayList<>();
        for (Template template : catalog) {
            if (template.degree() != node.requiredDegree()) {
                continue;
            }
            if (!template.tags().containsAll(requiredTags)) {
                continue;
            }
            candidates.add(template);
        }
        if (candidates.isEmpty()) {
            return null;
        }

        candidates.sort(Comparator.comparing(Template::id));
        int totalWeight = 0;
        int[] weights = new int[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            Template t = candidates.get(i);
            int penalty = recentUsage.getOrDefault(t.id(), 0);
            int weight = Math.max(1, 10 - penalty);
            weights[i] = weight;
            totalWeight += weight;
        }

        int roll = random.nextInt(totalWeight);
        int cursor = 0;
        for (int i = 0; i < candidates.size(); i++) {
            cursor += weights[i];
            if (roll < cursor) {
                return candidates.get(i);
            }
        }
        return candidates.get(0);
    }
}
