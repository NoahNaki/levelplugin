package me.nakilex.levelplugin.debug;

import com.sk89q.worldedit.math.BlockVector3;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
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

    private static final int DEFAULT_SPINE_LENGTH = 8;
    private static final int MAX_BRANCH_LENGTH = 5;
    private static final int MIN_SMALL_PIECES_BETWEEN_LARGE = 1;
    private static final int MIN_WALL_PIECES_BETWEEN_GATES = 3;
    private static final double BRANCH_OPEN_SIDE_CHANCE = 0.70D;

    private static double maxOverlapPercent = 2.0D;

    private StrongholdDebugGenerator() {
    }

    public static double getMaxOverlapPercent() {
        return maxOverlapPercent;
    }

    public static void setMaxOverlapPercent(double value) {
        maxOverlapPercent = Math.max(0.0D, Math.min(100.0D, value));
    }

    public static boolean generateTest(Player player) {
        if (player == null) {
            return false;
        }

        World world = player.getWorld();
        Random random = ThreadLocalRandom.current();

        CapturedTemplates captured = captureAllTemplates(world);
        if (captured == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Failed to capture one or more stronghold templates. Check source cuboids and markers.");
            return true;
        }

        int originX = player.getLocation().getBlockX() + 3;
        int originY = player.getLocation().getBlockY();
        int originZ = player.getLocation().getBlockZ() + 3;

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
            BlockFace side = pickOpenSide(spineHead, null, random);
            if (side == null) {
                break;
            }

            List<TemplateSpec> pool = candidatePoolForStep(captured, spineHead, spineState);

            if (pool.isEmpty()) {
                break;
            }

            PlacedTemplate next = tryPlaceFromSide(spineHead, side, pool, captured.connector(), occupied, random);
            if (next == null) {
                spineHead.markUsed(side);
                continue;
            }

            spineState = spineState.onPlaced(next.spec);
            placed.add(next);
            occupy(occupied, next);
            spineHead = next;
        }

        closeOpenSideWithDeadEnd(spineHead, captured, occupied, random, placed);

        List<PlacedTemplate> branchSeeds = new ArrayList<>(placed);
        for (PlacedTemplate seed : branchSeeds) {
            growBranches(seed, captured, occupied, random, placed);
        }

        for (PlacedTemplate entry : placed) {
            paste(world, entry.spec.template, entry.origin, entry.rotation);
        }

        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Generated stronghold spine+branches using " + placed.size()
                        + " pieces (overlap threshold: " + String.format("%.2f", maxOverlapPercent) + "%).");
        return true;
    }

    private static void growBranches(PlacedTemplate seed,
                                     CapturedTemplates captured,
                                     Set<Long> occupied,
                                     Random random,
                                     List<PlacedTemplate> placed) {
        List<BlockFace> openSides = seed.openSides();
        Collections.shuffle(openSides, random);

        for (BlockFace side : openSides) {
            if (random.nextDouble() > BRANCH_OPEN_SIDE_CHANCE) {
                continue;
            }

            PlacedTemplate branchCurrent = seed;
            BlockFace branchSide = side;
            PlacementState branchState = PlacementState.fromSeed(seed.spec);

            int segments = 1 + random.nextInt(MAX_BRANCH_LENGTH);
            for (int i = 0; i < segments; i++) {
                List<TemplateSpec> pool = candidatePoolForStep(captured, branchCurrent, branchState, i > 0);

                PlacedTemplate next = tryPlaceFromSide(branchCurrent, branchSide, pool, captured.connector(), occupied, random);
                if (next == null) {
                    branchCurrent.markUsed(branchSide);
                    break;
                }

                branchState = branchState.onPlaced(next.spec);
                placed.add(next);
                occupy(occupied, next);
                branchCurrent = next;

                BlockFace avoid = next.incomingSide;
                branchSide = pickOpenSide(next, avoid, random);
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

        PlacedTemplate deadEnd = tryPlaceFromSide(target, side, captured.deadEnds(), null, occupied, random);
        if (deadEnd == null) {
            target.markUsed(side);
            return;
        }

        placed.add(deadEnd);
        occupy(occupied, deadEnd);
    }

    private static PlacedTemplate tryPlaceFromSide(PlacedTemplate current,
                                                   BlockFace currentSide,
                                                   List<TemplateSpec> candidateSpecs,
                                                   TemplateSpec connector,
                                                   Set<Long> occupied,
                                                   Random random) {
        if (candidateSpecs == null || candidateSpecs.isEmpty()) {
            return null;
        }

        List<TemplateSpec> shuffled = weightedShuffle(candidateSpecs, random);

        PlacedTemplate direct = tryPlaceSingle(current, currentSide, shuffled, occupied, random, true);
        if (direct != null && !areBothLarge(current.spec, direct.spec)) {
            current.markUsed(currentSide);
            return direct;
        }
        return null;
    }

    private static PlacedTemplate tryPlaceSingle(PlacedTemplate current,
                                                 BlockFace currentSide,
                                                 List<TemplateSpec> candidateSpecs,
                                                 Set<Long> occupied,
                                                 Random random,
                                                 boolean enforceOverlap) {
        RotatedTemplate currentRotated = rotateTemplate(current.spec.template, current.rotation);
        BlockVector3 currentConnector = currentRotated.connectors.get(currentSide);
        if (currentConnector == null) {
            return null;
        }
        BlockVector3 worldConnector = current.origin.add(currentConnector);

        for (TemplateSpec spec : candidateSpecs) {
            for (int rot = 0; rot < 4; rot++) {
                RotatedTemplate rotated = rotateTemplate(spec.template, rot);
                for (Map.Entry<BlockFace, BlockVector3> entry : rotated.connectors.entrySet()) {
                    if (entry.getKey() != opposite(currentSide)) {
                        continue;
                    }
                    BlockVector3 origin = worldConnector.subtract(entry.getValue());
                    origin = slideUntilThreshold(occupied, rotated.blocks, origin, currentSide);
                    if (enforceOverlap && overlapPercent(occupied, rotated.blocks, origin) > maxOverlapPercent) {
                        continue;
                    }
                    PlacedTemplate placed = new PlacedTemplate(spec, rot, origin);
                    placed.incomingSide = entry.getKey();
                    placed.markUsed(entry.getKey());
                    return placed;
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
        return !isLarge(current.spec) && state.smallPiecesSinceLarge >= MIN_SMALL_PIECES_BETWEEN_LARGE;
    }

    private static boolean canPlaceGate(PlacementState state) {
        return state.wallPiecesSinceGate >= MIN_WALL_PIECES_BETWEEN_GATES;
    }

    private static boolean canUseSpec(TemplateSpec spec, PlacementState state) {
        return !isGate(spec) || canPlaceGate(state);
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
        Map<BlockFace, List<BlockVector3>> markersBySide = new EnumMap<>(BlockFace.class);
        for (BlockFace face : List.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)) {
            markersBySide.put(face, new ArrayList<>());
        }

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
                        assignSideMarker(markersBySide, relX, relZ, width, length, rel);
                    }

                    if (type.isAir() || EXCLUDED.contains(type)) {
                        continue;
                    }
                    blocks.put(rel, data);
                }
            }
        }

        Map<BlockFace, BlockVector3> connectors = new EnumMap<>(BlockFace.class);
        for (Map.Entry<BlockFace, List<BlockVector3>> entry : markersBySide.entrySet()) {
            BlockVector3 center = centerOf(entry.getValue());
            if (center != null) {
                connectors.put(entry.getKey(), center);
            }
        }

        return new Template(blocks, connectors, width, height, length);
    }

    private static void assignSideMarker(Map<BlockFace, List<BlockVector3>> markersBySide,
                                         int relX, int relZ, int width, int length, BlockVector3 rel) {
        int westDist = relX;
        int eastDist = (width - 1) - relX;
        int northDist = relZ;
        int southDist = (length - 1) - relZ;

        int min = Math.min(Math.min(westDist, eastDist), Math.min(northDist, southDist));
        BlockFace side;
        if (westDist == min) {
            side = BlockFace.WEST;
        } else if (eastDist == min) {
            side = BlockFace.EAST;
        } else if (northDist == min) {
            side = BlockFace.NORTH;
        } else {
            side = BlockFace.SOUTH;
        }
        markersBySide.get(side).add(rel);
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
        Map<BlockFace, BlockVector3> conn = new EnumMap<>(BlockFace.class);
        for (Map.Entry<BlockFace, BlockVector3> e : template.connectors.entrySet()) {
            conn.put(rotateFace(e.getKey(), rot), rotateVector(e.getValue(), template.width, template.length, rot));
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
                            Map<BlockFace, BlockVector3> connectors,
                            int width,
                            int height,
                            int length) {
    }

    private record RotatedTemplate(Map<BlockVector3, BlockData> blocks,
                                   Map<BlockFace, BlockVector3> connectors) {
    }

    private record CapturedTemplates(List<TemplateSpec> walls,
                                     List<TemplateSpec> largeJunctions,
                                     List<TemplateSpec> deadEnds,
                                     TemplateSpec connector) {
    }

    private record WeightedSpec(TemplateSpec spec, double key) {
    }

    private record PlacementState(int smallPiecesSinceLarge, int wallPiecesSinceGate) {
        private static PlacementState initial() {
            return new PlacementState(MIN_SMALL_PIECES_BETWEEN_LARGE, MIN_WALL_PIECES_BETWEEN_GATES);
        }

        private static PlacementState fromSeed(TemplateSpec seed) {
            int smallCount = isLarge(seed) ? 0 : MIN_SMALL_PIECES_BETWEEN_LARGE;
            int gateCount = isGate(seed) ? 0 : MIN_WALL_PIECES_BETWEEN_GATES;
            return new PlacementState(smallCount, gateCount);
        }

        private PlacementState onPlaced(TemplateSpec placed) {
            int nextSmallSinceLarge = isLarge(placed)
                    ? 0
                    : Math.min(MIN_SMALL_PIECES_BETWEEN_LARGE + 1, smallPiecesSinceLarge + 1);
            int nextWallsSinceGate = isGate(placed)
                    ? 0
                    : (isWall(placed)
                    ? Math.min(MIN_WALL_PIECES_BETWEEN_GATES + 1, wallPiecesSinceGate + 1)
                    : wallPiecesSinceGate);
            return new PlacementState(nextSmallSinceLarge, nextWallsSinceGate);
        }
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
