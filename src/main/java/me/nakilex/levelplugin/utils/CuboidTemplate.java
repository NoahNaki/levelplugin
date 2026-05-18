package me.nakilex.levelplugin.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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

    private CuboidTemplate(String sourceWorldName,
                           int minX,
                           int minY,
                           int minZ,
                           int width,
                           int height,
                           int depth,
                           List<BlockCopy> blocks) {
        this.sourceWorldName = sourceWorldName;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.blocks = Collections.unmodifiableList(new ArrayList<>(blocks));
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
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material material = block.getType();
                    if ((skipAir && material == Material.AIR) || excluded.contains(material)) {
                        continue;
                    }
                    blocks.add(new BlockCopy(x - minX, y - minY, z - minZ, block.getBlockData()));
                }
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
                blocks);
    }

    public void paste(World world, int baseX, int baseY, int baseZ) {
        if (world == null) {
            return;
        }
        for (BlockCopy block : blocks) {
            world.getBlockAt(baseX + block.x(), baseY + block.y(), baseZ + block.z())
                    .setBlockData(block.data(), false);
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
        return new CuboidTemplate(sourceWorldName, minX, minY, minZ, width, height, depth, filtered);
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

    public record BlockCopy(int x, int y, int z, BlockData data) { }
}
