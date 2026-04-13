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
import java.util.IdentityHashMap;
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
    private static final int MAX_SINGLE_PLACEMENTS_PER_SIDE = 48;
    private static final int MAX_CONNECTOR_BRIDGE_OPTIONS = 8;
    private static final double BRANCH_OPEN_SIDE_CHANCE = 1.00D;
    private static final boolean USE_FRONTIER_SCHEDULER = false;

    private static double maxOverlapPercent = 2.0D;
    private static final Map<Template, RotatedTemplate[]> ROTATION_CACHE = new IdentityHashMap<>();

    private StrongholdDebugGenerator() {
    }

    public static double getMaxOverlapPercent() {
        return maxOverlapPercent;
    }

    public static void setMaxOverlapPercent(double value) {
        maxOverlapPercent = Math.max(0.0D, Math.min(100.0D, value));
    }

    public static Map<String, TemplateConnectionInfo> inspectTemplateConnections() {
        clearRotationCache();
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
        clearRotationCache();

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

            ExpansionChoice choice = pickBestExpansion(spineHead, pool, captured, occupied, placed, spineState, random, diagnostics);
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

        if (USE_FRONTIER_SCHEDULER) {
            growBranchesFrontier(captured, occupied, random, placed, MAX_TOTAL_PIECES, diagnostics);
        } else {
            for (int seedIndex = 0; seedIndex < placed.size() && placed.size() < MAX_TOTAL_PIECES; seedIndex++) {
                growBranches(placed.get(seedIndex), captured, occupied, random, placed, MAX_TOTAL_PIECES, diagnostics);
            }
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
                        + ", rejected(wallPacing): " + diagnostics.rejectedWallPacing
                        + ", rejected(largeSpacing): " + diagnostics.rejectedLargeSpacing
                        + ", connectors: " + diagnostics.templateConnectorSummary);
        return true;
    }

    public static boolean generateTowerWall(Player player) {
        if (player == null) {
            return false;
        }
        clearRotationCache();

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
        CapturedTemplates captured = captureAllTemplates(sourceWorld);
        if (captured == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Failed to capture one or more stronghold templates. Check source cuboids and markers.");
            return true;
        }

        TemplateSpec tower = findTemplateById(captured.largeJunctions(), "tower_1");
        if (tower == null && !captured.largeJunctions().isEmpty()) {
            tower = captured.largeJunctions().get(0);
        }
        if (tower == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "No tower template found for towerwall preset.");
            return true;
        }

        List<TemplateSpec> wallPool = new ArrayList<>();
        for (TemplateSpec wall : captured.walls()) {
            if (wall.id.startsWith("straight_")) {
                wallPool.add(wall);
            }
        }
        if (wallPool.isEmpty()) {
            wallPool.addAll(captured.walls());
        }
        if (wallPool.isEmpty()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "No wall templates found for towerwall preset.");
            return true;
        }

        int originX = 0;
        int originY = -61;
        int originZ = 0;
        world.getChunkAt(Math.floorDiv(originX, 16), Math.floorDiv(originZ, 16)).load(true);

        List<PlacedTemplate> placed = new ArrayList<>();
        Set<Long> occupied = new HashSet<>();

        PlacedTemplate root = new PlacedTemplate(tower, 0, BlockVector3.at(originX, originY, originZ));
        placed.add(root);
        occupy(occupied, root);

        record TowerSeed(PlacedTemplate towerPlaced, int depth) {}
        Deque<TowerSeed> queue = new ArrayDeque<>();
        queue.add(new TowerSeed(root, 0));

        final int maxDepth = 2;
        final int wallsPerBranch = 3;
        while (!queue.isEmpty() && placed.size() < MAX_TOTAL_PIECES) {
            TowerSeed seed = queue.poll();
            if (seed.depth >= maxDepth) {
                continue;
            }

            for (BlockFace side : distinctOpenSides(seed.towerPlaced)) {
                PlacedTemplate current = seed.towerPlaced;
                BlockFace travelSide = side;
                boolean builtCorridor = true;

                for (int i = 0; i < wallsPerBranch; i++) {
                    PlacementAttempt wallAttempt = selectBestAttempt(current, travelSide, wallPool, captured, occupied);
                    if (wallAttempt == null) {
                        builtCorridor = false;
                        current.markUsed(travelSide);
                        break;
                    }
                    applyPlacementAttempt(current, travelSide, wallAttempt, placed, occupied);
                    current = wallAttempt.placed;
                    travelSide = opposite(current.incomingSide);
                    if (placed.size() >= MAX_TOTAL_PIECES) {
                        break;
                    }
                }
                if (!builtCorridor || placed.size() >= MAX_TOTAL_PIECES) {
                    continue;
                }

                PlacementAttempt towerAttempt = selectBestAttempt(current, travelSide, List.of(tower), captured, occupied);
                if (towerAttempt == null) {
                    current.markUsed(travelSide);
                    continue;
                }
                applyPlacementAttempt(current, travelSide, towerAttempt, placed, occupied);
                queue.add(new TowerSeed(towerAttempt.placed, seed.depth + 1));
            }
        }

        ensureTargetChunksLoaded(world, placed);
        for (PlacedTemplate entry : placed) {
            paste(world, entry.spec.template, entry.origin, entry.rotation);
        }
        player.teleport(new org.bukkit.Location(world, originX + 0.5, originY + 2, originZ + 0.5));
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Generated towerwall preset using " + placed.size() + " pieces in world '" + world.getName() + "'.");
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Towerwall diagnostics -> remaining open outputs: " + countOpenOutputs(placed)
                        + ", viable next outputs: " + countViableOpenOutputs(placed, captured, occupied));
        return true;
    }

    private static TemplateSpec findTemplateById(List<TemplateSpec> specs, String id) {
        for (TemplateSpec spec : specs) {
            if (spec.id.equalsIgnoreCase(id)) {
                return spec;
            }
        }
        return null;
    }

    private static List<BlockFace> distinctOpenSides(PlacedTemplate placed) {
        List<BlockFace> distinct = new ArrayList<>();
        for (BlockFace side : placed.openSides()) {
            if (!distinct.contains(side)) {
                distinct.add(side);
            }
        }
        return distinct;
    }

    private static PlacementAttempt selectBestAttempt(PlacedTemplate current,
                                                      BlockFace side,
                                                      List<TemplateSpec> pool,
                                                      CapturedTemplates captured,
                                                      Set<Long> occupied) {
        List<PlacementAttempt> attempts = enumeratePlacementAttempts(current, side, pool, captured.connector(), occupied);
        if (attempts.isEmpty()) {
            return null;
        }
        PlacementAttempt best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        PlacementState state = PlacementState.fromSeed(current.spec);
        for (PlacementAttempt attempt : attempts) {
            double score = scoreAttempt(current, attempt, occupied, state, captured);
            if (score > bestScore) {
                bestScore = score;
                best = attempt;
            }
        }
        return best;
    }

    private static void applyPlacementAttempt(PlacedTemplate current,
                                              BlockFace side,
                                              PlacementAttempt attempt,
                                              List<PlacedTemplate> placed,
                                              Set<Long> occupied) {
        if (attempt.connector != null) {
            attempt.connector.markUsed(side);
            placed.add(attempt.connector);
            occupy(occupied, attempt.connector);
        }
        current.markUsed(side);
        placed.add(attempt.placed);
        occupy(occupied, attempt.placed);
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

                PlacementAttempt attempt = tryPlaceFromSide(branchCurrent, branchSide, pool, captured.connector(), occupied, placed, branchState, captured, random, diagnostics);
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
                branchSide = pickBestContinuationSide(attempt.placed, avoid, captured, occupied, branchState, random);
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
        if (hasViableExpansionFrom(target, captured, occupied)) {
            return;
        }
        List<BlockFace> openSides = target.openSides();
        Collections.shuffle(openSides, random);
        for (BlockFace side : openSides) {
            if (canExpandFromSide(target, side, captured, occupied)) {
                continue;
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
                    random,
                    null
            );
            if (deadEndAttempt == null) {
                target.markUsed(side);
                continue;
            }

            target.markUsed(side);
            placed.add(deadEndAttempt.placed);
            occupy(occupied, deadEndAttempt.placed);
        }
    }

    private static boolean hasViableExpansionFrom(PlacedTemplate target,
                                                  CapturedTemplates captured,
                                                  Set<Long> occupied) {
        if (target == null || captured == null) {
            return false;
        }
        PlacementState state = PlacementState.fromSeed(target.spec);
        List<TemplateSpec> pool = candidatePoolForStep(captured, target, state);
        for (BlockFace side : target.openSides()) {
            if (!enumeratePlacementAttempts(target, side, pool, captured.connector(), occupied).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean canExpandFromSide(PlacedTemplate target,
                                             BlockFace side,
                                             CapturedTemplates captured,
                                             Set<Long> occupied) {
        PlacementState state = PlacementState.fromSeed(target.spec);
        List<TemplateSpec> pool = candidatePoolForStep(captured, target, state);
        return !enumeratePlacementAttempts(target, side, pool, captured.connector(), occupied).isEmpty();
    }

    private static BlockFace pickBestContinuationSide(PlacedTemplate placed,
                                                      BlockFace avoid,
                                                      CapturedTemplates captured,
                                                      Set<Long> occupied,
                                                      PlacementState state,
                                                      Random random) {
        List<BlockFace> open = placed.openSides();
        if (avoid != null) {
            open.removeIf(side -> side == avoid);
        }
        if (open.isEmpty()) {
            return null;
        }
        List<TemplateSpec> pool = candidatePoolForStep(captured, placed, state);
        BlockFace best = null;
        int bestCount = -1;
        for (BlockFace side : open) {
            int count = enumeratePlacementAttempts(placed, side, pool, captured.connector(), occupied).size();
            if (count > bestCount) {
                best = side;
                bestCount = count;
            }
        }
        if (bestCount > 0) {
            return best;
        }
        return open.get(random.nextInt(open.size()));
    }

    private static void growBranchesFrontier(CapturedTemplates captured,
                                             Set<Long> occupied,
                                             Random random,
                                             List<PlacedTemplate> placed,
                                             int maxPieces,
                                             GenerationDiagnostics diagnostics) {
        for (int seedIndex = 0; seedIndex < placed.size() && placed.size() < maxPieces; seedIndex++) {
            growBranches(placed.get(seedIndex), captured, occupied, random, placed, maxPieces, diagnostics);
        }
    }

    private static PlacementAttempt tryPlaceFromSide(PlacedTemplate current,
                                                     BlockFace currentSide,
                                                     List<TemplateSpec> candidateSpecs,
                                                     TemplateSpec connector,
                                                     Set<Long> occupied,
                                                     List<PlacedTemplate> placedTemplates,
                                                     PlacementState state,
                                                     CapturedTemplates captured,
                                                     Random random,
                                                     GenerationDiagnostics diagnostics) {
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
        return pickBestAttempt(current, attempts, occupied, placedTemplates, state, captured, diagnostics);
    }

    private static ExpansionChoice pickBestExpansion(PlacedTemplate current,
                                                     List<TemplateSpec> candidateSpecs,
                                                     CapturedTemplates captured,
                                                     Set<Long> occupied,
                                                     List<PlacedTemplate> placedTemplates,
                                                     PlacementState state,
                                                     Random random,
                                                     GenerationDiagnostics diagnostics) {
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
                    random,
                    diagnostics
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
        Set<String> seen = new HashSet<>();

        if (connector != null) {
            List<PlacedTemplate> connectorPlacements = enumerateSinglePlacements(
                    current, currentSide, List.of(connector), occupied, true, MAX_CONNECTOR_BRIDGE_OPTIONS
            );
            for (PlacedTemplate connectorPlaced : connectorPlacements) {
                Set<Long> occupiedWithConnector = new HashSet<>(occupied);
                occupy(occupiedWithConnector, connectorPlaced);
                for (PlacedTemplate viaConnector : enumerateSinglePlacements(
                        connectorPlaced, currentSide, candidateSpecs, occupiedWithConnector, true, MAX_SINGLE_PLACEMENTS_PER_SIDE
                )) {
                    if (areBothLarge(current.spec, viaConnector.spec)) {
                        continue;
                    }
                    String key = placementAttemptKey(connectorPlaced, viaConnector);
                    if (seen.add(key)) {
                        attempts.add(new PlacementAttempt(connectorPlaced, viaConnector));
                    }
                }
            }
        }

        for (PlacedTemplate direct : enumerateSinglePlacements(
                current, currentSide, candidateSpecs, occupied, true, MAX_SINGLE_PLACEMENTS_PER_SIDE
        )) {
            if (areBothLarge(current.spec, direct.spec)) {
                continue;
            }
            String key = placementAttemptKey(null, direct);
            if (seen.add(key)) {
                attempts.add(new PlacementAttempt(null, direct));
            }
        }
        return attempts;
    }

    private static String placementAttemptKey(PlacedTemplate connector, PlacedTemplate placed) {
        return placementKey(connector) + "|" + placementKey(placed);
    }

    private static String placementKey(PlacedTemplate placed) {
        if (placed == null) {
            return "none";
        }
        return placed.spec.id + ":" + placed.rotation + ":"
                + placed.origin.getBlockX() + ","
                + placed.origin.getBlockY() + ","
                + placed.origin.getBlockZ();
    }

    private static PlacementAttempt pickBestAttempt(PlacedTemplate current,
                                                    List<PlacementAttempt> attempts,
                                                    Set<Long> occupied,
                                                    List<PlacedTemplate> placedTemplates,
                                                    PlacementState state,
                                                    CapturedTemplates captured,
                                                    GenerationDiagnostics diagnostics) {
        PlacementAttempt best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (PlacementAttempt attempt : attempts) {
            ValidationResult validation = validatePlacementRules(attempt, state, placedTemplates);
            if (!validation.valid) {
                if (diagnostics != null && validation.reason == ValidationReason.WALL_PACING) {
                    diagnostics.rejectedWallPacing++;
                } else if (diagnostics != null && validation.reason == ValidationReason.LARGE_SPACING) {
                    diagnostics.rejectedLargeSpacing++;
                }
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

        int openOutputs = attempt.placed.openSides().size();
        int connectorDiversity = countDistinctSides(attempt.placed.openSides());

        int branchBonus = openOutputs >= 2 ? 1 : 0;
        int junctionBonus = isLarge(attempt.placed.spec) ? 1 : 0;
        int continuationBonus = !areBothLarge(current.spec, attempt.placed.spec) ? 1 : 0;

        return (openOutputs * 30.0D)
                + (connectorDiversity * 12.0D)
                + (branchBonus * 20.0D)
                + (junctionBonus * 12.0D)
                + (continuationBonus * 6.0D)
                - overlap;
    }

    private static int countDistinctSides(List<BlockFace> sides) {
        return new HashSet<>(sides).size();
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

    private static List<PlacedTemplate> enumerateSinglePlacements(PlacedTemplate current,
                                                                  BlockFace currentSide,
                                                                  List<TemplateSpec> candidateSpecs,
                                                                  Set<Long> occupied,
                                                                  boolean enforceOverlap,
                                                                  int maxPlacements) {
        List<PlacedTemplate> placements = new ArrayList<>();
        RotatedTemplate currentRotated = rotateTemplate(current.spec.template, current.rotation);
        List<BlockVector3> currentConnectors = currentRotated.connectors.get(currentSide);
        if (currentConnectors == null || currentConnectors.isEmpty()) {
            return placements;
        }
        Set<String> seenPlacements = new HashSet<>();
        int perConnectorBudget = Math.max(1, maxPlacements / Math.max(1, currentConnectors.size()));

        // Pass 1: ensure each connector slot contributes options before one slot monopolizes the cap.
        for (BlockVector3 currentConnector : currentConnectors) {
            int addedForConnector = enumerateFromConnector(
                    current,
                    currentSide,
                    currentConnector,
                    candidateSpecs,
                    occupied,
                    enforceOverlap,
                    perConnectorBudget,
                    maxPlacements,
                    placements,
                    seenPlacements
            );
            if (placements.size() >= maxPlacements) {
                return placements;
            }
            if (addedForConnector <= 0) {
                continue;
            }
        }

        // Pass 2: fill remaining budget with any additional valid options.
        for (BlockVector3 currentConnector : currentConnectors) {
            enumerateFromConnector(
                    current,
                    currentSide,
                    currentConnector,
                    candidateSpecs,
                    occupied,
                    enforceOverlap,
                    Integer.MAX_VALUE,
                    maxPlacements,
                    placements,
                    seenPlacements
            );
            if (placements.size() >= maxPlacements) {
                return placements;
            }
        }
        return placements;
    }

    private static int enumerateFromConnector(PlacedTemplate current,
                                              BlockFace currentSide,
                                              BlockVector3 currentConnector,
                                              List<TemplateSpec> candidateSpecs,
                                              Set<Long> occupied,
                                              boolean enforceOverlap,
                                              int limitForConnector,
                                              int maxPlacements,
                                              List<PlacedTemplate> out,
                                              Set<String> seenPlacements) {
        int added = 0;
        BlockVector3 worldConnector = current.origin.add(currentConnector);
        for (TemplateSpec spec : candidateSpecs) {
            for (int rot = 0; rot < 4; rot++) {
                RotatedTemplate rotated = rotateTemplate(spec.template, rot);
                List<BlockVector3> candidateConnectors = rotated.connectors.get(opposite(currentSide));
                if (candidateConnectors == null || candidateConnectors.isEmpty()) {
                    continue;
                }
                for (BlockVector3 candidateConnector : candidateConnectors) {
                    BlockVector3 idealOrigin = worldConnector.subtract(candidateConnector);
                    BlockVector3 origin = adjustedOriginForOverlap(spec, occupied, rotated.blocks, idealOrigin, currentSide);
                    if (!connectorDriftWithinLimit(idealOrigin, origin, spec)) {
                        continue;
                    }
                    if (enforceOverlap && overlapPercent(occupied, rotated.blocks, origin) > maxOverlapPercent) {
                        continue;
                    }
                    PlacedTemplate placed = new PlacedTemplate(spec, rot, origin);
                    placed.incomingSide = opposite(currentSide);
                    placed.markUsed(opposite(currentSide));
                    String key = placementKey(placed);
                    if (!seenPlacements.add(key)) {
                        continue;
                    }
                    out.add(placed);
                    added++;
                    if (added >= limitForConnector || out.size() >= maxPlacements) {
                        return added;
                    }
                }
            }
        }
        return added;
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

    private static ValidationResult validatePlacementRules(PlacementAttempt attempt,
                                                           PlacementState state,
                                                           List<PlacedTemplate> placedTemplates) {
        if (attempt == null || attempt.placed == null) {
            return ValidationResult.denied(ValidationReason.INVALID_ATTEMPT);
        }
        if (!isLarge(attempt.placed.spec)) {
            return ValidationResult.allowed();
        }
        if (state.wallPiecesSinceLarge < MIN_WALL_PIECES_BETWEEN_LARGE) {
            return ValidationResult.denied(ValidationReason.WALL_PACING);
        }
        if (!hasLargeTemplateSpacing(attempt.placed, placedTemplates)) {
            return ValidationResult.denied(ValidationReason.LARGE_SPACING);
        }
        return ValidationResult.allowed();
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
            open.removeIf(side -> side == avoid);
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

    private static BlockVector3 adjustedOriginForOverlap(TemplateSpec spec,
                                                         Set<Long> occupied,
                                                         Map<BlockVector3, BlockData> movingBlocks,
                                                         BlockVector3 idealOrigin,
                                                         BlockFace joinSide) {
        if (isLarge(spec)) {
            return idealOrigin;
        }
        return slideUntilThreshold(occupied, movingBlocks, idealOrigin, joinSide);
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
        RotatedTemplate[] cached = ROTATION_CACHE.computeIfAbsent(template, ignored -> new RotatedTemplate[4]);
        int rot = Math.floorMod(rotation, 4);
        RotatedTemplate rotatedCached = cached[rot];
        if (rotatedCached != null) {
            return rotatedCached;
        }
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
        RotatedTemplate built = new RotatedTemplate(out, conn);
        cached[rot] = built;
        return built;
    }

    private static void clearRotationCache() {
        ROTATION_CACHE.clear();
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
        private int rejectedWallPacing;
        private int rejectedLargeSpacing;
        private String templateConnectorSummary = "";
    }

    private enum ValidationReason {
        NONE,
        INVALID_ATTEMPT,
        WALL_PACING,
        LARGE_SPACING
    }

    private record ValidationResult(boolean valid, ValidationReason reason) {
        private static ValidationResult allowed() {
            return new ValidationResult(true, ValidationReason.NONE);
        }

        private static ValidationResult denied(ValidationReason reason) {
            return new ValidationResult(false, reason);
        }
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
        private final Map<BlockFace, Integer> usedConnectorCounts = new EnumMap<>(BlockFace.class);
        private BlockFace incomingSide;

        private PlacedTemplate(TemplateSpec spec, int rotation, BlockVector3 origin) {
            this.spec = spec;
            this.rotation = rotation;
            this.origin = origin;
        }

        private List<BlockFace> openSides() {
            List<BlockFace> out = new ArrayList<>();
            RotatedTemplate rotated = rotateTemplate(spec.template, rotation);
            for (Map.Entry<BlockFace, List<BlockVector3>> entry : rotated.connectors.entrySet()) {
                BlockFace side = entry.getKey();
                int totalForSide = entry.getValue() == null ? 0 : entry.getValue().size();
                int usedForSide = usedConnectorCounts.getOrDefault(side, 0);
                int remaining = Math.max(0, totalForSide - usedForSide);
                for (int i = 0; i < remaining; i++) {
                    out.add(side);
                }
            }
            return out;
        }

        private void markUsed(BlockFace side) {
            if (side != null) {
                usedConnectorCounts.merge(side, 1, Integer::sum);
            }
        }
    }
}
