package me.nakilex.levelplugin.debug;

import com.sk89q.worldedit.math.BlockVector3;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.World.Environment;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Debug-only stronghold composer.
 */
public final class StrongholdDebugGenerator {
    private static final Set<Material> EXCLUDED = Set.of(
            Material.REDSTONE_BLOCK,
            Material.LIGHT_BLUE_CONCRETE,
            Material.WHITE_CONCRETE
    );

    private static final List<TemplateSpec> TEMPLATE_SPECS = List.of(
            new TemplateSpec("corner_1", new TemplateBounds(473, -38, -5346, 543, -61, -5276), PieceCategory.WALL, 1),
            new TemplateSpec("corner_2", new TemplateBounds(544, -38, -5631, 614, -61, -5701), PieceCategory.WALL, 1),
            new TemplateSpec("corner_3", new TemplateBounds(614, -61, -5630, 544, -38, -5560), PieceCategory.WALL, 1),
            new TemplateSpec("straight_1", new TemplateBounds(402, -38, -5276, 472, -61, -5346), PieceCategory.WALL, 2),
            new TemplateSpec("straight_2", new TemplateBounds(472, -61, -5347, 402, -38, -5417), PieceCategory.WALL, 2),
            new TemplateSpec("straight_3", new TemplateBounds(402, -38, -5418, 472, -61, -5488), PieceCategory.WALL, 2),
            new TemplateSpec("straight_4", new TemplateBounds(472, -61, -5489, 402, -38, -5559), PieceCategory.WALL, 2),
            new TemplateSpec("straight_5", new TemplateBounds(402, -38, -5560, 472, -61, -5630), PieceCategory.WALL, 2),
            new TemplateSpec("straight_6", new TemplateBounds(472, -61, -5631, 402, -38, -5701), PieceCategory.WALL, 2),
            new TemplateSpec("straight_7", new TemplateBounds(473, -38, -5701, 543, -61, -5631), PieceCategory.WALL, 2),
            new TemplateSpec("straight_8", new TemplateBounds(543, -61, -5630, 473, -38, -5560), PieceCategory.WALL, 2),
            new TemplateSpec("straight_9", new TemplateBounds(473, -38, -5417, 543, -61, -5347), PieceCategory.WALL, 2),
            new TemplateSpec("t_section", new TemplateBounds(615, -61, -5276, 685, -7, -5206), PieceCategory.JUNCTION_LARGE, 3),
            new TemplateSpec("tower_1", new TemplateBounds(615, -61, -5488, 685, -7, -5418), PieceCategory.JUNCTION_LARGE, 3),
            new TemplateSpec("gate_1", new TemplateBounds(686, -61, -5346, 614, -10, -5418), PieceCategory.JUNCTION_LARGE, 1),
            new TemplateSpec("gate_2", new TemplateBounds(686, -61, -5276, 614, -10, -5346), PieceCategory.JUNCTION_LARGE, 1),
            new TemplateSpec("church", new TemplateBounds(757, -61, -5559, 827, 34, -5489), PieceCategory.JUNCTION_LARGE, 1),
            new TemplateSpec("deadend_1", new TemplateBounds(543, -38, -5418, 473, -61, -5488), PieceCategory.DEAD_END, 1),
            new TemplateSpec("deadend_2", new TemplateBounds(473, -61, -5489, 543, -38, -5559), PieceCategory.DEAD_END, 1)
    );

    private static final TemplateSpec CONNECTOR_SPEC =
            new TemplateSpec("connector_1", new TemplateBounds(412, -61, -5711, 402, -38, -5701), PieceCategory.CONNECTOR, 1);

    private static final String SOURCE_WORLD = "flatland";
    private static final String GENERATED_WORLD_PREFIX = "stronghold_debug_";

    private static final int DEFAULT_SPINE_LENGTH = 20;
    private static final int MAX_BRANCH_LENGTH = 10;
    private static final int MAX_TOTAL_PIECES = 96;
    private static final int MIN_WALL_PIECES_BETWEEN_LARGE = 1;
    private static final int MIN_BLOCKS_BETWEEN_LARGE = 24;
    private static final int MIN_WALL_PIECES_BETWEEN_GATES = 3;
    private static final int MAX_CONNECTOR_DRIFT_BLOCKS = 6;
    private static final int MAX_LARGE_CONNECTOR_DRIFT_BLOCKS = 0;
    private static final double BRANCH_OPEN_SIDE_CHANCE = 0.90D;

    private static double maxOverlapPercent = 2.0D;

    private StrongholdDebugGenerator() {
    }

    public static double getMaxOverlapPercent() {
        return maxOverlapPercent;
    }

    public static void setMaxOverlapPercent(double value) {
        maxOverlapPercent = Math.max(0.0D, Math.min(100.0D, value));
    }

    public static Map<String, TemplateConnectionInfo> inspectTemplateConnections() {
        Main plugin = Main.getInstance();
        if (plugin != null && plugin.getWorldManager() != null) {
            plugin.getWorldManager().ensureWorldsLoaded(SOURCE_WORLD);
        }
        World sourceWorld = Bukkit.getWorld(SOURCE_WORLD);
        if (sourceWorld == null) {
            return Map.of();
        }

        loadSourceChunks(sourceWorld);
        CapturedTemplates captured = captureAllTemplates(sourceWorld);
        if (captured == null) {
            return Map.of();
        }

        Map<String, TemplateConnectionInfo> out = new LinkedHashMap<>();
        List<TemplateSpec> all = new ArrayList<>();
        all.addAll(captured.walls());
        all.addAll(captured.largeJunctions());
        all.addAll(captured.deadEnds());
        all.add(captured.connector());
        for (TemplateSpec spec : all) {
            List<BlockFace> sides = new ArrayList<>(spec.template.connectors.keySet());
            int connectorCount = spec.template.connectors.values().stream().mapToInt(List::size).sum();
            out.put(spec.id, new TemplateConnectionInfo(connectorCount, sides));
        }
        return out;
    }

    public static int cleanupGeneratedWorlds(Main plugin) {
        if (plugin == null || plugin.getWorldManager() == null) {
            return 0;
        }
        return plugin.getWorldManager().deleteWorldsByPrefix(GENERATED_WORLD_PREFIX);
    }

    public static boolean generateTest(Player player) {
        if (player == null) {
            return false;
        }

        Main plugin = Main.getInstance();
        if (plugin == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Plugin bootstrap is unavailable. Try again after startup completes.");
            return true;
        }

        plugin.getWorldManager().ensureWorldsLoaded(SOURCE_WORLD);
        World sourceWorld = Bukkit.getWorld(SOURCE_WORLD);
        if (sourceWorld == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Source template world '" + SOURCE_WORLD + "' is not loaded.");
            return true;
        }

        World world = createGeneratedWorld(plugin, player);
        if (world == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Failed to create a superflat world for stronghold generation.");
            return true;
        }

        loadSourceChunks(sourceWorld);
        Random random = ThreadLocalRandom.current();

        CapturedTemplates captured = captureAllTemplates(sourceWorld);
        if (captured == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Failed to capture one or more stronghold templates. Check source cuboids and markers.");
            return true;
        }
        GenerationDiagnostics diagnostics = new GenerationDiagnostics();
        diagnostics.templateConnectorSummary = templateConnectorSummary(captured);

        int originX = 0;
        int originZ = 0;
        world.getChunkAt(Math.floorDiv(originX, 16), Math.floorDiv(originZ, 16)).load(true);
        int originY = -61;

        List<PlacedTemplate> placed = new ArrayList<>();
        Set<Long> occupied = new HashSet<>();

        TemplateSpec startSpec = pickWeighted(captured.walls(), random);
        if (startSpec == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "No wall templates were captured.");
            return true;
        }

        PlacedTemplate start = new PlacedTemplate(startSpec, 0, BlockVector3.at(originX, originY, originZ));
        placed.add(start);
        occupy(occupied, start);

        PlacedTemplate spineHead = start;
        PlacementState spineState = PlacementState.initial();
        for (int i = 0; i < DEFAULT_SPINE_LENGTH; i++) {
            List<TemplateSpec> pool = candidatePoolForStep(captured, spineHead, spineState);

            if (pool.isEmpty()) {
                break;
            }

            ExpansionChoice choice = pickBestExpansion(spineHead, pool, captured, occupied, placed, spineState, random);
            if (choice == null) {
                break;
            }

            BlockFace side = choice.side();
            PlacementAttempt attempt = choice.attempt();
            if (attempt == null) {
                diagnostics.spineBlockedSides++;
                spineHead.markUsed(side);
                continue;
            }

            if (attempt.connector != null) {
                attempt.connector.markUsed(side);
                spineState = spineState.onPlaced(attempt.connector.spec);
                placed.add(attempt.connector);
                occupy(occupied, attempt.connector);
            }
            spineHead.markUsed(side);
            spineState = spineState.onPlaced(attempt.placed.spec);
            placed.add(attempt.placed);
            occupy(occupied, attempt.placed);
            spineHead = attempt.placed;
        }

        closeOpenSideWithDeadEnd(spineHead, captured, occupied, random, placed);

        for (int seedIndex = 0; seedIndex < placed.size() && placed.size() < MAX_TOTAL_PIECES; seedIndex++) {
            growBranches(placed.get(seedIndex), captured, occupied, random, placed, MAX_TOTAL_PIECES, diagnostics);
        }

        ensureTargetChunksLoaded(world, placed);
        for (PlacedTemplate entry : placed) {
            paste(world, entry.spec.template, entry.origin, entry.rotation);
        }

        player.teleport(new org.bukkit.Location(world, originX + 0.5, originY + 2, originZ + 0.5));
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Generated stronghold spine+branches using " + placed.size()
                        + " pieces in world '" + world.getName() + "' (overlap threshold: "
                        + String.format("%.2f", maxOverlapPercent) + "%).");
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Stronghold diagnostics -> spine blocked sides: " + diagnostics.spineBlockedSides
                        + ", branch blocked sides: " + diagnostics.branchBlockedSides
                        + ", remaining open outputs: " + countOpenOutputs(placed)
                        + ", viable next outputs: " + countViableOpenOutputs(placed, captured, occupied)
                        + ", connectors: " + diagnostics.templateConnectorSummary);
        return true;
    }


    private static World createGeneratedWorld(Main plugin, Player player) {
        String worldName = GENERATED_WORLD_PREFIX + System.currentTimeMillis();
        World world = plugin.getWorldManager().createWorld(worldName, WorldType.FLAT, Environment.NORMAL, false);
        if (world == null) {
            return null;
        }
        world.setKeepSpawnInMemory(false);
        world.setAutoSave(false);
        world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Generating stronghold in superflat world '" + world.getName() + "'...");
        return world;
    }

    private static void loadSourceChunks(World sourceWorld) {
        for (TemplateSpec spec : TEMPLATE_SPECS) {
            loadChunksForBounds(sourceWorld, spec.bounds);
        }
        loadChunksForBounds(sourceWorld, CONNECTOR_SPEC.bounds);
    }

    private static void loadChunksForBounds(World world, TemplateBounds bounds) {
        int minX = Math.min(bounds.minX, bounds.maxX);
        int maxX = Math.max(bounds.minX, bounds.maxX);
        int minZ = Math.min(bounds.minZ, bounds.maxZ);
        int maxZ = Math.max(bounds.minZ, bounds.maxZ);
        int minChunkX = Math.floorDiv(minX, 16);
        int maxChunkX = Math.floorDiv(maxX, 16);
        int minChunkZ = Math.floorDiv(minZ, 16);
        int maxChunkZ = Math.floorDiv(maxZ, 16);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                world.getChunkAt(chunkX, chunkZ).load(true);
            }
        }
    }

    private static void ensureTargetChunksLoaded(World world, List<PlacedTemplate> placedTemplates) {
        Set<Long> loadedChunks = new HashSet<>();
        for (PlacedTemplate placedTemplate : placedTemplates) {
            RotatedTemplate rotatedTemplate = rotateTemplate(placedTemplate.spec.template, placedTemplate.rotation);
            for (BlockVector3 rel : rotatedTemplate.blocks.keySet()) {
                int x = placedTemplate.origin.getBlockX() + rel.getBlockX();
                int z = placedTemplate.origin.getBlockZ() + rel.getBlockZ();
                int chunkX = Math.floorDiv(x, 16);
                int chunkZ = Math.floorDiv(z, 16);
                long key = (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
                if (loadedChunks.add(key)) {
                    world.getChunkAt(chunkX, chunkZ).load(true);
                }
            }
        }
    }
    private static void growBranches(PlacedTemplate seed,
                                     CapturedTemplates captured,
                                     Set<Long> occupied,
                                     Random random,
                                     List<PlacedTemplate> placed,
                                     int maxPieces,
                                     GenerationDiagnostics diagnostics) {
        if (placed.size() >= maxPieces) {
            return;
        }
        List<BlockFace> openSides = seed.openSides();
        Collections.shuffle(openSides, random);

        for (BlockFace side : openSides) {
            if (placed.size() >= maxPieces) {
                return;
            }
            if (random.nextDouble() > BRANCH_OPEN_SIDE_CHANCE) {
                continue;
            }

            PlacedTemplate branchCurrent = seed;
            BlockFace branchSide = side;
            PlacementState branchState = PlacementState.fromSeed(seed.spec);

            int remaining = Math.max(1, maxPieces - placed.size());
            int maxSegments = Math.min(MAX_BRANCH_LENGTH, remaining);
            int segments = 1 + random.nextInt(maxSegments);
            for (int i = 0; i < segments; i++) {
                if (placed.size() >= maxPieces) {
                    return;
                }
                List<TemplateSpec> pool = candidatePoolForStep(captured, branchCurrent, branchState, i > 0);

                PlacementAttempt attempt = tryPlaceFromSide(branchCurrent, branchSide, pool, captured.connector(), occupied, placed, branchState, captured, random);
                if (attempt == null) {
                    diagnostics.branchBlockedSides++;
                    branchCurrent.markUsed(branchSide);
                    break;
                }

                if (attempt.connector != null) {
                    attempt.connector.markUsed(branchSide);
                    branchState = branchState.onPlaced(attempt.connector.spec);
                    placed.add(attempt.connector);
                    occupy(occupied, attempt.connector);
                }
                branchCurrent.markUsed(branchSide);
                branchState = branchState.onPlaced(attempt.placed.spec);
                placed.add(attempt.placed);
                occupy(occupied, attempt.placed);
                branchCurrent = attempt.placed;

                BlockFace avoid = attempt.placed.incomingSide;
                branchSide = pickOpenSide(attempt.placed, avoid, random);
                if (branchSide == null) {
                    break;
                }
            }

            closeOpenSideWithDeadEnd(branchCurrent, captured, occupied, random, placed);
        }
    }

    private static void closeOpenSideWithDeadEnd(PlacedTemplate target,
                                                  CapturedTemplates captured,
                                                  Set<Long> occupied,
                                                  Random random,
                                                  List<PlacedTemplate> placed) {
        if (captured.deadEnds().isEmpty()) {
            return;
        }
        BlockFace side = pickOpenSide(target, null, random);
        if (side == null) {
            return;
        }

        PlacementAttempt deadEndAttempt = tryPlaceFromSide(
                target,
                side,
                captured.deadEnds(),
                null,
                occupied,
                placed,
                PlacementState.fromSeed(target.spec),
                null,
                random
        );
        if (deadEndAttempt == null) {
            target.markUsed(side);
            return;
        }

        target.markUsed(side);
        placed.add(deadEndAttempt.placed);
        occupy(occupied, deadEndAttempt.placed);
    }

    private static PlacementAttempt tryPlaceFromSide(PlacedTemplate current,
                                                     BlockFace currentSide,
                                                     List<TemplateSpec> candidateSpecs,
                                                     TemplateSpec connector,
                                                     Set<Long> occupied,
                                                     List<PlacedTemplate> placedTemplates,
                                                     PlacementState state,
                                                     CapturedTemplates captured,
                                                     Random random) {
        if (candidateSpecs == null || candidateSpecs.isEmpty()) {
            return null;
        }

        List<PlacementAttempt> attempts = enumeratePlacementAttempts(
                current,
                currentSide,
                weightedShuffle(candidateSpecs, random),
                connector,
                occupied
        );
        if (attempts.isEmpty()) {
            return null;
        }
        return pickBestAttempt(current, attempts, occupied, placedTemplates, state, captured);
    }

    private static ExpansionChoice pickBestExpansion(PlacedTemplate current,
                                                     List<TemplateSpec> candidateSpecs,
                                                     CapturedTemplates captured,
                                                     Set<Long> occupied,
                                                     List<PlacedTemplate> placedTemplates,
                                                     PlacementState state,
                                                     Random random) {
        List<BlockFace> openSides = current.openSides();
        Collections.shuffle(openSides, random);

        ExpansionChoice best = null;
        for (BlockFace side : openSides) {
            PlacementAttempt attempt = tryPlaceFromSide(
                    current,
                    side,
                    candidateSpecs,
                    captured.connector(),
                    occupied,
                    placedTemplates,
                    state,
                    captured,
                    random
            );
            if (attempt == null) {
                continue;
            }
            double score = scoreAttempt(current, attempt, occupied, state, captured);
            if (best == null || score > best.score()) {
                best = new ExpansionChoice(side, attempt, score);
            }
        }
        return best;
    }

    private static List<PlacementAttempt> enumeratePlacementAttempts(PlacedTemplate current,
                                                                     BlockFace currentSide,
                                                                     List<TemplateSpec> candidateSpecs,
                                                                     TemplateSpec connector,
                                                                     Set<Long> occupied) {
        List<PlacementAttempt> attempts = new ArrayList<>();

        if (connector != null) {
            PlacedTemplate connectorPlaced = tryPlaceSingle(current, currentSide, List.of(connector), occupied, true);
            if (connectorPlaced != null) {
                Set<Long> occupiedWithConnector = new HashSet<>(occupied);
                occupy(occupiedWithConnector, connectorPlaced);
                PlacedTemplate viaConnector = tryPlaceSingle(connectorPlaced, currentSide, candidateSpecs, occupiedWithConnector, true);
                if (viaConnector != null && !areBothLarge(current.spec, viaConnector.spec)) {
                    attempts.add(new PlacementAttempt(connectorPlaced, viaConnector));
                }
            }
        }

        PlacedTemplate direct = tryPlaceSingle(current, currentSide, candidateSpecs, occupied, true);
        if (direct != null && !areBothLarge(current.spec, direct.spec)) {
            attempts.add(new PlacementAttempt(null, direct));
        }
        return attempts;
    }

    private static PlacementAttempt pickBestAttempt(PlacedTemplate current,
                                                    List<PlacementAttempt> attempts,
                                                    Set<Long> occupied,
                                                    List<PlacedTemplate> placedTemplates,
                                                    PlacementState state,
                                                    CapturedTemplates captured) {
        PlacementAttempt best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (PlacementAttempt attempt : attempts) {
            if (!satisfiesPlacementRules(attempt, state, placedTemplates)) {
                continue;
            }
            double score = scoreAttempt(current, attempt, occupied, state, captured);
            if (score > bestScore) {
                bestScore = score;
                best = attempt;
            }
        }
        return best;
    }

    private static double scoreAttempt(PlacedTemplate current,
                                       PlacementAttempt attempt,
                                       Set<Long> occupied,
                                       PlacementState state,
                                       CapturedTemplates captured) {
        RotatedTemplate rotated = rotateTemplate(attempt.placed.spec.template, attempt.placed.rotation);
        double overlap = overlapPercent(occupied, rotated.blocks, attempt.placed.origin);

        Set<Long> occupiedAfter = new HashSet<>(occupied);
        if (attempt.connector != null) {
            occupy(occupiedAfter, attempt.connector);
        }
        occupy(occupiedAfter, attempt.placed);

        PlacementState nextState = state;
        if (attempt.connector != null) {
            nextState = nextState.onPlaced(attempt.connector.spec);
        }
        nextState = nextState.onPlaced(attempt.placed.spec);

        int openOutputs = attempt.placed.openSides().size();
        int viableNextSteps = 0;
        if (captured != null) {
            List<TemplateSpec> nextPool = candidatePoolForStep(captured, attempt.placed, nextState);
            for (BlockFace side : attempt.placed.openSides()) {
                if (!enumeratePlacementAttempts(attempt.placed, side, nextPool, captured.connector(), occupiedAfter).isEmpty()) {
                    viableNextSteps++;
                }
            }
        }

        int branchBonus = openOutputs >= 2 ? 1 : 0;
        int junctionBonus = isLarge(attempt.placed.spec) ? 1 : 0;
        int continuationBonus = !areBothLarge(current.spec, attempt.placed.spec) ? 1 : 0;

        return (viableNextSteps * 100.0D)
                + (openOutputs * 15.0D)
                + (branchBonus * 20.0D)
                + (junctionBonus * 12.0D)
                + (continuationBonus * 6.0D)
                - overlap;
    }

    private static int countOpenOutputs(List<PlacedTemplate> placed) {
        int total = 0;
        for (PlacedTemplate entry : placed) {
            total += entry.openSides().size();
        }
        return total;
    }

    private static int countViableOpenOutputs(List<PlacedTemplate> placed,
                                              CapturedTemplates captured,
                                              Set<Long> occupied) {
        int viable = 0;
        for (PlacedTemplate entry : placed) {
            PlacementState state = PlacementState.fromSeed(entry.spec);
            List<TemplateSpec> pool = candidatePoolForStep(captured, entry, state);
            for (BlockFace side : entry.openSides()) {
                if (!enumeratePlacementAttempts(entry, side, pool, captured.connector(), occupied).isEmpty()) {
                    viable++;
                }
            }
        }
        return viable;
    }

    private static String templateConnectorSummary(CapturedTemplates captured) {
        List<String> entries = new ArrayList<>();
        List<TemplateSpec> all = new ArrayList<>();
        all.addAll(captured.walls());
        all.addAll(captured.largeJunctions());
        all.addAll(captured.deadEnds());
        for (TemplateSpec spec : all) {
            int connectorCount = spec.template.connectors.values().stream().mapToInt(List::size).sum();
            entries.add(spec.id + ":" + connectorCount);
        }
        return String.join(", ", entries);
    }

    private static PlacedTemplate tryPlaceSingle(PlacedTemplate current,
                                                 BlockFace currentSide,
                                                 List<TemplateSpec> candidateSpecs,
                                                 Set<Long> occupied,
                                                 boolean enforceOverlap) {
        RotatedTemplate currentRotated = rotateTemplate(current.spec.template, current.rotation);
        List<BlockVector3> currentConnectors = currentRotated.connectors.get(currentSide);
        if (currentConnectors == null || currentConnectors.isEmpty()) {
            return null;
        }

        for (TemplateSpec spec : candidateSpecs) {
            for (int rot = 0; rot < 4; rot++) {
                RotatedTemplate rotated = rotateTemplate(spec.template, rot);
                List<BlockVector3> candidateConnectors = rotated.connectors.get(opposite(currentSide));
                if (candidateConnectors == null || candidateConnectors.isEmpty()) {
                    continue;
                }
                for (BlockVector3 currentConnector : currentConnectors) {
                    BlockVector3 worldConnector = current.origin.add(currentConnector);
                    for (BlockVector3 candidateConnector : candidateConnectors) {
                        BlockVector3 idealOrigin = worldConnector.subtract(candidateConnector);
                        BlockVector3 origin = idealOrigin;
                        origin = slideUntilThreshold(occupied, rotated.blocks, origin, currentSide);
                        if (!connectorDriftWithinLimit(idealOrigin, origin, spec)) {
                            continue;
                        }
                        if (enforceOverlap && overlapPercent(occupied, rotated.blocks, origin) > maxOverlapPercent) {
                            continue;
                        }
                        PlacedTemplate placed = new PlacedTemplate(spec, rot, origin);
                        placed.incomingSide = opposite(currentSide);
                        placed.markUsed(opposite(currentSide));
                        return placed;
                    }
                }
            }
        }

        return null;
    }

    private static boolean areBothLarge(TemplateSpec a, TemplateSpec b) {
        return isLarge(a) && isLarge(b);
    }

    private static boolean isLarge(TemplateSpec spec) {
        return spec.category == PieceCategory.JUNCTION_LARGE;
    }

    private static boolean isGate(TemplateSpec spec) {
        return spec.id.startsWith("gate_");
    }

    private static boolean isWall(TemplateSpec spec) {
        return spec.category == PieceCategory.WALL;
    }

    private static boolean canPlaceLargeAfter(PlacedTemplate current, PlacementState state) {
        return !isLarge(current.spec) && state.wallPiecesSinceLarge >= MIN_WALL_PIECES_BETWEEN_LARGE;
    }

    private static boolean canPlaceGate(PlacementState state) {
        return state.wallPiecesSinceGate >= MIN_WALL_PIECES_BETWEEN_GATES;
    }

    private static boolean canUseSpec(TemplateSpec spec, PlacementState state) {
        return !isGate(spec) || canPlaceGate(state);
    }

    private static boolean satisfiesPlacementRules(PlacementAttempt attempt,
                                                   PlacementState state,
                                                   List<PlacedTemplate> placedTemplates) {
        if (attempt == null || attempt.placed == null) {
            return false;
        }
        if (!isLarge(attempt.placed.spec)) {
            return true;
        }
        if (state.wallPiecesSinceLarge < MIN_WALL_PIECES_BETWEEN_LARGE) {
            return false;
        }
        return hasLargeTemplateSpacing(attempt.placed, placedTemplates);
    }

    private static boolean hasLargeTemplateSpacing(PlacedTemplate target, List<PlacedTemplate> placedTemplates) {
        if (placedTemplates == null || placedTemplates.isEmpty()) {
            return true;
        }
        RotatedTemplate targetRotated = rotateTemplate(target.spec.template, target.rotation);
        Bounds2D targetBounds = boundsForPlaced(target, targetRotated);
        if (targetBounds == null) {
            return true;
        }
        Point2D targetCenter = centerOf(targetBounds);

        double nearestLargeDistance = Double.MAX_VALUE;
        for (PlacedTemplate existing : placedTemplates) {
            if (!isLarge(existing.spec)) {
                continue;
            }
            if (existing == target) {
                continue;
            }
            RotatedTemplate existingRotated = rotateTemplate(existing.spec.template, existing.rotation);
            Bounds2D existingBounds = boundsForPlaced(existing, existingRotated);
            if (existingBounds == null) {
                continue;
            }
            Point2D existingCenter = centerOf(existingBounds);
            nearestLargeDistance = Math.min(nearestLargeDistance, planarDistance(targetCenter, existingCenter));
        }
        return nearestLargeDistance >= MIN_BLOCKS_BETWEEN_LARGE;
    }

    private static Point2D centerOf(Bounds2D bounds) {
        return new Point2D((bounds.minX + bounds.maxX) / 2.0D, (bounds.minZ + bounds.maxZ) / 2.0D);
    }

    private static double planarDistance(Point2D a, Point2D b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt((dx * dx) + (dz * dz));
    }

    private static Bounds2D boundsForPlaced(PlacedTemplate placed, RotatedTemplate rotated) {
        if (placed == null || rotated == null || rotated.blocks.isEmpty()) {
            return null;
        }
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockVector3 rel : rotated.blocks.keySet()) {
            int x = placed.origin.getBlockX() + rel.getBlockX();
            int z = placed.origin.getBlockZ() + rel.getBlockZ();
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
        }
        return new Bounds2D(minX, maxX, minZ, maxZ);
    }

    private static List<TemplateSpec> candidatePoolForStep(CapturedTemplates captured,
                                                           PlacedTemplate current,
                                                           PlacementState state) {
        return candidatePoolForStep(captured, current, state, true);
    }

    private static List<TemplateSpec> candidatePoolForStep(CapturedTemplates captured,
                                                           PlacedTemplate current,
                                                           PlacementState state,
                                                           boolean allowLarge) {
        List<TemplateSpec> pool = new ArrayList<>(captured.walls());
        if (allowLarge && canPlaceLargeAfter(current, state)) {
            for (TemplateSpec large : captured.largeJunctions()) {
                if (canUseSpec(large, state)) {
                    pool.add(large);
                }
            }
        }
        return pool;
    }

    private static List<TemplateSpec> weightedShuffle(List<TemplateSpec> candidateSpecs, Random random) {
        if (candidateSpecs.isEmpty()) {
            return List.of();
        }
        List<WeightedSpec> weighted = new ArrayList<>(candidateSpecs.size());
        for (TemplateSpec spec : candidateSpecs) {
            double u = Math.max(1.0E-12D, random.nextDouble());
            double key = -Math.log(u) / Math.max(1, spec.weight);
            weighted.add(new WeightedSpec(spec, key));
        }
        weighted.sort((a, b) -> Double.compare(a.key, b.key));

        List<TemplateSpec> out = new ArrayList<>(weighted.size());
        for (WeightedSpec entry : weighted) {
            out.add(entry.spec);
        }
        return out;
    }

    private static BlockFace pickOpenSide(PlacedTemplate placed, BlockFace avoid, Random random) {
        List<BlockFace> open = placed.openSides();
        if (avoid != null) {
            open.remove(avoid);
        }
        if (open.isEmpty()) {
            return null;
        }
        return open.get(random.nextInt(open.size()));
    }

    private static void occupy(Set<Long> occupied, PlacedTemplate placed) {
        RotatedTemplate rotated = rotateTemplate(placed.spec.template, placed.rotation);
        for (BlockVector3 rel : rotated.blocks.keySet()) {
            occupied.add(posKey(
                    placed.origin.getBlockX() + rel.getBlockX(),
                    placed.origin.getBlockY() + rel.getBlockY(),
                    placed.origin.getBlockZ() + rel.getBlockZ()
            ));
        }
    }

    private static double overlapPercent(Set<Long> occupied,
                                         Map<BlockVector3, BlockData> blocks,
                                         BlockVector3 origin) {
        if (blocks.isEmpty()) {
            return 100.0D;
        }
        int overlap = 0;
        for (BlockVector3 rel : blocks.keySet()) {
            int x = origin.getBlockX() + rel.getBlockX();
            int y = origin.getBlockY() + rel.getBlockY();
            int z = origin.getBlockZ() + rel.getBlockZ();
            if (occupied.contains(posKey(x, y, z))) {
                overlap++;
            }
        }
        return (overlap * 100.0D) / blocks.size();
    }

    private static BlockVector3 slideUntilThreshold(Set<Long> occupied,
                                                    Map<BlockVector3, BlockData> movingBlocks,
                                                    BlockVector3 startOrigin,
                                                    BlockFace joinSide) {
        int towardX = -joinSide.getModX();
        int towardZ = -joinSide.getModZ();
        int awayX = joinSide.getModX();
        int awayZ = joinSide.getModZ();

        BlockVector3 current = startOrigin;
        for (int i = 0; i < 64 && overlapPercent(occupied, movingBlocks, current) > maxOverlapPercent; i++) {
            current = current.add(awayX, 0, awayZ);
        }

        for (int i = 0; i < 256; i++) {
            BlockVector3 next = current.add(towardX, 0, towardZ);
            if (overlapPercent(occupied, movingBlocks, next) > maxOverlapPercent) {
                break;
            }
            current = next;
        }

        return current;
    }

    private static boolean connectorDriftWithinLimit(BlockVector3 idealOrigin,
                                                     BlockVector3 adjustedOrigin,
                                                     TemplateSpec spec) {
        int dx = Math.abs(adjustedOrigin.getBlockX() - idealOrigin.getBlockX());
        int dz = Math.abs(adjustedOrigin.getBlockZ() - idealOrigin.getBlockZ());
        int maxDrift = isLarge(spec) ? MAX_LARGE_CONNECTOR_DRIFT_BLOCKS : MAX_CONNECTOR_DRIFT_BLOCKS;
        return (dx + dz) <= maxDrift;
    }

    private static CapturedTemplates captureAllTemplates(World world) {
        Map<String, Template> captured = new HashMap<>();
        for (TemplateSpec spec : TEMPLATE_SPECS) {
            Template template = captureTemplate(world, spec.bounds);
            if (template.blocks.isEmpty() || template.connectors.isEmpty()) {
                return null;
            }
            captured.put(spec.id, template);
        }

        Template connector = captureTemplate(world, CONNECTOR_SPEC.bounds);
        if (connector.blocks.isEmpty() || connector.connectors.isEmpty()) {
            return null;
        }

        List<TemplateSpec> walls = bind(captured, PieceCategory.WALL);
        List<TemplateSpec> large = bind(captured, PieceCategory.JUNCTION_LARGE);
        List<TemplateSpec> deadEnds = bind(captured, PieceCategory.DEAD_END);
        TemplateSpec connectorSpec = CONNECTOR_SPEC.withTemplate(connector);

        return new CapturedTemplates(walls, large, deadEnds, connectorSpec);
    }

    private static List<TemplateSpec> bind(Map<String, Template> templates, PieceCategory category) {
        List<TemplateSpec> out = new ArrayList<>();
        for (TemplateSpec spec : TEMPLATE_SPECS) {
            if (spec.category != category) {
                continue;
            }
            Template template = templates.get(spec.id);
            if (template != null) {
                out.add(spec.withTemplate(template));
            }
        }
        return out;
    }

    private static TemplateSpec pickWeighted(List<TemplateSpec> specs, Random random) {
        if (specs == null || specs.isEmpty()) {
            return null;
        }
        int total = specs.stream().mapToInt(TemplateSpec::weight).sum();
        int roll = random.nextInt(Math.max(1, total));
        int cursor = 0;
        for (TemplateSpec spec : specs) {
            cursor += Math.max(1, spec.weight);
            if (roll < cursor) {
                return spec;
            }
        }
        return specs.get(0);
    }

    private static Template captureTemplate(World world, TemplateBounds bounds) {
        int minX = Math.min(bounds.minX, bounds.maxX);
        int maxX = Math.max(bounds.minX, bounds.maxX);
        int minY = Math.min(bounds.minY, bounds.maxY);
        int maxY = Math.max(bounds.minY, bounds.maxY);
        int minZ = Math.min(bounds.minZ, bounds.maxZ);
        int maxZ = Math.max(bounds.minZ, bounds.maxZ);

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int length = maxZ - minZ + 1;

        Map<BlockVector3, BlockData> blocks = new HashMap<>();
        Set<BlockVector3> redstoneMarkers = new HashSet<>();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockData data = world.getBlockAt(x, y, z).getBlockData();
                    Material type = data.getMaterial();
                    int relX = x - minX;
                    int relY = y - minY;
                    int relZ = z - minZ;
                    BlockVector3 rel = BlockVector3.at(relX, relY, relZ);

                    if (type == Material.REDSTONE_BLOCK) {
                        redstoneMarkers.add(rel);
                    }

                    if (type.isAir() || EXCLUDED.contains(type)) {
                        continue;
                    }
                    blocks.put(rel, data);
                }
            }
        }

        Map<BlockFace, List<BlockVector3>> connectors = detectConnectorsFromMarkerWalls(redstoneMarkers, width, length);

        return new Template(blocks, connectors, width, height, length);
    }

    private static Map<BlockFace, List<BlockVector3>> detectConnectorsFromMarkerWalls(Set<BlockVector3> markers,
                                                                                       int width,
                                                                                       int length) {
        Map<BlockFace, List<ConnectorCandidate>> bySide = new EnumMap<>(BlockFace.class);
        for (BlockFace side : List.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)) {
            bySide.put(side, new ArrayList<>());
        }
        for (Set<BlockVector3> component : splitMarkerComponents(markers)) {
            if (component.isEmpty()) {
                continue;
            }
            BlockVector3 center = centerOf(new ArrayList<>(component));
            if (center == null) {
                continue;
            }

            Map<BlockFace, Integer> touchCounts = sideTouchCounts(component, width, length);
            boolean attachedToAnySide = false;
            for (Map.Entry<BlockFace, Integer> entry : touchCounts.entrySet()) {
                if (entry.getValue() <= 0) {
                    continue;
                }
                attachedToAnySide = true;
                BlockFace side = entry.getKey();
                int distanceToSide = distanceToSide(center, side, width, length);
                ConnectorCandidate candidate = new ConnectorCandidate(center, entry.getValue(), component.size(), distanceToSide);
                bySide.get(side).add(candidate);
            }

            if (!attachedToAnySide) {
                BlockFace nearestSide = nearestSide(center, width, length);
                int distanceToSide = distanceToSide(center, nearestSide, width, length);
                ConnectorCandidate candidate = new ConnectorCandidate(center, 0, component.size(), distanceToSide);
                bySide.get(nearestSide).add(candidate);
            }
        }
        Map<BlockFace, List<BlockVector3>> out = new EnumMap<>(BlockFace.class);
        for (Map.Entry<BlockFace, List<ConnectorCandidate>> entry : bySide.entrySet()) {
            entry.getValue().sort(ConnectorCandidate::compareForOrdering);
            List<BlockVector3> centers = new ArrayList<>();
            for (ConnectorCandidate candidate : entry.getValue()) {
                if (centers.stream().noneMatch(existing -> existing.equals(candidate.center))) {
                    centers.add(candidate.center);
                }
            }
            if (!centers.isEmpty()) {
                out.put(entry.getKey(), centers);
            }
        }
        return out;
    }

    private static List<Set<BlockVector3>> splitMarkerComponents(Set<BlockVector3> markers) {
        List<Set<BlockVector3>> components = new ArrayList<>();
        Set<BlockVector3> remaining = new HashSet<>(markers);
        while (!remaining.isEmpty()) {
            BlockVector3 start = remaining.iterator().next();
            Set<BlockVector3> component = new HashSet<>();
            Deque<BlockVector3> queue = new ArrayDeque<>();
            queue.add(start);
            remaining.remove(start);
            while (!queue.isEmpty()) {
                BlockVector3 current = queue.poll();
                component.add(current);
                for (BlockVector3 neighbor : markerNeighbors(current)) {
                    if (remaining.remove(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
            components.add(component);
        }
        return components;
    }

    private static List<BlockVector3> markerNeighbors(BlockVector3 current) {
        return List.of(
                current.add(1, 0, 0), current.add(-1, 0, 0),
                current.add(0, 1, 0), current.add(0, -1, 0),
                current.add(0, 0, 1), current.add(0, 0, -1)
        );
    }

    private static Map<BlockFace, Integer> sideTouchCounts(Set<BlockVector3> component, int width, int length) {
        int westTouches = 0;
        int eastTouches = 0;
        int northTouches = 0;
        int southTouches = 0;
        for (BlockVector3 marker : component) {
            if (marker.getBlockX() == 0) {
                westTouches++;
            }
            if (marker.getBlockX() == width - 1) {
                eastTouches++;
            }
            if (marker.getBlockZ() == 0) {
                northTouches++;
            }
            if (marker.getBlockZ() == length - 1) {
                southTouches++;
            }
        }

        Map<BlockFace, Integer> touches = new EnumMap<>(BlockFace.class);
        touches.put(BlockFace.WEST, westTouches);
        touches.put(BlockFace.EAST, eastTouches);
        touches.put(BlockFace.NORTH, northTouches);
        touches.put(BlockFace.SOUTH, southTouches);
        return touches;
    }

    private static BlockFace nearestSide(BlockVector3 center, int width, int length) {
        int westDist = center.getBlockX();
        int eastDist = (width - 1) - center.getBlockX();
        int northDist = center.getBlockZ();
        int southDist = (length - 1) - center.getBlockZ();
        int min = Math.min(Math.min(westDist, eastDist), Math.min(northDist, southDist));
        if (westDist == min) {
            return BlockFace.WEST;
        }
        if (eastDist == min) {
            return BlockFace.EAST;
        }
        if (northDist == min) {
            return BlockFace.NORTH;
        }
        return BlockFace.SOUTH;
    }

    private static int distanceToSide(BlockVector3 center, BlockFace side, int width, int length) {
        return switch (side) {
            case WEST -> center.getBlockX();
            case EAST -> (width - 1) - center.getBlockX();
            case NORTH -> center.getBlockZ();
            case SOUTH -> (length - 1) - center.getBlockZ();
            default -> Integer.MAX_VALUE;
        };
    }

    private static BlockVector3 centerOf(List<BlockVector3> points) {
        if (points == null || points.isEmpty()) {
            return null;
        }
        double sx = 0;
        double sy = 0;
        double sz = 0;
        for (BlockVector3 p : points) {
            sx += p.getX();
            sy += p.getY();
            sz += p.getZ();
        }
        return BlockVector3.at(
                (int) Math.round(sx / points.size()),
                (int) Math.round(sy / points.size()),
                (int) Math.round(sz / points.size())
        );
    }

    private record ConnectorCandidate(BlockVector3 center, int edgeTouches, int size, int distanceToSide) {
        private boolean betterThan(ConnectorCandidate other) {
            if (edgeTouches != other.edgeTouches) {
                return edgeTouches > other.edgeTouches;
            }
            if (size != other.size) {
                return size > other.size;
            }
            return distanceToSide < other.distanceToSide;
        }

        private static int compareForOrdering(ConnectorCandidate a, ConnectorCandidate b) {
            if (a.edgeTouches != b.edgeTouches) {
                return Integer.compare(b.edgeTouches, a.edgeTouches);
            }
            if (a.size != b.size) {
                return Integer.compare(b.size, a.size);
            }
            return Integer.compare(a.distanceToSide, b.distanceToSide);
        }
    }

    private static void paste(World world, Template template, BlockVector3 origin, int rotation) {
        RotatedTemplate rotated = rotateTemplate(template, rotation);
        for (Map.Entry<BlockVector3, BlockData> entry : rotated.blocks.entrySet()) {
            BlockVector3 rel = entry.getKey();
            BlockData data = entry.getValue();
            int x = origin.getBlockX() + rel.getBlockX();
            int y = origin.getBlockY() + rel.getBlockY();
            int z = origin.getBlockZ() + rel.getBlockZ();
            world.getBlockAt(x, y, z).setBlockData(data, false);
        }
    }

    private static RotatedTemplate rotateTemplate(Template template, int rotation) {
        int rot = Math.floorMod(rotation, 4);
        Map<BlockVector3, BlockData> out = new HashMap<>();
        for (Map.Entry<BlockVector3, BlockData> e : template.blocks.entrySet()) {
            BlockVector3 rv = rotateVector(e.getKey(), template.width, template.length, rot);
            out.put(rv, rotateBlockData(e.getValue(), rot));
        }
        Map<BlockFace, List<BlockVector3>> conn = new EnumMap<>(BlockFace.class);
        for (Map.Entry<BlockFace, List<BlockVector3>> e : template.connectors.entrySet()) {
            BlockFace rotatedSide = rotateFace(e.getKey(), rot);
            List<BlockVector3> rotatedPoints = conn.computeIfAbsent(rotatedSide, ignored -> new ArrayList<>());
            for (BlockVector3 point : e.getValue()) {
                rotatedPoints.add(rotateVector(point, template.width, template.length, rot));
            }
        }
        return new RotatedTemplate(out, conn);
    }

    private static BlockVector3 rotateVector(BlockVector3 vec, int width, int length, int rotation) {
        int x = vec.getBlockX();
        int y = vec.getBlockY();
        int z = vec.getBlockZ();
        return switch (rotation) {
            case 1 -> BlockVector3.at(length - 1 - z, y, x);
            case 2 -> BlockVector3.at(width - 1 - x, y, length - 1 - z);
            case 3 -> BlockVector3.at(z, y, width - 1 - x);
            default -> vec;
        };
    }

    private static BlockData rotateBlockData(BlockData source, int rotation) {
        BlockData data = Bukkit.createBlockData(source.getAsString());
        for (int i = 0; i < Math.floorMod(rotation, 4); i++) {
            if (data instanceof Directional directional) {
                BlockFace current = directional.getFacing();
                BlockFace next = rotateFace(current, 1);
                if (directional.getFaces().contains(next)) {
                    directional.setFacing(next);
                }
            }
            if (data instanceof Rotatable rotatable) {
                BlockFace current = rotatable.getRotation();
                BlockFace next = rotateFace(current, 1);
                rotatable.setRotation(next);
            }
            if (data instanceof Orientable orientable) {
                switch (orientable.getAxis()) {
                    case X -> orientable.setAxis(org.bukkit.Axis.Z);
                    case Z -> orientable.setAxis(org.bukkit.Axis.X);
                    default -> {
                    }
                }
            }
        }
        return data;
    }

    private static BlockFace rotateFace(BlockFace face, int rot) {
        int turns = Math.floorMod(rot, 4);
        BlockFace current = face;
        for (int i = 0; i < turns; i++) {
            current = switch (current) {
                case NORTH -> BlockFace.EAST;
                case EAST -> BlockFace.SOUTH;
                case SOUTH -> BlockFace.WEST;
                case WEST -> BlockFace.NORTH;
                default -> current;
            };
        }
        return current;
    }

    private static BlockFace opposite(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.NORTH;
            case EAST -> BlockFace.WEST;
            case WEST -> BlockFace.EAST;
            default -> face.getOppositeFace();
        };
    }

    private static long posKey(int x, int y, int z) {
        long lx = ((long) x & 0x3FFFFFFL) << 38;
        long lz = ((long) z & 0x3FFFFFFL) << 12;
        long ly = (long) y & 0xFFFL;
        return lx | lz | ly;
    }

    private enum PieceCategory {
        WALL,
        JUNCTION_LARGE,
        DEAD_END,
        CONNECTOR
    }

    private record TemplateBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    }

    private record TemplateSpec(String id,
                                TemplateBounds bounds,
                                PieceCategory category,
                                int weight,
                                Template template) {
        private TemplateSpec(String id, TemplateBounds bounds, PieceCategory category, int weight) {
            this(id, bounds, category, weight, null);
        }

        private TemplateSpec withTemplate(Template template) {
            return new TemplateSpec(id, bounds, category, weight, template);
        }
    }

    private record Template(Map<BlockVector3, BlockData> blocks,
                            Map<BlockFace, List<BlockVector3>> connectors,
                            int width,
                            int height,
                            int length) {
    }

    private record RotatedTemplate(Map<BlockVector3, BlockData> blocks,
                                   Map<BlockFace, List<BlockVector3>> connectors) {
    }

    private record CapturedTemplates(List<TemplateSpec> walls,
                                     List<TemplateSpec> largeJunctions,
                                     List<TemplateSpec> deadEnds,
                                     TemplateSpec connector) {
    }

    private record WeightedSpec(TemplateSpec spec, double key) {
    }

    private record PlacementAttempt(PlacedTemplate connector, PlacedTemplate placed) {
    }

    private record ExpansionChoice(BlockFace side, PlacementAttempt attempt, double score) {
    }

    private static final class GenerationDiagnostics {
        private int spineBlockedSides;
        private int branchBlockedSides;
        private String templateConnectorSummary = "";
    }

    public record TemplateConnectionInfo(int connectorCount, List<BlockFace> sides) {
    }

    private record PlacementState(int wallPiecesSinceLarge, int wallPiecesSinceGate) {
        private static PlacementState initial() {
            return new PlacementState(MIN_WALL_PIECES_BETWEEN_LARGE, MIN_WALL_PIECES_BETWEEN_GATES);
        }

        private static PlacementState fromSeed(TemplateSpec seed) {
            int smallCount = isLarge(seed) ? 0 : (isWall(seed) ? MIN_WALL_PIECES_BETWEEN_LARGE : 0);
            int gateCount = isGate(seed) ? 0 : MIN_WALL_PIECES_BETWEEN_GATES;
            return new PlacementState(smallCount, gateCount);
        }

        private PlacementState onPlaced(TemplateSpec placed) {
            int nextSmallSinceLarge = isLarge(placed)
                    ? 0
                    : (isWall(placed)
                    ? Math.min(MIN_WALL_PIECES_BETWEEN_LARGE + 1, wallPiecesSinceLarge + 1)
                    : wallPiecesSinceLarge);
            int nextWallsSinceGate = isGate(placed)
                    ? 0
                    : (isWall(placed)
                    ? Math.min(MIN_WALL_PIECES_BETWEEN_GATES + 1, wallPiecesSinceGate + 1)
                    : wallPiecesSinceGate);
            return new PlacementState(nextSmallSinceLarge, nextWallsSinceGate);
        }
    }

    private record Bounds2D(int minX, int maxX, int minZ, int maxZ) {
    }

    private record Point2D(double x, double z) {
    }

    private static final class PlacedTemplate {
        private final TemplateSpec spec;
        private final int rotation;
        private final BlockVector3 origin;
        private final Set<BlockFace> usedConnectors = new HashSet<>();
        private BlockFace incomingSide;

        private PlacedTemplate(TemplateSpec spec, int rotation, BlockVector3 origin) {
            this.spec = spec;
            this.rotation = rotation;
            this.origin = origin;
        }

        private List<BlockFace> openSides() {
            List<BlockFace> out = new ArrayList<>();
            for (BlockFace side : rotateTemplate(spec.template, rotation).connectors.keySet()) {
                if (!usedConnectors.contains(side)) {
                    out.add(side);
                }
            }
            return out;
        }

        private void markUsed(BlockFace side) {
            if (side != null) {
                usedConnectors.add(side);
            }
        }
    }
}
