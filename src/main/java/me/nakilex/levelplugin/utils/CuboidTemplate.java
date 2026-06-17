package me.nakilex.levelplugin.utils;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.util.BoundingBox;

/**
 * Reusable in-memory block template captured from a cuboid selection.
 *
 * <p>This keeps systems that need dungeon/stronghold-style area templates from
 * having to create temporary schematic files just to copy a selected region.</p>
 */
public final class CuboidTemplate {
    private final String sourceWorldName;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int width;
    private final int height;
    private final int depth;
    private final List<BlockCopy> blocks;
    private final List<NexoBlockCopy> nexoBlocks;
    private final List<NexoFurnitureCopy> nexoFurniture;

    private CuboidTemplate(String sourceWorldName,
                           int minX,
                           int minY,
                           int minZ,
                           int width,
                           int height,
                           int depth,
                           List<BlockCopy> blocks,
                           List<NexoBlockCopy> nexoBlocks,
                           List<NexoFurnitureCopy> nexoFurniture) {
        this.sourceWorldName = sourceWorldName;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.blocks = Collections.unmodifiableList(new ArrayList<>(blocks));
        this.nexoBlocks = Collections.unmodifiableList(new ArrayList<>(nexoBlocks == null ? List.of() : nexoBlocks));
        this.nexoFurniture = Collections.unmodifiableList(new ArrayList<>(nexoFurniture == null ? List.of() : nexoFurniture));
    }

    public static CuboidTemplate capture(Location pos1, Location pos2) {
        return capture(pos1, pos2, true);
    }

    public static CuboidTemplate capture(Location pos1, Location pos2, boolean skipAir) {
        return capture(pos1, pos2, skipAir, Set.of());
    }

    public static CuboidTemplate capture(Location pos1, Location pos2, boolean skipAir, Set<Material> excludedMaterials) {
        Objects.requireNonNull(pos1, "pos1");
        Objects.requireNonNull(pos2, "pos2");
        World world = pos1.getWorld();
        if (world == null || pos2.getWorld() == null || !world.equals(pos2.getWorld())) {
            throw new IllegalArgumentException("Cuboid positions must be in the same loaded world.");
        }

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        Set<Material> excluded = excludedMaterials == null ? Set.of() : excludedMaterials;
        List<BlockCopy> blocks = new ArrayList<>();
        List<NexoBlockCopy> nexoBlocks = new ArrayList<>();
        List<NexoFurnitureCopy> nexoFurniture = new ArrayList<>();
        Set<UUID> capturedFurnitureEntities = new HashSet<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material material = block.getType();
                    if (!((skipAir && material == Material.AIR) || excluded.contains(material))) {
                        blocks.add(new BlockCopy(x - minX, y - minY, z - minZ, block.getBlockData()));
                    }

                    captureNexoBlock(block, minX, minY, minZ).ifPresent(nexoBlocks::add);
                    captureNexoFurniture(block, minX, minY, minZ, capturedFurnitureEntities).ifPresent(nexoFurniture::add);
                }
            }
        }

        BoundingBox bounds = new BoundingBox(minX, minY, minZ, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D);
        for (Entity entity : world.getNearbyEntities(bounds)) {
            if (entity instanceof ItemDisplay display) {
                captureNexoFurniture(display, minX, minY, minZ, capturedFurnitureEntities).ifPresent(nexoFurniture::add);
            }
        }

        return new CuboidTemplate(
                world.getName(),
                minX,
                minY,
                minZ,
                maxX - minX + 1,
                maxY - minY + 1,
                maxZ - minZ + 1,
                blocks,
                nexoBlocks,
                nexoFurniture);
    }

    public void paste(World world, int baseX, int baseY, int baseZ) {
        if (world == null) {
            return;
        }
        for (BlockCopy block : blocks) {
            world.getBlockAt(baseX + block.x(), baseY + block.y(), baseZ + block.z())
                    .setBlockData(block.data(), false);
        }
        for (NexoBlockCopy block : nexoBlocks) {
            Location target = new Location(world, baseX + block.x(), baseY + block.y(), baseZ + block.z());
            NexoBlocks.place(block.itemId(), target);
        }
        for (NexoFurnitureCopy furniture : nexoFurniture) {
            Location target = new Location(world,
                    baseX + furniture.x(),
                    baseY + furniture.y(),
                    baseZ + furniture.z(),
                    furniture.yaw(),
                    0.0F);
            NexoFurniture.remove(target);
            NexoFurniture.place(furniture.itemId(), target, furniture.yaw(), furniture.facing());
        }
    }

    public Optional<BlockCopy> firstBlock(Material material) {
        if (material == null) {
            return Optional.empty();
        }
        for (BlockCopy block : blocks) {
            if (block.data().getMaterial() == material) {
                return Optional.of(block);
            }
        }
        return Optional.empty();
    }

    public CuboidTemplate without(Material material) {
        if (material == null) {
            return this;
        }
        List<BlockCopy> filtered = new ArrayList<>();
        for (BlockCopy block : blocks) {
            if (block.data().getMaterial() != material) {
                filtered.add(block);
            }
        }
        return new CuboidTemplate(sourceWorldName, minX, minY, minZ, width, height, depth, filtered, nexoBlocks, nexoFurniture);
    }

    public String sourceWorldName() {
        return sourceWorldName;
    }

    public int minX() {
        return minX;
    }

    public int minY() {
        return minY;
    }

    public int minZ() {
        return minZ;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int depth() {
        return depth;
    }

    public List<BlockCopy> blocks() {
        return blocks;
    }

    public List<NexoBlockCopy> nexoBlocks() {
        return nexoBlocks;
    }

    public List<NexoFurnitureCopy> nexoFurniture() {
        return nexoFurniture;
    }

    private static Optional<NexoBlockCopy> captureNexoBlock(Block block, int minX, int minY, int minZ) {
        if (block == null) {
            return Optional.empty();
        }
        CustomBlockMechanic mechanic = NexoBlocks.customBlockMechanic(block);
        if (mechanic == null || mechanic.getItemID() == null || mechanic.getItemID().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new NexoBlockCopy(
                block.getX() - minX,
                block.getY() - minY,
                block.getZ() - minZ,
                mechanic.getItemID()));
    }

    private static Optional<NexoFurnitureCopy> captureNexoFurniture(Block block,
                                                                    int minX,
                                                                    int minY,
                                                                    int minZ,
                                                                    Set<UUID> capturedFurnitureEntities) {
        if (block == null) {
            return Optional.empty();
        }
        return captureNexoFurniture(NexoFurniture.baseEntity(block), minX, minY, minZ, capturedFurnitureEntities);
    }

    private static Optional<NexoFurnitureCopy> captureNexoFurniture(ItemDisplay baseEntity,
                                                                    int minX,
                                                                    int minY,
                                                                    int minZ,
                                                                    Set<UUID> capturedFurnitureEntities) {
        if (baseEntity == null || !capturedFurnitureEntities.add(baseEntity.getUniqueId())) {
            return Optional.empty();
        }
        FurnitureMechanic mechanic = NexoFurniture.furnitureMechanic(baseEntity);
        if (mechanic == null || mechanic.getItemID() == null || mechanic.getItemID().isBlank()) {
            return Optional.empty();
        }
        Location location = baseEntity.getLocation();
        return Optional.of(new NexoFurnitureCopy(
                location.getX() - minX,
                location.getY() - minY,
                location.getZ() - minZ,
                mechanic.getItemID(),
                location.getYaw(),
                yawToFace(location.getYaw())));
    }

    private static BlockFace yawToFace(float yaw) {
        float normalized = ((yaw % 360.0F) + 360.0F) % 360.0F;
        if (normalized >= 45.0F && normalized < 135.0F) {
            return BlockFace.WEST;
        }
        if (normalized >= 135.0F && normalized < 225.0F) {
            return BlockFace.NORTH;
        }
        if (normalized >= 225.0F && normalized < 315.0F) {
            return BlockFace.EAST;
        }
        return BlockFace.SOUTH;
    }

    public record BlockCopy(int x, int y, int z, BlockData data) { }

    public record NexoBlockCopy(int x, int y, int z, String itemId) { }

    public record NexoFurnitureCopy(double x, double y, double z, String itemId, float yaw, BlockFace facing) { }
}
