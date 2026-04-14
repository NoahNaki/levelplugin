package me.nakilex.levelplugin.debug;

import com.sk89q.worldedit.math.BlockVector3;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.IdentityHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Debug-only stronghold composer.
 */
public final class StrongholdDebugGenerator {
    private static final Set<Material> EXCLUDED = Set.of(
            Material.REDSTONE_BLOCK,
            Material.LIGHT_BLUE_CONCRETE,
            Material.WHITE_CONCRETE
    );
    private static final Set<Material> CONNECTOR_MARKER_MATERIALS = Set.of(Material.REDSTONE_BLOCK);

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
            new TemplateSpec("tower_1", new TemplateBounds(615, -61, -5488, 685, -7, -5418), PieceCategory.JUNCTION_LARGE, 1),
            new TemplateSpec("gate_1", new TemplateBounds(686, -61, -5346, 614, -10, -5418), PieceCategory.JUNCTION_LARGE, 1),
            new TemplateSpec("gate_2", new TemplateBounds(686, -61, -5276, 614, -10, -5346), PieceCategory.JUNCTION_LARGE, 1),
            new TemplateSpec("church", new TemplateBounds(757, -61, -5559, 827, 34, -5489), PieceCategory.JUNCTION_LARGE, 1),
            new TemplateSpec("smallfort", new TemplateBounds(615, -61, -5701, 685, -22, -5631), PieceCategory.WALL, 1),
            new TemplateSpec("fortpassage", new TemplateBounds(685, -61, -5630, 615, -22, -5560), PieceCategory.WALL, 1),
            new TemplateSpec("fort", new TemplateBounds(615, -61, -5559, 685, -18, -5489), PieceCategory.WALL, 1),
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
    private static final int MIN_WALL_PIECES_BETWEEN_TOWERS = 2;
    private static final int MIN_BLOCKS_BETWEEN_TOWERS = 44;
    private static final int MAX_CONNECTOR_DRIFT_BLOCKS = 6;
    private static final int MAX_LARGE_CONNECTOR_DRIFT_BLOCKS = 0;
    private static final int CONNECTOR_SIDE_CAPTURE_DISTANCE = 2;
    private static final int MAX_SINGLE_PLACEMENTS_PER_SIDE = 48;
    private static final int MAX_CONNECTOR_BRIDGE_OPTIONS = 8;
    private static final int MIN_SINGLE_PLACEMENTS_PER_SIDE = 12;
    private static final int MIN_CONNECTOR_BRIDGE_OPTIONS = 2;
    private static final int OCCUPIED_BLOCKS_SOFT_CAP = 120_000;
    private static final int OCCUPIED_BLOCKS_HARD_CAP = 220_000;
    private static final double BRANCH_OPEN_SIDE_CHANCE = 1.00D;
    private static final int REQUIRED_CHURCH_CLEARANCE_RADIUS = 5;
    private static final double FORCED_LARGE_TEMPLATE_OVERLAP_PERCENT = 8.0D;
    private static final int SATELLITE_CHURCH_SEARCH_PADDING = 80;
    private static final int SATELLITE_CHURCH_SEARCH_STEP = 6;
    private static final int SATELLITE_CHURCH_MAX_PADDING = 360;
    private static final int SATELLITE_CHURCH_FAR_OFFSET = 220;
    private static final int MAX_EMERGENCY_TEMPLATE_RADIUS = 1200;
    private static final int MAX_SATELLITE_LINK_SEGMENTS = 6;
    private static final boolean USE_FRONTIER_SCHEDULER = false;
    private static final boolean ENABLE_EXPENSIVE_DIAGNOSTICS = false;
    private static final int TARGET_GATE_TEMPLATES = 2;
    private static final Map<String, Integer> REQUIRED_TEMPLATE_COUNTS = Map.of(
            "church", 1,
            "fort", 1,
            "smallfort", 1,
            "fortpassage", 1
    );
    private static final double UNDERUSED_TEMPLATE_BONUS = 35.0D;

    private static double maxOverlapPercent = 2.0D;
    private static final Map<Template, RotatedTemplate[]> ROTATION_CACHE = new IdentityHashMap<>();
    private static final Map<String, BlockData[]> BLOCK_DATA_ROTATION_CACHE = new HashMap<>();
    private static CapturedTemplates cachedCapturedTemplates;
    private static Map<String, TemplateConnectionInfo> cachedTemplateConnectionInfo;
    private static final List<UsageRule> USAGE_RULES = List.of(
            new UsageRule(spec -> spec != null && isGate(spec), TARGET_GATE_TEMPLATES, UNDERUSED_TEMPLATE_BONUS),
            new UsageRule(spec -> matcherForTemplateId("church").test(spec), requiredCountForTemplate("church"), UNDERUSED_TEMPLATE_BONUS),
            new UsageRule(spec -> matcherForTemplateId("fort").test(spec), requiredCountForTemplate("fort"), UNDERUSED_TEMPLATE_BONUS),
            new UsageRule(spec -> matcherForTemplateId("smallfort").test(spec), requiredCountForTemplate("smallfort"), UNDERUSED_TEMPLATE_BONUS),
            new UsageRule(spec -> matcherForTemplateId("fortpassage").test(spec), requiredCountForTemplate("fortpassage"), UNDERUSED_TEMPLATE_BONUS)
    );
    private static final List<ConnectorRequirementRule> CONNECTOR_REQUIREMENT_RULES = List.of(
            ConnectorRequirementRule.symmetric(
                    spec -> spec != null && isWall(spec),
                    spec -> spec != null && isTSection(spec)
            )
    );

    private StrongholdDebugGenerator() {
    }

    public static double getMaxOverlapPercent() {
        return maxOverlapPercent;
    }

    public static void setMaxOverlapPercent(double value) {
        maxOverlapPercent = Math.max(0.0D, Math.min(100.0D, value));
    }

    public static Map<String, TemplateConnectionInfo> inspectTemplateConnections() {
        CapturedTemplates captured = loadCapturedTemplates(false);
        if (captured == null) {
            return Map.of();
        }
        return cachedTemplateConnectionInfo == null ? Map.of() : cachedTemplateConnectionInfo;
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

        SourceSetup setup = prepareSourceTemplates(player, true);
        if (setup == null) {
            return true;
        }
        World sourceWorld = setup.sourceWorld();
        CapturedTemplates captured = setup.captured();

        World world = createGeneratedWorld(plugin, player);
        if (world == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Failed to create a superflat world for stronghold generation.");
            return true;
        }

        Random random = ThreadLocalRandom.current();
        GenerationDiagnostics diagnostics = new GenerationDiagnostics();
        diagnostics.templateConnectorSummary = templateConnectorSummary(captured);

        int originX = 0;
        int originZ = 0;
        world.getChunkAt(Math.floorDiv(originX, 16), Math.floorDiv(originZ, 16)).load(true);
        int originY = -61;

        List<PlacedTemplate> placed = new ArrayList<>();
        Set<Long> occupied = new HashSet<>();

        TemplateSpec startSpec = pickWeighted(eligibleWallPool(captured.walls()), random);
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
        List<TemplateSpec> allCandidateTemplates = new ArrayList<>();
        allCandidateTemplates.addAll(captured.walls());
        allCandidateTemplates.addAll(captured.largeJunctions());
        allCandidateTemplates.addAll(captured.deadEnds());

        for (String requiredTemplateId : REQUIRED_TEMPLATE_COUNTS.keySet()) {
            Predicate<TemplateSpec> matcher = matcherForTemplateId(requiredTemplateId);
            PlacedTemplate leastOverlapRequired = placeTemplateAtLeastOverlapOpenOutput(
                    matcher,
                    allCandidateTemplates,
                    captured.connector(),
                    occupied,
                    placed
            );
            if (leastOverlapRequired != null) {
                diagnostics.requiredLeastOverlapPlaced++;
                if ("church".equalsIgnoreCase(requiredTemplateId)) {
                    diagnostics.churchLeastOverlapPlaced = true;
                }
                growBranches(leastOverlapRequired, captured, occupied, random, placed, MAX_TOTAL_PIECES, diagnostics);
            }
        }
        SatellitePlacementResult satellitePlacement = placeSatelliteChurchWithOptionalLink(
                captured,
                occupied,
                random,
                placed,
                MAX_TOTAL_PIECES
        );
        diagnostics.satelliteChurchPlaced = satellitePlacement.placed();
        diagnostics.satelliteLinkSegments = satellitePlacement.linkSegments();

        int forcedRequiredPlacements = 0;
        boolean churchEmergencyPlaced = false;
        int requiredEmergencyPlacements = 0;
        for (Map.Entry<String, Integer> requiredTemplate : REQUIRED_TEMPLATE_COUNTS.entrySet()) {
            String templateId = requiredTemplate.getKey();
            Predicate<TemplateSpec> matcher = matcherForTemplateId(templateId);
            forcedRequiredPlacements += ensureTemplatePlacements(
                    matcher,
                    allCandidateTemplates,
                    requiredTemplate.getValue(),
                    FORCED_LARGE_TEMPLATE_OVERLAP_PERCENT,
                    captured,
                    occupied,
                    random,
                    placed,
                    MAX_TOTAL_PIECES
            );
            TemplateSpec requiredSpec = findTemplateById(allCandidateTemplates, templateId);
            boolean emergencyPlaced = forceTemplatePlacementIfMissing(
                    matcher,
                    requiredSpec,
                    -1,
                    occupied,
                    placed,
                    MAX_TOTAL_PIECES
            );
            if (emergencyPlaced) {
                requiredEmergencyPlacements++;
            }
            if ("church".equalsIgnoreCase(templateId)) {
                churchEmergencyPlaced = emergencyPlaced;
            }
        }
        diagnostics.requiredPlacementsForced = forcedRequiredPlacements;
        diagnostics.requiredEmergencyPlaced = requiredEmergencyPlacements;
        diagnostics.churchEmergencyPlaced = churchEmergencyPlaced;
        int sealedViableOutputs = closeViableOutputsWithDeadEnds(captured, occupied, random, placed);
        int finalChurchCount = countPlacedTemplatesMatching(placed, matcherForTemplateId("church"));

        for (PlacedTemplate entry : placed) {
            paste(world, entry.spec.template, entry.origin, entry.rotation);
        }
        int requiredRawCopies = 0;
        for (Map.Entry<String, Integer> requiredTemplate : REQUIRED_TEMPLATE_COUNTS.entrySet()) {
            String templateId = requiredTemplate.getKey();
            int requiredCount = requiredTemplate.getValue();
            int currentCount = countPlacedTemplatesMatching(placed, matcherForTemplateId(templateId));
            if (currentCount >= requiredCount) {
                continue;
            }
            TemplateSpec template = findTemplateById(allCandidateTemplates, templateId);
            if (template == null) {
                continue;
            }
            BlockVector3 rawOrigin = findRawPasteOriginNearFootprint(placed, template, originY);
            boolean copied = pasteTemplateSpecDirect(sourceWorld, world, template, rawOrigin);
            if (copied) {
                requiredRawCopies++;
                if ("church".equalsIgnoreCase(templateId)) {
                    diagnostics.churchRawCopied = true;
                    finalChurchCount = Math.max(finalChurchCount, requiredCount);
                }
            }
        }
        diagnostics.requiredRawCopied = requiredRawCopies;

        player.teleport(new org.bukkit.Location(world, originX + 0.5, originY + 2, originZ + 0.5));
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Generated stronghold spine+branches using " + placed.size()
                        + " pieces in world '" + world.getName() + "' (overlap threshold: "
                        + String.format("%.2f", maxOverlapPercent) + "%).");
        String viableOutputSummary = ENABLE_EXPENSIVE_DIAGNOSTICS
                ? String.valueOf(countViableOpenOutputs(placed, captured, occupied))
                : "skipped";
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Stronghold diagnostics -> spine blocked sides: " + diagnostics.spineBlockedSides
                        + ", branch blocked sides: " + diagnostics.branchBlockedSides
                        + ", remaining open outputs: " + countOpenOutputs(placed)
                        + ", viable next outputs: " + viableOutputSummary
                        + ", sealed viable outputs: " + sealedViableOutputs
                        + ", church placed: " + finalChurchCount
                        + ", required forced: " + diagnostics.requiredPlacementsForced
                        + ", church least-overlap: " + diagnostics.churchLeastOverlapPlaced
                        + ", required least-overlap: " + diagnostics.requiredLeastOverlapPlaced
                        + ", church satellite: " + diagnostics.satelliteChurchPlaced
                        + ", church emergency: " + diagnostics.churchEmergencyPlaced
                        + ", required emergency: " + diagnostics.requiredEmergencyPlaced
                        + ", church raw copy: " + diagnostics.churchRawCopied
                        + ", required raw copy: " + diagnostics.requiredRawCopied
                        + ", church origins: " + summarizeTemplateOrigins(placed, spec -> spec != null && "church".equalsIgnoreCase(spec.id))
                        + ", required counts: " + summarizeRequiredTemplateCounts(placed)
                        + ", satellite link segments: " + diagnostics.satelliteLinkSegments
                        + ", rejected(wallPacing): " + diagnostics.rejectedWallPacing
                        + ", rejected(largeSpacing): " + diagnostics.rejectedLargeSpacing
                        + ", placed templates: " + summarizePlacedTemplates(placed)
                        + ", template connectors(captured): " + diagnostics.templateConnectorSummary);
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

        SourceSetup setup = prepareSourceTemplates(player, true);
        if (setup == null) {
            return true;
        }
        CapturedTemplates captured = setup.captured();

        World world = createGeneratedWorld(plugin, player);
        if (world == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Failed to create a superflat world for stronghold generation.");
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
        List<TemplateSpec> linearWallPool = new ArrayList<>();
        for (TemplateSpec wall : wallPool) {
            if (isLinearWallTemplate(wall)) {
                linearWallPool.add(wall);
            }
        }
        if (!linearWallPool.isEmpty()) {
            wallPool = linearWallPool;
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

        final int maxBranches = 4;
        int builtBranches = 0;
        for (BlockFace side : distinctOpenSides(root)) {
            if (builtBranches >= maxBranches || placed.size() >= MAX_TOTAL_PIECES) {
                break;
            }
            PlacementAttempt wallAttempt = selectBestAttempt(root, side, wallPool, captured, occupied, 6, 0, false);
            if (wallAttempt == null) {
                root.markUsed(side);
                continue;
            }
            applyPlacementAttempt(root, side, wallAttempt, placed, occupied);
            builtBranches++;
        }

        for (PlacedTemplate entry : placed) {
            paste(world, entry.spec.template, entry.origin, entry.rotation);
        }
        player.teleport(new org.bukkit.Location(world, originX + 0.5, originY + 2, originZ + 0.5));
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Generated towerwall cross preset using " + placed.size() + " pieces in world '" + world.getName() + "'.");
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Towerwall diagnostics -> remaining open outputs: " + countOpenOutputs(placed)
                        + ", viable next outputs: skipped"
                        + ", walls built: " + builtBranches + "/" + maxBranches);
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

    private static Predicate<TemplateSpec> matcherForTemplateId(String templateId) {
        return spec -> spec != null && templateId != null && templateId.equalsIgnoreCase(spec.id);
    }

    private static int requiredCountForTemplate(String templateId) {
        if (templateId == null) {
            return 0;
        }
        return REQUIRED_TEMPLATE_COUNTS.getOrDefault(templateId.toLowerCase(java.util.Locale.ROOT), 0);
    }

    private static boolean isRequiredTemplate(TemplateSpec spec) {
        if (spec == null) {
            return false;
        }
        return requiredCountForTemplate(spec.id) > 0;
    }

    private static boolean isExclusiveRequiredWallTemplate(TemplateSpec spec) {
        return isWall(spec) && isRequiredTemplate(spec);
    }

    private static List<TemplateSpec> eligibleWallPool(List<TemplateSpec> walls) {
        List<TemplateSpec> pool = new ArrayList<>();
        if (walls == null) {
            return pool;
        }
        for (TemplateSpec wall : walls) {
            if (!isExclusiveRequiredWallTemplate(wall)) {
                pool.add(wall);
            }
        }
        return pool;
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
        return selectBestAttempt(current, side, pool, captured, occupied,
                MAX_SINGLE_PLACEMENTS_PER_SIDE, MAX_CONNECTOR_BRIDGE_OPTIONS, true);
    }

    private static PlacementAttempt selectBestAttempt(PlacedTemplate current,
                                                      BlockFace side,
                                                      List<TemplateSpec> pool,
                                                      CapturedTemplates captured,
                                                      Set<Long> occupied,
                                                      int maxSinglePlacements,
                                                      int maxConnectorBridgeOptions) {
        return selectBestAttempt(current, side, pool, captured, occupied,
                maxSinglePlacements, maxConnectorBridgeOptions, true);
    }

    private static PlacementAttempt selectBestAttempt(PlacedTemplate current,
                                                      BlockFace side,
                                                      List<TemplateSpec> pool,
                                                      CapturedTemplates captured,
                                                      Set<Long> occupied,
                                                      int maxSinglePlacements,
                                                      int maxConnectorBridgeOptions,
                                                      boolean allowOverlapSlide) {
        List<PlacementAttempt> attempts = enumeratePlacementAttempts(
                current, side, pool, captured.connector(), occupied,
                maxSinglePlacements, maxConnectorBridgeOptions, allowOverlapSlide
        );
        if (attempts.isEmpty()) {
            return null;
        }
        PlacementAttempt best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        PlacementState state = PlacementState.fromSeed(current.spec);
        for (PlacementAttempt attempt : attempts) {
            double score = scoreAttempt(current, attempt, occupied, state, captured, null);
            if (score > bestScore) {
                bestScore = score;
                best = attempt;
            }
        }
        return best;
    }

    private static boolean isLinearWallTemplate(TemplateSpec spec) {
        if (spec == null || spec.template == null || spec.template.connectors == null) {
            return false;
        }
        Set<BlockFace> sides = spec.template.connectors.keySet();
        if (sides.size() != 2) {
            return false;
        }
        if (sides.contains(BlockFace.NORTH) && sides.contains(BlockFace.SOUTH)) {
            return true;
        }
        return sides.contains(BlockFace.EAST) && sides.contains(BlockFace.WEST);
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

    private static SourceSetup prepareSourceTemplates(Player player, boolean forceRefresh) {
        Main plugin = Main.getInstance();
        if (plugin == null || plugin.getWorldManager() == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Plugin bootstrap is unavailable. Try again after startup completes.");
            return null;
        }
        plugin.getWorldManager().ensureWorldsLoaded(SOURCE_WORLD);
        World sourceWorld = Bukkit.getWorld(SOURCE_WORLD);
        if (sourceWorld == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Source template world '" + SOURCE_WORLD + "' is not loaded.");
            return null;
        }
        CapturedTemplates captured = loadCapturedTemplates(forceRefresh);
        if (captured == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Failed to capture one or more stronghold templates. Check source cuboids and markers.");
            return null;
        }
        return new SourceSetup(sourceWorld, captured);
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
            PlacementAttempt deadEndAttempt = tryPlaceDeadEndFromSide(target, side, captured, occupied, random, placed);
            if (deadEndAttempt == null) {
                target.markUsed(side);
                continue;
            }
            applyDeadEndPlacement(target, side, deadEndAttempt, placed, occupied);
        }
    }

    private static int closeViableOutputsWithDeadEnds(CapturedTemplates captured,
                                                       Set<Long> occupied,
                                                       Random random,
                                                       List<PlacedTemplate> placed) {
        if (captured == null || captured.deadEnds().isEmpty()) {
            return 0;
        }
        int sealed = 0;
        boolean progress = true;
        while (progress) {
            progress = false;
            List<PlacedTemplate> snapshot = new ArrayList<>(placed);
            for (PlacedTemplate entry : snapshot) {
                List<BlockFace> openSides = entry.openSides();
                Collections.shuffle(openSides, random);
                for (BlockFace side : openSides) {
                    if (!canExpandFromSide(entry, side, captured, occupied)) {
                        continue;
                    }
                    PlacementAttempt deadEndAttempt = tryPlaceDeadEndFromSide(entry, side, captured, occupied, random, placed);
                    if (deadEndAttempt == null) {
                        continue;
                    }
                    applyDeadEndPlacement(entry, side, deadEndAttempt, placed, occupied);
                    sealed++;
                    progress = true;
                }
            }
        }
        return sealed;
    }

    private static PlacementAttempt tryPlaceDeadEndFromSide(PlacedTemplate target,
                                                             BlockFace side,
                                                             CapturedTemplates captured,
                                                             Set<Long> occupied,
                                                             Random random,
                                                             List<PlacedTemplate> placed) {
        return tryPlaceFromSide(
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
    }

    private static void applyDeadEndPlacement(PlacedTemplate target,
                                              BlockFace side,
                                              PlacementAttempt deadEndAttempt,
                                              List<PlacedTemplate> placed,
                                              Set<Long> occupied) {
        target.markUsed(side);
        placed.add(deadEndAttempt.placed);
        occupy(occupied, deadEndAttempt.placed);
    }

    private static SatellitePlacementResult placeSatelliteChurchWithOptionalLink(CapturedTemplates captured,
                                                                                 Set<Long> occupied,
                                                                                 Random random,
                                                                                 List<PlacedTemplate> placed,
                                                                                 int maxPieces) {
        if (captured == null || placed == null || placed.isEmpty() || placed.size() >= maxPieces) {
            return SatellitePlacementResult.none();
        }
        if (countPlacedTemplatesMatching(placed, matcherForTemplateId("church"))
                >= requiredCountForTemplate("church")) {
            return SatellitePlacementResult.none();
        }
        TemplateSpec church = findTemplateById(captured.largeJunctions(), "church");
        if (church == null) {
            return SatellitePlacementResult.none();
        }
        PlacedTemplate satelliteChurch = findSatelliteChurchPlacement(church, occupied, placed);
        if (satelliteChurch == null) {
            return SatellitePlacementResult.none();
        }
        placed.add(satelliteChurch);
        occupy(occupied, satelliteChurch);
        int linkedSegments = attemptSatelliteLink(satelliteChurch, captured, occupied, random, placed, maxPieces);
        return new SatellitePlacementResult(true, linkedSegments);
    }

    private static PlacedTemplate findSatelliteChurchPlacement(TemplateSpec church,
                                                               Set<Long> occupied,
                                                               List<PlacedTemplate> placed) {
        Bounds2D footprint = combinedFootprint(placed);
        if (footprint == null) {
            return null;
        }
        int baseY = placed.get(0).origin.getBlockY();
        PlacedTemplate nearby = findSatellitePlacementInRange(
                church,
                occupied,
                baseY,
                footprint,
                SATELLITE_CHURCH_SEARCH_PADDING,
                SATELLITE_CHURCH_SEARCH_STEP
        );
        if (nearby != null) {
            return nearby;
        }
        PlacedTemplate wider = findSatellitePlacementInRange(
                church,
                occupied,
                baseY,
                footprint,
                SATELLITE_CHURCH_MAX_PADDING,
                SATELLITE_CHURCH_SEARCH_STEP * 2
        );
        if (wider != null) {
            return wider;
        }
        List<BlockVector3> farAnchors = List.of(
                BlockVector3.at(footprint.maxX + SATELLITE_CHURCH_FAR_OFFSET, baseY, footprint.maxZ + SATELLITE_CHURCH_FAR_OFFSET),
                BlockVector3.at(footprint.minX - SATELLITE_CHURCH_FAR_OFFSET, baseY, footprint.maxZ + SATELLITE_CHURCH_FAR_OFFSET),
                BlockVector3.at(footprint.maxX + SATELLITE_CHURCH_FAR_OFFSET, baseY, footprint.minZ - SATELLITE_CHURCH_FAR_OFFSET),
                BlockVector3.at(footprint.minX - SATELLITE_CHURCH_FAR_OFFSET, baseY, footprint.minZ - SATELLITE_CHURCH_FAR_OFFSET)
        );
        for (BlockVector3 anchor : farAnchors) {
            for (int rotation = 0; rotation < 4; rotation++) {
                PlacedTemplate candidate = new PlacedTemplate(church, rotation, anchor);
                if (canPlaceWithClearance(candidate, occupied, REQUIRED_CHURCH_CLEARANCE_RADIUS)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static PlacedTemplate findSatellitePlacementInRange(TemplateSpec church,
                                                                Set<Long> occupied,
                                                                int baseY,
                                                                Bounds2D footprint,
                                                                int padding,
                                                                int step) {
        int minX = footprint.minX - padding;
        int maxX = footprint.maxX + padding;
        int minZ = footprint.minZ - padding;
        int maxZ = footprint.maxZ + padding;
        int spacing = Math.max(1, step);

        for (int x = minX; x <= maxX; x += spacing) {
            for (int z = minZ; z <= maxZ; z += spacing) {
                for (int rotation = 0; rotation < 4; rotation++) {
                    PlacedTemplate candidate = new PlacedTemplate(church, rotation, BlockVector3.at(x, baseY, z));
                    if (canPlaceWithClearance(candidate, occupied, REQUIRED_CHURCH_CLEARANCE_RADIUS)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private static boolean canPlaceWithClearance(PlacedTemplate candidate,
                                                 Set<Long> occupied,
                                                 int clearanceRadius) {
        RotatedTemplate rotated = rotateTemplate(candidate.spec.template, candidate.rotation);
        if (!isOverlapWithinThreshold(occupied, rotated.blocks, candidate.origin, maxOverlapPercent)) {
            return false;
        }
        if (clearanceRadius < 0) {
            return true;
        }
        return hasExpandedAreaClearance(candidate, occupied, clearanceRadius);
    }

    private static boolean forceTemplatePlacementIfMissing(Predicate<TemplateSpec> matcher,
                                                           TemplateSpec template,
                                                           int clearanceRadius,
                                                           Set<Long> occupied,
                                                           List<PlacedTemplate> placed,
                                                           int maxPieces) {
        if (matcher == null || template == null || placed.size() >= maxPieces) {
            return false;
        }
        if (countPlacedTemplatesMatching(placed, matcher) > 0) {
            return false;
        }
        Bounds2D footprint = combinedFootprint(placed);
        if (footprint == null) {
            return false;
        }
        int baseY = placed.get(0).origin.getBlockY();
        int centerX = (footprint.minX + footprint.maxX) / 2;
        int centerZ = (footprint.minZ + footprint.maxZ) / 2;
        for (int radius = SATELLITE_CHURCH_FAR_OFFSET; radius <= MAX_EMERGENCY_TEMPLATE_RADIUS; radius += 40) {
            for (int dx = -radius; dx <= radius; dx += 40) {
                int[] zs = new int[]{-radius, radius};
                for (int dz : zs) {
                    PlacedTemplate candidate = tryTemplateAtAllRotations(template, occupied, clearanceRadius,
                            BlockVector3.at(centerX + dx, baseY, centerZ + dz));
                    if (candidate != null) {
                        placed.add(candidate);
                        occupy(occupied, candidate);
                        return true;
                    }
                }
            }
            for (int dz = -radius + 40; dz <= radius - 40; dz += 40) {
                int[] xs = new int[]{-radius, radius};
                for (int dx : xs) {
                    PlacedTemplate candidate = tryTemplateAtAllRotations(template, occupied, clearanceRadius,
                            BlockVector3.at(centerX + dx, baseY, centerZ + dz));
                    if (candidate != null) {
                        placed.add(candidate);
                        occupy(occupied, candidate);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static PlacedTemplate placeTemplateAtLeastOverlapOpenOutput(Predicate<TemplateSpec> matcher,
                                                                        List<TemplateSpec> candidates,
                                                                        TemplateSpec connectorTemplate,
                                                                        Set<Long> occupied,
                                                                        List<PlacedTemplate> placed) {
        if (matcher == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (countPlacedTemplatesMatching(placed, matcher) > 0) {
            return null;
        }
        TemplateSpec targetTemplate = null;
        for (TemplateSpec candidate : candidates) {
            if (matcher.test(candidate)) {
                targetTemplate = candidate;
                break;
            }
        }
        if (targetTemplate == null) {
            return null;
        }
        LeastOverlapChoice best = null;
        Set<String> seen = new HashSet<>();
        List<PlacedTemplate> snapshot = new ArrayList<>(placed);
        for (PlacedTemplate source : snapshot) {
            for (BlockFace side : source.openSides()) {
                for (PlacementAttempt attempt : enumerateLeastOverlapAttempts(
                        source, side, targetTemplate, connectorTemplate, occupied
                )) {
                    String key = System.identityHashCode(source) + ":" + side + ":" + placementAttemptKey(attempt.connector, attempt.placed);
                    if (!seen.add(key)) {
                        continue;
                    }
                    RotatedTemplate rotated = rotateTemplate(attempt.placed.spec.template, attempt.placed.rotation);
                    double overlap = overlapPercent(occupied, rotated.blocks, attempt.placed.origin);
                    if (best == null || overlap < best.overlap()) {
                        best = new LeastOverlapChoice(source, side, attempt, overlap);
                    }
                }
            }
        }
        if (best == null) {
            return null;
        }
        applyPlacementAttempt(best.source(), best.side(), best.attempt(), placed, occupied);
        return best.attempt().placed();
    }

    private static List<PlacementAttempt> enumerateLeastOverlapAttempts(PlacedTemplate source,
                                                                        BlockFace side,
                                                                        TemplateSpec targetTemplate,
                                                                        TemplateSpec connectorTemplate,
                                                                        Set<Long> occupied) {
        List<PlacementAttempt> attempts = new ArrayList<>();
        int occupiedSize = occupied == null ? 0 : occupied.size();
        int effectiveSinglePlacements = adaptivePlacementLimit(
                MAX_SINGLE_PLACEMENTS_PER_SIDE,
                MIN_SINGLE_PLACEMENTS_PER_SIDE,
                occupiedSize
        );
        int effectiveConnectorOptions = adaptivePlacementLimit(
                MAX_CONNECTOR_BRIDGE_OPTIONS,
                MIN_CONNECTOR_BRIDGE_OPTIONS,
                occupiedSize
        );
        for (PlacedTemplate direct : enumerateSinglePlacements(
                source,
                side,
                List.of(targetTemplate),
                occupied,
                false,
                effectiveSinglePlacements,
                false
        )) {
            if (areBothLarge(source.spec, direct.spec)) {
                continue;
            }
            if (requiresConnectorBetween(source.spec, direct.spec)) {
                continue;
            }
            attempts.add(new PlacementAttempt(null, direct));
        }

        if (connectorTemplate == null) {
            return attempts;
        }
        for (PlacedTemplate connectorPlaced : enumerateSinglePlacements(
                source,
                side,
                List.of(connectorTemplate),
                occupied,
                false,
                effectiveConnectorOptions,
                false
        )) {
            List<PlacedTemplate> viaPlacements = withTemporaryOccupancy(
                    occupied,
                    connectorPlaced,
                    () -> enumerateSinglePlacements(
                            connectorPlaced,
                            side,
                            List.of(targetTemplate),
                            occupied,
                            false,
                            effectiveSinglePlacements,
                            false
                    )
            );
            for (PlacedTemplate viaConnector : viaPlacements) {
                if (areBothLarge(source.spec, viaConnector.spec)) {
                    continue;
                }
                attempts.add(new PlacementAttempt(connectorPlaced, viaConnector));
            }
        }
        return attempts;
    }

    private static PlacedTemplate tryTemplateAtAllRotations(TemplateSpec template,
                                                            Set<Long> occupied,
                                                            int clearanceRadius,
                                                            BlockVector3 origin) {
        for (int rotation = 0; rotation < 4; rotation++) {
            PlacedTemplate candidate = new PlacedTemplate(template, rotation, origin);
            if (canPlaceWithClearance(candidate, occupied, clearanceRadius)) {
                return candidate;
            }
        }
        return null;
    }

    private static BlockVector3 findRawPasteOriginNearFootprint(List<PlacedTemplate> placed,
                                                                 TemplateSpec template,
                                                                 int y) {
        Bounds2D footprint = combinedFootprint(placed);
        int x = 240;
        int z = 240;
        if (footprint != null) {
            x = footprint.maxX + SATELLITE_CHURCH_FAR_OFFSET;
            z = footprint.maxZ + SATELLITE_CHURCH_FAR_OFFSET;
        }
        int minX = Math.min(template.bounds.minX, template.bounds.maxX);
        int minY = Math.min(template.bounds.minY, template.bounds.maxY);
        int minZ = Math.min(template.bounds.minZ, template.bounds.maxZ);
        return BlockVector3.at(x - minX, y - minY, z - minZ);
    }

    private static boolean pasteTemplateSpecDirect(World sourceWorld,
                                                   World targetWorld,
                                                   TemplateSpec template,
                                                   BlockVector3 targetOrigin) {
        if (sourceWorld == null || targetWorld == null || template == null || targetOrigin == null) {
            return false;
        }
        int minX = Math.min(template.bounds.minX, template.bounds.maxX);
        int maxX = Math.max(template.bounds.minX, template.bounds.maxX);
        int minY = Math.min(template.bounds.minY, template.bounds.maxY);
        int maxY = Math.max(template.bounds.minY, template.bounds.maxY);
        int minZ = Math.min(template.bounds.minZ, template.bounds.maxZ);
        int maxZ = Math.max(template.bounds.minZ, template.bounds.maxZ);
        boolean pastedAny = false;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    org.bukkit.block.Block sourceBlock = sourceWorld.getBlockAt(x, y, z);
                    Material type = sourceBlock.getType();
                    if (type == Material.AIR || EXCLUDED.contains(type)) {
                        continue;
                    }
                    int tx = targetOrigin.getBlockX() + (x - minX);
                    int ty = targetOrigin.getBlockY() + (y - minY);
                    int tz = targetOrigin.getBlockZ() + (z - minZ);
                    targetWorld.getBlockAt(tx, ty, tz).setBlockData(sourceBlock.getBlockData(), false);
                    pastedAny = true;
                }
            }
        }
        return pastedAny;
    }

    private static int attemptSatelliteLink(PlacedTemplate satelliteChurch,
                                            CapturedTemplates captured,
                                            Set<Long> occupied,
                                            Random random,
                                            List<PlacedTemplate> placed,
                                            int maxPieces) {
        if (satelliteChurch == null || captured == null || placed.size() >= maxPieces) {
            return 0;
        }
        TemplateSpec nearest = findNearestTemplateWithOpenSide(placed, satelliteChurch);
        if (nearest == null) {
            return 0;
        }
        PlacedTemplate current = findPlacedTemplateBySpec(placed, nearest);
        if (current == null) {
            return 0;
        }
        List<TemplateSpec> preferredWalls = preferredLinkWallPool(captured);
        PlacementState state = PlacementState.fromSeed(current.spec);
        int segments = 0;

        while (segments < MAX_SATELLITE_LINK_SEGMENTS && placed.size() < maxPieces) {
            BlockFace side = pickOpenSideToward(current, satelliteChurch, captured, occupied, state, random);
            if (side == null) {
                break;
            }
            PlacementAttempt attempt = tryPlaceFromSide(
                    current,
                    side,
                    preferredWalls,
                    captured.connector(),
                    occupied,
                    placed,
                    state,
                    captured,
                    random,
                    null
            );
            if (attempt == null) {
                current.markUsed(side);
                break;
            }
            applyPlacementAttempt(current, side, attempt, placed, occupied);
            state = state.onPlaced(attempt.placed.spec);
            current = attempt.placed;
            segments++;
        }
        return segments;
    }

    private static List<TemplateSpec> preferredLinkWallPool(CapturedTemplates captured) {
        List<TemplateSpec> straightWalls = new ArrayList<>();
        for (TemplateSpec wall : captured.walls()) {
            if (isLinearWallTemplate(wall)) {
                straightWalls.add(wall);
            }
        }
        if (!straightWalls.isEmpty()) {
            return straightWalls;
        }
        return captured.walls();
    }

    private static BlockFace pickOpenSideToward(PlacedTemplate current,
                                                PlacedTemplate target,
                                                CapturedTemplates captured,
                                                Set<Long> occupied,
                                                PlacementState state,
                                                Random random) {
        List<BlockFace> openSides = current.openSides();
        if (openSides.isEmpty()) {
            return null;
        }
        BlockFace bestSide = null;
        double bestDistance = Double.MAX_VALUE;
        Point2D targetCenter = centerOf(boundsForPlaced(target, rotateTemplate(target.spec.template, target.rotation)));
        List<TemplateSpec> pool = candidatePoolForStep(captured, current, state);
        for (BlockFace side : openSides) {
            if (enumeratePlacementAttempts(current, side, pool, captured.connector(), occupied).isEmpty()) {
                continue;
            }
            BlockVector3 probe = probeTowardSide(current, side);
            double distance = planarDistance(new Point2D(probe.getBlockX(), probe.getBlockZ()), targetCenter);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestSide = side;
            }
        }
        if (bestSide != null) {
            return bestSide;
        }
        return openSides.get(random.nextInt(openSides.size()));
    }

    private static BlockVector3 probeTowardSide(PlacedTemplate current, BlockFace side) {
        RotatedTemplate rotated = rotateTemplate(current.spec.template, current.rotation);
        Bounds2D bounds = boundsForPlaced(current, rotated);
        int x = (bounds.minX + bounds.maxX) / 2;
        int z = (bounds.minZ + bounds.maxZ) / 2;
        return switch (side) {
            case NORTH -> BlockVector3.at(x, current.origin.getBlockY(), bounds.minZ - 1);
            case SOUTH -> BlockVector3.at(x, current.origin.getBlockY(), bounds.maxZ + 1);
            case EAST -> BlockVector3.at(bounds.maxX + 1, current.origin.getBlockY(), z);
            case WEST -> BlockVector3.at(bounds.minX - 1, current.origin.getBlockY(), z);
            default -> current.origin;
        };
    }

    private static TemplateSpec findNearestTemplateWithOpenSide(List<PlacedTemplate> placed,
                                                                PlacedTemplate target) {
        if (target == null) {
            return null;
        }
        Point2D targetCenter = centerOf(boundsForPlaced(target, rotateTemplate(target.spec.template, target.rotation)));
        TemplateSpec best = null;
        double bestDistance = Double.MAX_VALUE;
        for (PlacedTemplate entry : placed) {
            if (entry == target || entry.openSides().isEmpty()) {
                continue;
            }
            Bounds2D bounds = boundsForPlaced(entry, rotateTemplate(entry.spec.template, entry.rotation));
            Point2D center = centerOf(bounds);
            double distance = planarDistance(center, targetCenter);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entry.spec;
            }
        }
        return best;
    }

    private static PlacedTemplate findPlacedTemplateBySpec(List<PlacedTemplate> placed, TemplateSpec spec) {
        for (PlacedTemplate entry : placed) {
            if (entry != null && entry.spec == spec) {
                return entry;
            }
        }
        return null;
    }

    private static Bounds2D combinedFootprint(List<PlacedTemplate> placed) {
        if (placed == null || placed.isEmpty()) {
            return null;
        }
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (PlacedTemplate entry : placed) {
            Bounds2D bounds = boundsForPlaced(entry, rotateTemplate(entry.spec.template, entry.rotation));
            if (bounds == null) {
                continue;
            }
            minX = Math.min(minX, bounds.minX);
            maxX = Math.max(maxX, bounds.maxX);
            minZ = Math.min(minZ, bounds.minZ);
            maxZ = Math.max(maxZ, bounds.maxZ);
        }
        if (minX == Integer.MAX_VALUE) {
            return null;
        }
        return new Bounds2D(minX, maxX, minZ, maxZ);
    }

    private static int ensureTemplatePlacements(Predicate<TemplateSpec> matcher,
                                                List<TemplateSpec> candidateTemplates,
                                                int requiredCount,
                                                double relaxedOverlapPercent,
                                                CapturedTemplates captured,
                                                Set<Long> occupied,
                                                Random random,
                                                List<PlacedTemplate> placed,
                                                int maxPieces) {
        if (matcher == null || candidateTemplates == null || candidateTemplates.isEmpty()) {
            return 0;
        }
        List<TemplateSpec> matchingTemplates = candidateTemplates.stream()
                .filter(matcher)
                .toList();
        if (matchingTemplates.isEmpty()) {
            return 0;
        }
        int forced = 0;
        int currentCount = countPlacedTemplatesMatching(placed, matcher);
        while (currentCount < requiredCount && placed.size() < maxPieces) {
            if (!tryPlaceTemplateFromOpenOutput(
                    matchingTemplates,
                    relaxedOverlapPercent,
                    captured,
                    occupied,
                    random,
                    placed
            )) {
                break;
            }
            forced++;
            currentCount++;
        }
        return forced;
    }

    private static boolean tryPlaceTemplateFromOpenOutput(List<TemplateSpec> candidateTemplates,
                                                          double relaxedOverlapPercent,
                                                          CapturedTemplates captured,
                                                          Set<Long> occupied,
                                                          Random random,
                                                          List<PlacedTemplate> placed) {
        List<PlacedTemplate> snapshot = new ArrayList<>(placed);
        Collections.shuffle(snapshot, random);
        for (PlacedTemplate source : snapshot) {
            List<BlockFace> openSides = source.openSides();
            Collections.shuffle(openSides, random);
            for (BlockFace side : openSides) {
                PlacementAttempt attempt = tryPlaceFromSide(
                        source,
                        side,
                        candidateTemplates,
                        captured.connector(),
                        occupied,
                        placed,
                        PlacementState.fromSeed(source.spec),
                        captured,
                        random,
                        null
                );
                if (attempt == null && relaxedOverlapPercent > maxOverlapPercent && containsLargeTemplate(candidateTemplates)) {
                    attempt = tryPlaceFromSideWithTemporaryOverlap(
                            source,
                            side,
                            candidateTemplates,
                            captured,
                            occupied,
                            random,
                            placed,
                            relaxedOverlapPercent
                    );
                }
                if (attempt == null) {
                    continue;
                }
                applyPlacementAttempt(source, side, attempt, placed, occupied);
                return true;
            }
        }
        return false;
    }

    private static boolean containsLargeTemplate(List<TemplateSpec> candidateTemplates) {
        for (TemplateSpec template : candidateTemplates) {
            if (isLarge(template)) {
                return true;
            }
        }
        return false;
    }

    private static PlacementAttempt tryPlaceFromSideWithTemporaryOverlap(PlacedTemplate source,
                                                                         BlockFace side,
                                                                         List<TemplateSpec> candidateTemplates,
                                                                         CapturedTemplates captured,
                                                                         Set<Long> occupied,
                                                                         Random random,
                                                                         List<PlacedTemplate> placed,
                                                                         double temporaryOverlapPercent) {
        double previousOverlap = maxOverlapPercent;
        setMaxOverlapPercent(temporaryOverlapPercent);
        try {
            return tryPlaceFromSide(
                    source,
                    side,
                    candidateTemplates,
                    captured.connector(),
                    occupied,
                    placed,
                    PlacementState.fromSeed(source.spec),
                    captured,
                    random,
                    null
            );
        } finally {
            setMaxOverlapPercent(previousOverlap);
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
        PlacementAttempt prioritizedChurch = tryPrioritizedChurchPlacement(
                current,
                currentSide,
                candidateSpecs,
                connector,
                occupied,
                placedTemplates,
                state,
                captured,
                diagnostics
        );
        if (prioritizedChurch != null) {
            return prioritizedChurch;
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

    private static PlacementAttempt tryPrioritizedChurchPlacement(PlacedTemplate current,
                                                                  BlockFace currentSide,
                                                                  List<TemplateSpec> candidateSpecs,
                                                                  TemplateSpec connector,
                                                                  Set<Long> occupied,
                                                                  List<PlacedTemplate> placedTemplates,
                                                                  PlacementState state,
                                                                  CapturedTemplates captured,
                                                                  GenerationDiagnostics diagnostics) {
        if (placedTemplates == null || captured == null) {
            return null;
        }
        if (countPlacedTemplatesMatching(placedTemplates, matcherForTemplateId("church"))
                >= requiredCountForTemplate("church")) {
            return null;
        }
        TemplateSpec church = null;
        for (TemplateSpec candidate : candidateSpecs) {
            if (candidate != null && "church".equalsIgnoreCase(candidate.id)) {
                church = candidate;
                break;
            }
        }
        if (church == null) {
            return null;
        }
        List<PlacementAttempt> churchAttempts = enumeratePlacementAttempts(
                current,
                currentSide,
                List.of(church),
                connector,
                occupied
        );
        if (churchAttempts.isEmpty()) {
            return null;
        }
        churchAttempts.removeIf(attempt -> !hasExpandedAreaClearance(attempt.placed, occupied, REQUIRED_CHURCH_CLEARANCE_RADIUS));
        if (churchAttempts.isEmpty()) {
            return null;
        }
        return pickBestAttempt(current, churchAttempts, occupied, placedTemplates, state, captured, diagnostics);
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
            double score = scoreAttempt(current, attempt, occupied, state, captured, placedTemplates);
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
        return enumeratePlacementAttempts(current, currentSide, candidateSpecs, connector, occupied,
                MAX_SINGLE_PLACEMENTS_PER_SIDE, MAX_CONNECTOR_BRIDGE_OPTIONS, true);
    }

    private static List<PlacementAttempt> enumeratePlacementAttempts(PlacedTemplate current,
                                                                     BlockFace currentSide,
                                                                     List<TemplateSpec> candidateSpecs,
                                                                     TemplateSpec connector,
                                                                     Set<Long> occupied,
                                                                     int maxSinglePlacements,
                                                                     int maxConnectorBridgeOptions) {
        return enumeratePlacementAttempts(current, currentSide, candidateSpecs, connector, occupied,
                maxSinglePlacements, maxConnectorBridgeOptions, true);
    }

    private static List<PlacementAttempt> enumeratePlacementAttempts(PlacedTemplate current,
                                                                     BlockFace currentSide,
                                                                     List<TemplateSpec> candidateSpecs,
                                                                     TemplateSpec connector,
                                                                     Set<Long> occupied,
                                                                     int maxSinglePlacements,
                                                                     int maxConnectorBridgeOptions,
                                                                     boolean allowOverlapSlide) {
        List<PlacementAttempt> attempts = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int occupiedSize = occupied == null ? 0 : occupied.size();
        int effectiveSinglePlacements = adaptivePlacementLimit(
                maxSinglePlacements,
                MIN_SINGLE_PLACEMENTS_PER_SIDE,
                occupiedSize
        );
        int effectiveConnectorOptions = adaptivePlacementLimit(
                maxConnectorBridgeOptions,
                MIN_CONNECTOR_BRIDGE_OPTIONS,
                occupiedSize
        );
        boolean effectiveAllowOverlapSlide = shouldAllowOverlapSlide(allowOverlapSlide, occupiedSize);

        if (connector != null && effectiveConnectorOptions > 0) {
            List<PlacedTemplate> connectorPlacements = enumerateSinglePlacements(
                    current, currentSide, List.of(connector), occupied, true, effectiveConnectorOptions, effectiveAllowOverlapSlide
            );
            for (PlacedTemplate connectorPlaced : connectorPlacements) {
                List<PlacedTemplate> viaPlacements = withTemporaryOccupancy(
                        occupied,
                        connectorPlaced,
                        () -> enumerateSinglePlacements(
                                connectorPlaced,
                                currentSide,
                                candidateSpecs,
                                occupied,
                                true,
                                effectiveSinglePlacements,
                                effectiveAllowOverlapSlide
                        )
                );
                for (PlacedTemplate viaConnector : viaPlacements) {
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
                current, currentSide, candidateSpecs, occupied, true, effectiveSinglePlacements, effectiveAllowOverlapSlide
        )) {
            if (areBothLarge(current.spec, direct.spec)) {
                continue;
            }
            if (requiresConnectorBetween(current.spec, direct.spec)) {
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
            ValidationResult validation = validatePlacementRules(current.spec, attempt, state, placedTemplates);
            if (!validation.valid) {
                if (diagnostics != null && validation.reason == ValidationReason.WALL_PACING) {
                    diagnostics.rejectedWallPacing++;
                } else if (diagnostics != null && validation.reason == ValidationReason.LARGE_SPACING) {
                    diagnostics.rejectedLargeSpacing++;
                }
                continue;
            }
            double score = scoreAttempt(current, attempt, occupied, state, captured, placedTemplates);
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
                                       CapturedTemplates captured,
                                       List<PlacedTemplate> placedTemplates) {
        RotatedTemplate rotated = rotateTemplate(attempt.placed.spec.template, attempt.placed.rotation);
        double overlap = overlapPercent(occupied, rotated.blocks, attempt.placed.origin);

        int openOutputs = attempt.placed.openSides().size();
        int connectorDiversity = countDistinctSides(attempt.placed.openSides());

        int branchBonus = openOutputs >= 2 ? 1 : 0;
        int junctionBonus = isLarge(attempt.placed.spec) ? 1 : 0;
        int continuationBonus = !areBothLarge(current.spec, attempt.placed.spec) ? 1 : 0;
        double usageBonus = usageDiversityBonus(attempt.placed.spec, placedTemplates);

        return (openOutputs * 30.0D)
                + (connectorDiversity * 12.0D)
                + (branchBonus * 20.0D)
                + (junctionBonus * 12.0D)
                + (continuationBonus * 6.0D)
                + usageBonus
                - overlap;
    }

    private static double usageDiversityBonus(TemplateSpec candidate, List<PlacedTemplate> placedTemplates) {
        if (candidate == null || placedTemplates == null || placedTemplates.isEmpty()) {
            return 0.0D;
        }
        double bonus = 0.0D;
        for (UsageRule rule : USAGE_RULES) {
            if (!rule.matcher().test(candidate)) {
                continue;
            }
            int currentCount = countPlacedTemplatesMatching(placedTemplates, rule.matcher());
            if (currentCount < rule.targetCount()) {
                bonus += (rule.targetCount() - currentCount) * rule.bonusPerMissing();
            }
        }
        return bonus;
    }

    private static int countPlacedTemplatesMatching(List<PlacedTemplate> placedTemplates, Predicate<TemplateSpec> matcher) {
        if (placedTemplates == null || placedTemplates.isEmpty() || matcher == null) {
            return 0;
        }
        int count = 0;
        for (PlacedTemplate placedTemplate : placedTemplates) {
            if (placedTemplate != null && placedTemplate.spec != null && matcher.test(placedTemplate.spec)) {
                count++;
            }
        }
        return count;
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

    private static String summarizePlacedTemplates(List<PlacedTemplate> placed) {
        if (placed == null || placed.isEmpty()) {
            return "none";
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (PlacedTemplate entry : placed) {
            if (entry == null || entry.spec == null) {
                continue;
            }
            counts.merge(entry.spec.id, 1, Integer::sum);
        }
        List<String> summary = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            summary.add(entry.getKey() + ":" + entry.getValue());
        }
        return String.join(", ", summary);
    }

    private static String summarizeTemplateOrigins(List<PlacedTemplate> placed, Predicate<TemplateSpec> matcher) {
        if (placed == null || placed.isEmpty() || matcher == null) {
            return "none";
        }
        List<String> out = new ArrayList<>();
        for (PlacedTemplate entry : placed) {
            if (entry == null || entry.spec == null || !matcher.test(entry.spec)) {
                continue;
            }
            out.add(entry.origin.getBlockX() + "," + entry.origin.getBlockY() + "," + entry.origin.getBlockZ());
        }
        if (out.isEmpty()) {
            return "none";
        }
        return String.join(" | ", out);
    }

    private static String summarizeRequiredTemplateCounts(List<PlacedTemplate> placed) {
        if (placed == null || placed.isEmpty()) {
            return "none";
        }
        List<String> summary = new ArrayList<>();
        for (Map.Entry<String, Integer> required : REQUIRED_TEMPLATE_COUNTS.entrySet()) {
            int count = countPlacedTemplatesMatching(placed, matcherForTemplateId(required.getKey()));
            summary.add(required.getKey() + ":" + count + "/" + required.getValue());
        }
        return String.join(", ", summary);
    }

    private static List<PlacedTemplate> enumerateSinglePlacements(PlacedTemplate current,
                                                                  BlockFace currentSide,
                                                                  List<TemplateSpec> candidateSpecs,
                                                                  Set<Long> occupied,
                                                                  boolean enforceOverlap,
                                                                  int maxPlacements,
                                                                  boolean allowOverlapSlide) {
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
                    seenPlacements,
                    allowOverlapSlide
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
                    seenPlacements,
                    allowOverlapSlide
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
                                              Set<String> seenPlacements,
                                              boolean allowOverlapSlide) {
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
                    BlockVector3 origin = allowOverlapSlide
                            ? adjustedOriginForOverlap(spec, occupied, rotated.blocks, idealOrigin, currentSide)
                            : idealOrigin;
                    if (!connectorDriftWithinLimit(idealOrigin, origin, spec)) {
                        continue;
                    }
                    if (enforceOverlap && !isOverlapWithinThreshold(occupied, rotated.blocks, origin, maxOverlapPercent)) {
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

    private static boolean isTSection(TemplateSpec spec) {
        return "t_section".equalsIgnoreCase(spec.id);
    }

    private static boolean isTower(TemplateSpec spec) {
        return spec.id.startsWith("tower_");
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
        if (isGate(spec) && !canPlaceGate(state)) {
            return false;
        }
        if (isTower(spec) && state.wallPiecesSinceTower < MIN_WALL_PIECES_BETWEEN_TOWERS) {
            return false;
        }
        return true;
    }

    private static boolean requiresConnectorBetween(TemplateSpec from, TemplateSpec to) {
        if (from == null || to == null) {
            return false;
        }
        for (ConnectorRequirementRule rule : CONNECTOR_REQUIREMENT_RULES) {
            if (rule.matches(from, to)) {
                return true;
            }
        }
        return false;
    }

    private static ValidationResult validatePlacementRules(TemplateSpec sourceSpec,
                                                           PlacementAttempt attempt,
                                                           PlacementState state,
                                                           List<PlacedTemplate> placedTemplates) {
        if (attempt == null || attempt.placed == null) {
            return ValidationResult.denied(ValidationReason.INVALID_ATTEMPT);
        }
        if (attempt.connector == null && requiresConnectorBetween(sourceSpec, attempt.placed.spec)) {
            return ValidationResult.denied(ValidationReason.CONNECTOR_REQUIRED);
        }
        if (!isLarge(attempt.placed.spec)) {
            return ValidationResult.allowed();
        }
        if (state.wallPiecesSinceLarge < MIN_WALL_PIECES_BETWEEN_LARGE) {
            return ValidationResult.denied(ValidationReason.WALL_PACING);
        }
        if (!hasTemplateSpacing(attempt.placed, placedTemplates, StrongholdDebugGenerator::isLarge, MIN_BLOCKS_BETWEEN_LARGE)) {
            return ValidationResult.denied(ValidationReason.LARGE_SPACING);
        }
        if (isTower(attempt.placed.spec)
                && !hasTemplateSpacing(attempt.placed, placedTemplates, StrongholdDebugGenerator::isTower, MIN_BLOCKS_BETWEEN_TOWERS)) {
            return ValidationResult.denied(ValidationReason.LARGE_SPACING);
        }
        return ValidationResult.allowed();
    }

    private static boolean hasTemplateSpacing(PlacedTemplate target,
                                              List<PlacedTemplate> placedTemplates,
                                              java.util.function.Predicate<TemplateSpec> filter,
                                              int minDistance) {
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
            if (!filter.test(existing.spec)) {
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
        return nearestLargeDistance >= minDistance;
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
        if (placed == null) {
            return null;
        }
        return placed.bounds2D();
    }

    private static boolean hasExpandedAreaClearance(PlacedTemplate placed,
                                                    Set<Long> occupied,
                                                    int expansionRadius) {
        if (placed == null || occupied == null) {
            return false;
        }
        RotatedTemplate rotated = rotateTemplate(placed.spec.template, placed.rotation);
        Bounds3D bounds = boundsForPlaced3D(placed, rotated);
        if (bounds == null) {
            return false;
        }
        int minX = bounds.minX - Math.max(0, expansionRadius);
        int maxX = bounds.maxX + Math.max(0, expansionRadius);
        int minZ = bounds.minZ - Math.max(0, expansionRadius);
        int maxZ = bounds.maxZ + Math.max(0, expansionRadius);

        for (long key : occupied) {
            int x = unpackPosX(key);
            if (x < minX || x > maxX) {
                continue;
            }
            int y = unpackPosY(key);
            if (y < bounds.minY || y > bounds.maxY) {
                continue;
            }
            int z = unpackPosZ(key);
            if (z < minZ || z > maxZ) {
                continue;
            }
            return false;
        }
        return true;
    }

    private static Bounds3D boundsForPlaced3D(PlacedTemplate placed, RotatedTemplate rotated) {
        if (placed == null) {
            return null;
        }
        return placed.bounds3D();
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
        List<TemplateSpec> pool = eligibleWallPool(captured.walls());
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

    private static Set<Long> occupiedKeysForPlaced(PlacedTemplate placed) {
        if (placed == null || placed.spec == null || placed.spec.template == null) {
            return Set.of();
        }
        Set<Long> keys = new HashSet<>();
        occupy(keys, placed);
        return keys;
    }

    private static <T> T withTemporaryOccupancy(Set<Long> occupied, PlacedTemplate temporary, Supplier<T> operation) {
        if (occupied == null || operation == null) {
            return null;
        }
        Set<Long> tempKeys = occupiedKeysForPlaced(temporary);
        if (tempKeys.isEmpty()) {
            return operation.get();
        }
        Set<Long> added = new HashSet<>();
        for (Long key : tempKeys) {
            if (occupied.add(key)) {
                added.add(key);
            }
        }
        try {
            return operation.get();
        } finally {
            occupied.removeAll(added);
        }
    }

    private static int adaptivePlacementLimit(int configuredMax, int configuredMin, int occupiedSize) {
        if (configuredMax <= 0) {
            return 0;
        }
        int boundedMin = Math.max(1, Math.min(configuredMax, configuredMin));
        if (occupiedSize <= OCCUPIED_BLOCKS_SOFT_CAP) {
            return configuredMax;
        }
        if (occupiedSize >= OCCUPIED_BLOCKS_HARD_CAP) {
            return boundedMin;
        }
        double progress = (occupiedSize - OCCUPIED_BLOCKS_SOFT_CAP)
                / (double) (OCCUPIED_BLOCKS_HARD_CAP - OCCUPIED_BLOCKS_SOFT_CAP);
        int scaled = (int) Math.round(configuredMax - ((configuredMax - boundedMin) * progress));
        return Math.max(boundedMin, Math.min(configuredMax, scaled));
    }

    private static boolean shouldAllowOverlapSlide(boolean requested, int occupiedSize) {
        return requested && occupiedSize < OCCUPIED_BLOCKS_HARD_CAP;
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

    private static boolean isOverlapWithinThreshold(Set<Long> occupied,
                                                    Map<BlockVector3, BlockData> blocks,
                                                    BlockVector3 origin,
                                                    double thresholdPercent) {
        if (blocks.isEmpty()) {
            return false;
        }
        double boundedThreshold = Math.max(0.0D, Math.min(100.0D, thresholdPercent));
        if (boundedThreshold >= 100.0D) {
            return true;
        }
        int allowedOverlaps = (int) Math.floor((boundedThreshold / 100.0D) * blocks.size());
        int overlap = 0;
        for (BlockVector3 rel : blocks.keySet()) {
            int x = origin.getBlockX() + rel.getBlockX();
            int y = origin.getBlockY() + rel.getBlockY();
            int z = origin.getBlockZ() + rel.getBlockZ();
            if (occupied.contains(posKey(x, y, z))) {
                overlap++;
                if (overlap > allowedOverlaps) {
                    return false;
                }
            }
        }
        return true;
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
        for (int i = 0; i < 64 && !isOverlapWithinThreshold(occupied, movingBlocks, current, maxOverlapPercent); i++) {
            current = current.add(awayX, 0, awayZ);
        }

        for (int i = 0; i < 256; i++) {
            BlockVector3 next = current.add(towardX, 0, towardZ);
            if (!isOverlapWithinThreshold(occupied, movingBlocks, next, maxOverlapPercent)) {
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
        Map<Long, ChunkSnapshot> snapshots = loadChunkSnapshots(world, minX, maxX, minZ, maxZ);

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int length = maxZ - minZ + 1;

        Map<BlockVector3, BlockData> blocks = new HashMap<>();
        Set<BlockVector3> redstoneMarkers = new HashSet<>();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockData data = blockDataAt(snapshots, world, x, y, z);
                    Material type = data.getMaterial();
                    int relX = x - minX;
                    int relY = y - minY;
                    int relZ = z - minZ;
                    BlockVector3 rel = BlockVector3.at(relX, relY, relZ);

                    if (CONNECTOR_MARKER_MATERIALS.contains(type)) {
                        redstoneMarkers.add(rel);
                    }

                    if (type.isAir() || EXCLUDED.contains(type) || CONNECTOR_MARKER_MATERIALS.contains(type)) {
                        continue;
                    }
                    blocks.put(rel, data);
                }
            }
        }

        StructureFootprint footprint = structureFootprintFor(blocks, width, length);
        Map<BlockFace, List<BlockVector3>> connectors = detectConnectorsFromMarkers(redstoneMarkers, footprint);

        return new Template(blocks, connectors, width, height, length);
    }

    private static Map<Long, ChunkSnapshot> loadChunkSnapshots(World world, int minX, int maxX, int minZ, int maxZ) {
        Map<Long, ChunkSnapshot> snapshots = new HashMap<>();
        int minChunkX = Math.floorDiv(minX, 16);
        int maxChunkX = Math.floorDiv(maxX, 16);
        int minChunkZ = Math.floorDiv(minZ, 16);
        int maxChunkZ = Math.floorDiv(maxZ, 16);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                long key = chunkKey(chunkX, chunkZ);
                snapshots.put(key, world.getChunkAt(chunkX, chunkZ).getChunkSnapshot(false, false, false));
            }
        }
        return snapshots;
    }

    private static BlockData blockDataAt(Map<Long, ChunkSnapshot> snapshots, World world, int x, int y, int z) {
        int chunkX = Math.floorDiv(x, 16);
        int chunkZ = Math.floorDiv(z, 16);
        ChunkSnapshot snapshot = snapshots.get(chunkKey(chunkX, chunkZ));
        int localX = Math.floorMod(x, 16);
        int localZ = Math.floorMod(z, 16);
        if (snapshot != null && y >= world.getMinHeight() && y < world.getMaxHeight()) {
            return snapshot.getBlockData(localX, y, localZ);
        }
        return world.getBlockAt(x, y, z).getBlockData();
    }

    private static StructureFootprint structureFootprintFor(Map<BlockVector3, BlockData> blocks,
                                                            int fallbackWidth,
                                                            int fallbackLength) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (BlockVector3 rel : blocks.keySet()) {
            minX = Math.min(minX, rel.getBlockX());
            maxX = Math.max(maxX, rel.getBlockX());
            minZ = Math.min(minZ, rel.getBlockZ());
            maxZ = Math.max(maxZ, rel.getBlockZ());
        }
        if (minX == Integer.MAX_VALUE) {
            return new StructureFootprint(0, fallbackWidth - 1, 0, fallbackLength - 1);
        }
        return new StructureFootprint(minX, maxX, minZ, maxZ);
    }

    private static Map<BlockFace, List<BlockVector3>> detectConnectorsFromMarkers(Set<BlockVector3> markers,
                                                                                   StructureFootprint footprint) {
        Map<BlockFace, List<BlockVector3>> bySide = new EnumMap<>(BlockFace.class);
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
            BlockFace nearestSide = nearestSide(center, footprint);
            bySide.get(nearestSide).add(projectConnectorToFootprintEdge(center, nearestSide, footprint));
        }

        Map<BlockFace, List<BlockVector3>> out = new EnumMap<>(BlockFace.class);
        for (Map.Entry<BlockFace, List<BlockVector3>> entry : bySide.entrySet()) {
            List<BlockVector3> points = entry.getValue();
            if (points.isEmpty()) {
                continue;
            }
            points.sort((a, b) -> {
                if (entry.getKey() == BlockFace.NORTH || entry.getKey() == BlockFace.SOUTH) {
                    return Integer.compare(a.getBlockX(), b.getBlockX());
                }
                return Integer.compare(a.getBlockZ(), b.getBlockZ());
            });
            out.put(entry.getKey(), points);
        }
        return out;
    }

    private static BlockVector3 projectConnectorToFootprintEdge(BlockVector3 point,
                                                                BlockFace side,
                                                                StructureFootprint footprint) {
        if (point == null || side == null || footprint == null) {
            return point;
        }
        int x = point.getBlockX();
        int y = point.getBlockY();
        int z = point.getBlockZ();
        return switch (side) {
            case NORTH -> BlockVector3.at(clamp(x, footprint.minX, footprint.maxX), y, footprint.minZ);
            case SOUTH -> BlockVector3.at(clamp(x, footprint.minX, footprint.maxX), y, footprint.maxZ);
            case EAST -> BlockVector3.at(footprint.maxX, y, clamp(z, footprint.minZ, footprint.maxZ));
            case WEST -> BlockVector3.at(footprint.minX, y, clamp(z, footprint.minZ, footprint.maxZ));
            default -> point;
        };
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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

    private static BlockFace nearestSide(BlockVector3 center, StructureFootprint footprint) {
        int westDist = Math.abs(center.getBlockX() - footprint.minX());
        int eastDist = Math.abs(footprint.maxX() - center.getBlockX());
        int northDist = Math.abs(center.getBlockZ() - footprint.minZ());
        int southDist = Math.abs(footprint.maxZ() - center.getBlockZ());
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

    private record StructureFootprint(int minX, int maxX, int minZ, int maxZ) {}

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
        BLOCK_DATA_ROTATION_CACHE.clear();
    }

    private static CapturedTemplates loadCapturedTemplates(boolean forceRefresh) {
        if (!forceRefresh && cachedCapturedTemplates != null) {
            return cachedCapturedTemplates;
        }
        clearRotationCache();
        Main plugin = Main.getInstance();
        if (plugin != null && plugin.getWorldManager() != null) {
            plugin.getWorldManager().ensureWorldsLoaded(SOURCE_WORLD);
        }
        World sourceWorld = Bukkit.getWorld(SOURCE_WORLD);
        if (sourceWorld == null) {
            return null;
        }
        loadSourceChunks(sourceWorld);
        CapturedTemplates captured = captureAllTemplates(sourceWorld);
        if (captured == null) {
            cachedCapturedTemplates = null;
            cachedTemplateConnectionInfo = null;
            return null;
        }
        cachedCapturedTemplates = captured;
        cachedTemplateConnectionInfo = Collections.unmodifiableMap(buildTemplateConnectionInfo(captured));
        return captured;
    }

    private static Map<String, TemplateConnectionInfo> buildTemplateConnectionInfo(CapturedTemplates captured) {
        Map<String, TemplateConnectionInfo> out = new LinkedHashMap<>();
        List<TemplateSpec> all = new ArrayList<>();
        all.addAll(captured.walls());
        all.addAll(captured.largeJunctions());
        all.addAll(captured.deadEnds());
        all.add(captured.connector());
        for (TemplateSpec spec : all) {
            if (spec == null || spec.template == null || spec.template.connectors == null
                    || spec.template.connectors.isEmpty()) {
                continue;
            }
            List<BlockFace> sides = new ArrayList<>(EnumSet.copyOf(spec.template.connectors.keySet()));
            int connectorCount = spec.template.connectors.values().stream().mapToInt(List::size).sum();
            out.put(spec.id, new TemplateConnectionInfo(connectorCount, sides));
        }
        return out;
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
        int normalizedRotation = Math.floorMod(rotation, 4);
        if (normalizedRotation == 0) {
            return source.clone();
        }
        String serialized = source.getAsString();
        BlockData[] cacheByRotation = BLOCK_DATA_ROTATION_CACHE.computeIfAbsent(serialized, ignored -> new BlockData[4]);
        BlockData cached = cacheByRotation[normalizedRotation];
        if (cached == null) {
            cached = source.clone();
            for (int i = 0; i < normalizedRotation; i++) {
                if (cached instanceof Directional directional) {
                    BlockFace current = directional.getFacing();
                    BlockFace next = rotateFace(current, 1);
                    if (directional.getFaces().contains(next)) {
                        directional.setFacing(next);
                    }
                }
                if (cached instanceof Rotatable rotatable) {
                    BlockFace current = rotatable.getRotation();
                    BlockFace next = rotateFace(current, 1);
                    rotatable.setRotation(next);
                }
                if (cached instanceof Orientable orientable) {
                    switch (orientable.getAxis()) {
                        case X -> orientable.setAxis(org.bukkit.Axis.Z);
                        case Z -> orientable.setAxis(org.bukkit.Axis.X);
                        default -> {
                        }
                    }
                }
            }
            cacheByRotation[normalizedRotation] = cached;
        }
        return cached.clone();
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

    private static int unpackPosX(long key) {
        return unpackSigned(key >> 38, 26);
    }

    private static int unpackPosY(long key) {
        return unpackSigned(key, 12);
    }

    private static int unpackPosZ(long key) {
        return unpackSigned(key >> 12, 26);
    }

    private static int unpackSigned(long value, int bits) {
        int shift = Long.SIZE - bits;
        return (int) (value << shift >> shift);
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
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

    private record UsageRule(Predicate<TemplateSpec> matcher, int targetCount, double bonusPerMissing) {
    }

    private record ConnectorRequirementRule(Predicate<TemplateSpec> left, Predicate<TemplateSpec> right) {
        private static ConnectorRequirementRule symmetric(Predicate<TemplateSpec> a, Predicate<TemplateSpec> b) {
            return new ConnectorRequirementRule(a, b);
        }

        private boolean matches(TemplateSpec from, TemplateSpec to) {
            return (left.test(from) && right.test(to)) || (left.test(to) && right.test(from));
        }
    }

    private record PlacementAttempt(PlacedTemplate connector, PlacedTemplate placed) {
    }

    private record ExpansionChoice(BlockFace side, PlacementAttempt attempt, double score) {
    }

    private record LeastOverlapChoice(PlacedTemplate source, BlockFace side, PlacementAttempt attempt, double overlap) {
    }

    private static final class GenerationDiagnostics {
        private int spineBlockedSides;
        private int branchBlockedSides;
        private int requiredPlacementsForced;
        private int requiredLeastOverlapPlaced;
        private boolean churchLeastOverlapPlaced;
        private boolean satelliteChurchPlaced;
        private boolean churchEmergencyPlaced;
        private int requiredEmergencyPlaced;
        private boolean churchRawCopied;
        private int requiredRawCopied;
        private int satelliteLinkSegments;
        private int rejectedWallPacing;
        private int rejectedLargeSpacing;
        private String templateConnectorSummary = "";
    }

    private enum ValidationReason {
        NONE,
        INVALID_ATTEMPT,
        CONNECTOR_REQUIRED,
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

    private record SourceSetup(World sourceWorld, CapturedTemplates captured) {
    }

    private record PlacementState(int wallPiecesSinceLarge, int wallPiecesSinceGate, int wallPiecesSinceTower) {
        private static PlacementState initial() {
            return new PlacementState(
                    MIN_WALL_PIECES_BETWEEN_LARGE,
                    MIN_WALL_PIECES_BETWEEN_GATES,
                    MIN_WALL_PIECES_BETWEEN_TOWERS
            );
        }

        private static PlacementState fromSeed(TemplateSpec seed) {
            int smallCount = isLarge(seed) ? 0 : (isWall(seed) ? MIN_WALL_PIECES_BETWEEN_LARGE : 0);
            int gateCount = isGate(seed) ? 0 : MIN_WALL_PIECES_BETWEEN_GATES;
            int towerCount = isTower(seed) ? 0 : MIN_WALL_PIECES_BETWEEN_TOWERS;
            return new PlacementState(smallCount, gateCount, towerCount);
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
            int nextWallsSinceTower = isTower(placed)
                    ? 0
                    : (isWall(placed)
                    ? Math.min(MIN_WALL_PIECES_BETWEEN_TOWERS + 1, wallPiecesSinceTower + 1)
                    : wallPiecesSinceTower);
            return new PlacementState(nextSmallSinceLarge, nextWallsSinceGate, nextWallsSinceTower);
        }
    }

    private record Bounds2D(int minX, int maxX, int minZ, int maxZ) {
    }

    private record Bounds3D(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
    }

    private record Point2D(double x, double z) {
    }

    private record SatellitePlacementResult(boolean placed, int linkSegments) {
        private static SatellitePlacementResult none() {
            return new SatellitePlacementResult(false, 0);
        }
    }

    private static final class PlacedTemplate {
        private final TemplateSpec spec;
        private final int rotation;
        private final BlockVector3 origin;
        private final Map<BlockFace, Integer> usedConnectorCounts = new EnumMap<>(BlockFace.class);
        private BlockFace incomingSide;
        private Bounds2D cachedBounds2D;
        private Bounds3D cachedBounds3D;

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

        private Bounds2D bounds2D() {
            if (cachedBounds2D == null) {
                computeBounds();
            }
            return cachedBounds2D;
        }

        private Bounds3D bounds3D() {
            if (cachedBounds3D == null) {
                computeBounds();
            }
            return cachedBounds3D;
        }

        private void computeBounds() {
            RotatedTemplate rotated = rotateTemplate(spec.template, rotation);
            if (rotated == null || rotated.blocks.isEmpty()) {
                cachedBounds2D = null;
                cachedBounds3D = null;
                return;
            }
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (BlockVector3 rel : rotated.blocks.keySet()) {
                int x = origin.getBlockX() + rel.getBlockX();
                int y = origin.getBlockY() + rel.getBlockY();
                int z = origin.getBlockZ() + rel.getBlockZ();
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);
            }
            cachedBounds2D = new Bounds2D(minX, maxX, minZ, maxZ);
            cachedBounds3D = new Bounds3D(minX, maxX, minY, maxY, minZ, maxZ);
        }
    }

}
