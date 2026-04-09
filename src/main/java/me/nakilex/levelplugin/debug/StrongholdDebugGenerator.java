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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Debug-only stronghold wall composer. Captures two source cuboids, strips
 * scaffold marker materials, and pastes them connected by parsed connector
 * sides from redstone marker blocks.
 */
public final class StrongholdDebugGenerator {
    private static final Set<Material> EXCLUDED = Set.of(
            Material.REDSTONE_BLOCK,
            Material.LIGHT_BLUE_CONCRETE,
            Material.WHITE_CONCRETE
    );

    private static final TemplateBounds STRAIGHT_1 = new TemplateBounds(402, -61, -5346, 472, -38, -5276);
    private static final TemplateBounds CONNECTOR_1 = new TemplateBounds(402, -61, -5711, 412, -38, -5701);
    private StrongholdDebugGenerator() {
    }

    public static boolean generateTest(Player player) {
        if (player == null) {
            return false;
        }
        World world = player.getWorld();
        Template straight = captureTemplate(world, STRAIGHT_1);
        Template connector = captureTemplate(world, CONNECTOR_1);
        if (straight.blocks.isEmpty() || connector.blocks.isEmpty()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Unable to capture stronghold templates from source cuboids.");
            return true;
        }
        if (straight.connectors.isEmpty() || connector.connectors.isEmpty()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Connector markers were not found. Make sure redstone marker blocks exist in source cuboids.");
            return true;
        }

        int originX = player.getLocation().getBlockX() + 3;
        int originY = player.getLocation().getBlockY();
        int originZ = player.getLocation().getBlockZ() + 3;
        PlacedTemplate current = new PlacedTemplate(straight, 0, BlockVector3.at(originX, originY, originZ), null);
        List<PlacedTemplate> placements = new ArrayList<>();
        placements.add(current);

        List<Template> sequence = List.of(connector, straight, connector, straight);
        for (Template nextTemplate : sequence) {
            BlockFace preferredSide = current.incomingSide == null ? BlockFace.EAST : opposite(current.incomingSide);
            PlacementResult next = findBestPlacement(current, nextTemplate, preferredSide);
            if (next == null) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                        "Failed to compute a valid connector alignment for stronghold templates.");
                return true;
            }
            current = new PlacedTemplate(nextTemplate, next.rotation, next.origin, next.nextIncomingSide);
            placements.add(current);
        }

        for (PlacedTemplate placed : placements) {
            paste(world, placed.template, placed.origin, placed.rotation);
        }

        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Generated stronghold test with connected walls (filtered redstone/blue/white markers).");
        return true;
    }

    private static PlacementResult findBestPlacement(PlacedTemplate current,
                                                     Template next,
                                                     BlockFace preferredCurrentSide) {
        RotatedTemplate currentRotated = rotateTemplate(current.template, current.rotation);
        for (Map.Entry<BlockFace, BlockVector3> a : currentRotated.connectors.entrySet()) {
            BlockFace aSide = a.getKey();
            if (preferredCurrentSide != null && aSide != preferredCurrentSide) {
                continue;
            }
            BlockVector3 worldConnectorA = current.origin.add(a.getValue());
            for (int rot = 0; rot < 4; rot++) {
                RotatedTemplate rotated = rotateTemplate(next, rot);
                for (Map.Entry<BlockFace, BlockVector3> b : rotated.connectors.entrySet()) {
                    BlockFace bSide = b.getKey();
                    if (aSide != opposite(bSide)) {
                        continue;
                    }
                    BlockVector3 bOrigin = worldConnectorA.subtract(b.getValue());
                    bOrigin = slideUntilCollision(currentRotated.blocks, current.origin, rotated.blocks, bOrigin, aSide);
                    return new PlacementResult(bOrigin, rot, bSide);
                }
            }
        }
        if (preferredCurrentSide != null) {
            return findBestPlacement(current, next, null);
        }
        return null;
    }

    private static BlockVector3 slideUntilCollision(Map<BlockVector3, BlockData> fixedBlocks,
                                                    BlockVector3 fixedOrigin,
                                                    Map<BlockVector3, BlockData> movingBlocks,
                                                    BlockVector3 startOrigin,
                                                    BlockFace joinSide) {
        Set<Long> straightPositions = absoluteBlockPositions(fixedBlocks, fixedOrigin);
        int towardX = -joinSide.getModX();
        int towardZ = -joinSide.getModZ();
        int awayX = joinSide.getModX();
        int awayZ = joinSide.getModZ();

        BlockVector3 current = startOrigin;
        // If we happen to start in collision, back away until we are not.
        for (int i = 0; i < 64 && hasOverlap(straightPositions, movingBlocks, current); i++) {
            current = current.add(awayX, 0, awayZ);
        }

        // Move inward until the next step would overlap.
        for (int i = 0; i < 256; i++) {
            BlockVector3 next = current.add(towardX, 0, towardZ);
            if (hasOverlap(straightPositions, movingBlocks, next)) {
                break;
            }
            current = next;
        }
        return current;
    }

    private static boolean hasOverlap(Set<Long> straightPositions,
                                      Map<BlockVector3, BlockData> rotatedBlocks,
                                      BlockVector3 rotatedOrigin) {
        for (BlockVector3 rel : rotatedBlocks.keySet()) {
            int x = rotatedOrigin.getBlockX() + rel.getBlockX();
            int y = rotatedOrigin.getBlockY() + rel.getBlockY();
            int z = rotatedOrigin.getBlockZ() + rel.getBlockZ();
            if (straightPositions.contains(posKey(x, y, z))) {
                return true;
            }
        }
        return false;
    }

    private static Set<Long> absoluteBlockPositions(Map<BlockVector3, BlockData> blocks, BlockVector3 origin) {
        Set<Long> set = new java.util.HashSet<>();
        for (BlockVector3 rel : blocks.keySet()) {
            int x = origin.getBlockX() + rel.getBlockX();
            int y = origin.getBlockY() + rel.getBlockY();
            int z = origin.getBlockZ() + rel.getBlockZ();
            set.add(posKey(x, y, z));
        }
        return set;
    }

    private static long posKey(int x, int y, int z) {
        long lx = ((long) x & 0x3FFFFFFL) << 38;
        long lz = ((long) z & 0x3FFFFFFL) << 12;
        long ly = (long) y & 0xFFFL;
        return lx | lz | ly;
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
        for (Map.Entry<BlockFace, List<BlockVector3>> e : markersBySide.entrySet()) {
            BlockVector3 center = centerOf(e.getValue());
            if (center != null) {
                connectors.put(e.getKey(), center);
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
        int cx = (int) Math.round(sx / points.size());
        int cy = (int) Math.round(sy / points.size());
        int cz = (int) Math.round(sz / points.size());
        return BlockVector3.at(cx, cy, cz);
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

    private record TemplateBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {}

    private record Template(Map<BlockVector3, BlockData> blocks,
                            Map<BlockFace, BlockVector3> connectors,
                            int width,
                            int height,
                            int length) {}

    private record RotatedTemplate(Map<BlockVector3, BlockData> blocks,
                                   Map<BlockFace, BlockVector3> connectors) {}

    private record PlacementResult(BlockVector3 origin, int rotation, BlockFace nextIncomingSide) {}

    private record PlacedTemplate(Template template, int rotation, BlockVector3 origin, BlockFace incomingSide) {}
}
