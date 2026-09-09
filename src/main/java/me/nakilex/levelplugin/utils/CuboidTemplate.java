package me.nakilex.levelplugin.utils;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Reusable in-memory block template captured from a cuboid selection.
 *
 * <p>This keeps systems that need dungeon/stronghold-style area templates from
 * having to create temporary schematic files just to copy a selected region.</p>
 *
 * <p>Blocks are stored as a palette of distinct {@link BlockData} plus parallel
 * primitive arrays, so a template holds a handful of block states rather than one
 * object per captured block. Capture reads {@link ChunkSnapshot}s instead of live
 * {@link Block} handles, which lets the scan run off the main thread.</p>
 */
public final class CuboidTemplate {
    /**
     * The only vanilla blocks Nexo can back a custom block with (its custom_block
     * mechanics are noteblock, stringblock and chorusblock). Every other material is
     * definitionally not a Nexo block, so the expensive registry lookup is skipped.
     */
    private static final Set<Material> NEXO_BLOCK_CANDIDATES = Set.of(
            Material.NOTE_BLOCK,
            Material.TRIPWIRE,
            Material.CHORUS_PLANT,
            Material.CHORUS_FLOWER);

    /** Concurrent async chunk loads; the disk I/O is off-thread, so this can be generous. */
    private static final int CHUNK_LOADS_IN_FLIGHT = 16;
    private static final int PASTE_BLOCKS_PER_TICK = 20_000;

    private final String sourceWorldName;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int width;
    private final int height;
    private final int depth;
    private final BlockData[] palette;
    private final int[] positions;
    private final int[] paletteIds;
    private final int blockCount;
    private final List<NexoBlockCopy> nexoBlocks;
    private final List<NexoFurnitureCopy> nexoFurniture;

    private CuboidTemplate(String sourceWorldName,
                           int minX,
                           int minY,
                           int minZ,
                           int width,
                           int height,
                           int depth,
                           BlockData[] palette,
                           int[] positions,
                           int[] paletteIds,
                           int blockCount,
                           List<NexoBlockCopy> nexoBlocks,
                           List<NexoFurnitureCopy> nexoFurniture) {
        this.sourceWorldName = sourceWorldName;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.palette = palette;
        this.positions = positions;
        this.paletteIds = paletteIds;
        this.blockCount = blockCount;
        this.nexoBlocks = Collections.unmodifiableList(new ArrayList<>(nexoBlocks == null ? List.of() : nexoBlocks));
        this.nexoFurniture = Collections.unmodifiableList(new ArrayList<>(nexoFurniture == null ? List.of() : nexoFurniture));
    }

    public static CuboidTemplate capture(Location pos1, Location pos2) {
        return capture(pos1, pos2, true);
    }

    public static CuboidTemplate capture(Location pos1, Location pos2, boolean skipAir) {
        return capture(pos1, pos2, skipAir, Set.of());
    }

    /**
     * Captures synchronously. Suitable for player-sized selections; for large regions
     * prefer {@link #captureAsync} so the scan does not stall the server thread.
     */
    public static CuboidTemplate capture(Location pos1, Location pos2, boolean skipAir, Set<Material> excludedMaterials) {
        Bounds bounds = Bounds.of(pos1, pos2);
        World world = bounds.world();
        List<SnapshotEntry> snapshots = new ArrayList<>();
        for (int cx = bounds.minX() >> 4; cx <= bounds.maxX() >> 4; cx++) {
            for (int cz = bounds.minZ() >> 4; cz <= bounds.maxZ() >> 4; cz++) {
                snapshots.add(new SnapshotEntry(cx, cz, world.getChunkAt(cx, cz).getChunkSnapshot()));
            }
        }
        ScanResult scan = scan(bounds, snapshots, skipAir, excludedMaterials);
        return finish(world, bounds, scan);
    }

    /**
     * Captures without blocking the server thread.
     *
     * <p>Chunk snapshots are taken on the main thread a few dozen per tick, the block
     * scan runs on a worker thread, and the small amount of work that genuinely needs
     * the server thread (Nexo custom-block resolution and the furniture entity sweep)
     * happens in a single short pass at the end.</p>
     */
    public static CompletableFuture<CuboidTemplate> captureAsync(Plugin plugin,
                                                                 Location pos1,
                                                                 Location pos2,
                                                                 boolean skipAir,
                                                                 Set<Material> excludedMaterials) {
        Objects.requireNonNull(plugin, "plugin");
        Bounds bounds = Bounds.of(pos1, pos2);
        World world = bounds.world();
        Set<Material> excluded = excludedMaterials == null ? Set.of() : Set.copyOf(excludedMaterials);

        CompletableFuture<CuboidTemplate> result = new CompletableFuture<>();
        List<int[]> chunkKeys = new ArrayList<>();
        for (int cx = bounds.minX() >> 4; cx <= bounds.maxX() >> 4; cx++) {
            for (int cz = bounds.minZ() >> 4; cz <= bounds.maxZ() >> 4; cz++) {
                chunkKeys.add(new int[]{cx, cz});
            }
        }

        List<SnapshotEntry> snapshots = new ArrayList<>(chunkKeys.size());
        snapshotChunks(world, chunkKeys, snapshots, result, () ->
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    ScanResult scan;
                    try {
                        scan = scan(bounds, snapshots, skipAir, excluded);
                    } catch (Throwable throwable) {
                        result.completeExceptionally(throwable);
                        return;
                    }
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            result.complete(finish(world, bounds, scan));
                        } catch (Throwable throwable) {
                            result.completeExceptionally(throwable);
                        }
                    });
                }));

        return result;
    }

    /**
     * Loads every chunk in the region via Paper's async chunk loader, keeping a bounded
     * number of loads in flight, and snapshots each one as it arrives. The disk I/O
     * happens off the server thread; only the snapshot itself runs on it.
     */
    private static void snapshotChunks(World world,
                                       List<int[]> chunkKeys,
                                       List<SnapshotEntry> out,
                                       CompletableFuture<?> failureSink,
                                       Runnable onDone) {
        if (chunkKeys.isEmpty()) {
            onDone.run();
            return;
        }
        java.util.concurrent.atomic.AtomicInteger next = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger completed = new java.util.concurrent.atomic.AtomicInteger();
        Runnable[] pump = new Runnable[1];
        pump[0] = () -> {
            int index = next.getAndIncrement();
            if (index >= chunkKeys.size()) {
                return;
            }
            int cx = chunkKeys.get(index)[0];
            int cz = chunkKeys.get(index)[1];
            world.getChunkAtAsync(cx, cz).whenComplete((chunk, error) -> {
                if (error != null) {
                    failureSink.completeExceptionally(error);
                    return;
                }
                try {
                    // Completed on the server thread by Paper, so this snapshot is safe.
                    out.add(new SnapshotEntry(cx, cz, chunk.getChunkSnapshot()));
                } catch (Throwable throwable) {
                    failureSink.completeExceptionally(throwable);
                    return;
                }
                if (completed.incrementAndGet() == chunkKeys.size()) {
                    onDone.run();
                    return;
                }
                pump[0].run();
            });
        };
        for (int i = 0; i < Math.min(CHUNK_LOADS_IN_FLIGHT, chunkKeys.size()); i++) {
            pump[0].run();
        }
    }

    /**
     * Scans snapshots into a palette. Thread-safe: touches only snapshot data.
     */
    private static ScanResult scan(Bounds bounds,
                                   List<SnapshotEntry> snapshots,
                                   boolean skipAir,
                                   Set<Material> excludedMaterials) {
        Set<Material> excluded = excludedMaterials == null ? Set.of() : excludedMaterials;
        Map<BlockData, Integer> paletteIndex = new HashMap<>();
        List<BlockData> palette = new ArrayList<>();
        IntBuffer positions = new IntBuffer();
        IntBuffer paletteIds = new IntBuffer();
        IntBuffer nexoCandidates = new IntBuffer();

        int height = bounds.height();
        int depth = bounds.depth();

        for (SnapshotEntry entry : snapshots) {
            ChunkSnapshot snapshot = entry.snapshot();
            int baseX = entry.chunkX() << 4;
            int baseZ = entry.chunkZ() << 4;
            int fromX = Math.max(bounds.minX(), baseX);
            int toX = Math.min(bounds.maxX(), baseX + 15);
            int fromZ = Math.max(bounds.minZ(), baseZ);
            int toZ = Math.min(bounds.maxZ(), baseZ + 15);

            for (int x = fromX; x <= toX; x++) {
                int localX = x - baseX;
                for (int z = fromZ; z <= toZ; z++) {
                    int localZ = z - baseZ;
                    for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                        Material material = snapshot.getBlockType(localX, y, localZ);
                        if (excluded.contains(material)) {
                            continue;
                        }
                        if (NEXO_BLOCK_CANDIDATES.contains(material)) {
                            nexoCandidates.add(linear(x - bounds.minX(), y - bounds.minY(), z - bounds.minZ(), height, depth));
                        }
                        if (skipAir && material == Material.AIR) {
                            continue;
                        }
                        BlockData data = snapshot.getBlockData(localX, y, localZ);
                        Integer id = paletteIndex.get(data);
                        if (id == null) {
                            id = palette.size();
                            paletteIndex.put(data, id);
                            palette.add(data);
                        }
                        positions.add(linear(x - bounds.minX(), y - bounds.minY(), z - bounds.minZ(), height, depth));
                        paletteIds.add(id);
                    }
                }
            }
        }

        return new ScanResult(
                palette.toArray(new BlockData[0]),
                positions.trimmed(),
                paletteIds.trimmed(),
                positions.size(),
                nexoCandidates.trimmed(),
                nexoCandidates.size());
    }

    /**
     * Resolves the parts that require the server thread: Nexo custom-block ids for the
     * handful of candidate positions, and the furniture entity sweep.
     */
    private static CuboidTemplate finish(World world, Bounds bounds, ScanResult scan) {
        List<NexoBlockCopy> nexoBlocks = new ArrayList<>();
        int[] candidates = scan.nexoCandidates();
        for (int i = 0; i < scan.nexoCandidateCount(); i++) {
            int linear = candidates[i];
            int relZ = linear % bounds.depth();
            int rest = linear / bounds.depth();
            int relY = rest % bounds.height();
            int relX = rest / bounds.height();
            Block block = world.getBlockAt(bounds.minX() + relX, bounds.minY() + relY, bounds.minZ() + relZ);
            CustomBlockMechanic mechanic = NexoBlocks.customBlockMechanic(block);
            if (mechanic == null || mechanic.getItemID() == null || mechanic.getItemID().isBlank()) {
                continue;
            }
            nexoBlocks.add(new NexoBlockCopy(relX, relY, relZ, mechanic.getItemID()));
        }

        List<NexoFurnitureCopy> nexoFurniture = new ArrayList<>();
        Set<UUID> captured = new HashSet<>();
        BoundingBox box = new BoundingBox(
                bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX() + 1.0D, bounds.maxY() + 1.0D, bounds.maxZ() + 1.0D);
        for (Entity entity : world.getNearbyEntities(box)) {
            if (entity instanceof ItemDisplay display) {
                captureNexoFurniture(display, bounds.minX(), bounds.minY(), bounds.minZ(), captured)
                        .ifPresent(nexoFurniture::add);
            }
        }

        return new CuboidTemplate(
                world.getName(),
                bounds.minX(),
                bounds.minY(),
                bounds.minZ(),
                bounds.width(),
                bounds.height(),
                bounds.depth(),
                scan.palette(),
                scan.positions(),
                scan.paletteIds(),
                scan.blockCount(),
                nexoBlocks,
                nexoFurniture);
    }

    public void paste(World world, int baseX, int baseY, int baseZ) {
        if (world == null) {
            return;
        }
        for (int i = 0; i < blockCount; i++) {
            writeBlock(world, baseX, baseY, baseZ, i);
        }
        pasteNexo(world, baseX, baseY, baseZ);
    }

    /**
     * Pastes across multiple ticks so a large template cannot stall the server thread.
     * {@code onDone} runs on the server thread once every block has been written.
     */
    public void pasteBatched(Plugin plugin, World world, int baseX, int baseY, int baseZ, Runnable onDone) {
        if (world == null) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        new BukkitRunnable() {
            private int index;

            @Override
            public void run() {
                int end = Math.min(blockCount, index + PASTE_BLOCKS_PER_TICK);
                for (; index < end; index++) {
                    writeBlock(world, baseX, baseY, baseZ, index);
                }
                if (index < blockCount) {
                    return;
                }
                cancel();
                pasteNexo(world, baseX, baseY, baseZ);
                if (onDone != null) {
                    onDone.run();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void writeBlock(World world, int baseX, int baseY, int baseZ, int index) {
        int linear = positions[index];
        int relZ = linear % depth;
        int rest = linear / depth;
        int relY = rest % height;
        int relX = rest / height;
        world.getBlockAt(baseX + relX, baseY + relY, baseZ + relZ)
                .setBlockData(palette[paletteIds[index]], false);
    }

    private void pasteNexo(World world, int baseX, int baseY, int baseZ) {
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
        for (int i = 0; i < blockCount; i++) {
            if (palette[paletteIds[i]].getMaterial() != material) {
                continue;
            }
            int linear = positions[i];
            int relZ = linear % depth;
            int rest = linear / depth;
            return Optional.of(new BlockCopy(rest / height, rest % height, relZ, palette[paletteIds[i]]));
        }
        return Optional.empty();
    }

    public CuboidTemplate without(Material material) {
        if (material == null) {
            return this;
        }
        IntBuffer keptPositions = new IntBuffer();
        IntBuffer keptIds = new IntBuffer();
        for (int i = 0; i < blockCount; i++) {
            if (palette[paletteIds[i]].getMaterial() == material) {
                continue;
            }
            keptPositions.add(positions[i]);
            keptIds.add(paletteIds[i]);
        }
        return new CuboidTemplate(sourceWorldName, minX, minY, minZ, width, height, depth,
                palette, keptPositions.trimmed(), keptIds.trimmed(), keptPositions.size(),
                nexoBlocks, nexoFurniture);
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

    public int blockCount() {
        return blockCount;
    }

    /**
     * Materialises the stored palette back into individual records. Callers that only
     * need counts or a single lookup should prefer {@link #blockCount()} or
     * {@link #firstBlock(Material)}, which do not allocate per block.
     */
    public List<BlockCopy> blocks() {
        List<BlockCopy> out = new ArrayList<>(blockCount);
        for (int i = 0; i < blockCount; i++) {
            int linear = positions[i];
            int relZ = linear % depth;
            int rest = linear / depth;
            out.add(new BlockCopy(rest / height, rest % height, relZ, palette[paletteIds[i]]));
        }
        return out;
    }

    public List<NexoBlockCopy> nexoBlocks() {
        return nexoBlocks;
    }

    public List<NexoFurnitureCopy> nexoFurniture() {
        return nexoFurniture;
    }

    private static int linear(int relX, int relY, int relZ, int height, int depth) {
        return (relX * height + relY) * depth + relZ;
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

    /** Cuboid bounds clamped to the world's buildable height range. */
    private record Bounds(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        static Bounds of(Location pos1, Location pos2) {
            Objects.requireNonNull(pos1, "pos1");
            Objects.requireNonNull(pos2, "pos2");
            World world = pos1.getWorld();
            if (world == null || pos2.getWorld() == null || !world.equals(pos2.getWorld())) {
                throw new IllegalArgumentException("Cuboid positions must be in the same loaded world.");
            }
            int minY = Math.max(world.getMinHeight(), Math.min(pos1.getBlockY(), pos2.getBlockY()));
            int maxY = Math.min(world.getMaxHeight() - 1, Math.max(pos1.getBlockY(), pos2.getBlockY()));
            return new Bounds(world,
                    Math.min(pos1.getBlockX(), pos2.getBlockX()),
                    minY,
                    Math.min(pos1.getBlockZ(), pos2.getBlockZ()),
                    Math.max(pos1.getBlockX(), pos2.getBlockX()),
                    maxY,
                    Math.max(pos1.getBlockZ(), pos2.getBlockZ()));
        }

        int width() {
            return maxX - minX + 1;
        }

        int height() {
            return maxY - minY + 1;
        }

        int depth() {
            return maxZ - minZ + 1;
        }
    }

    private record SnapshotEntry(int chunkX, int chunkZ, ChunkSnapshot snapshot) { }

    private record ScanResult(BlockData[] palette,
                              int[] positions,
                              int[] paletteIds,
                              int blockCount,
                              int[] nexoCandidates,
                              int nexoCandidateCount) { }

    /** Growable primitive int list; avoids boxing millions of coordinates. */
    private static final class IntBuffer {
        private int[] data = new int[1024];
        private int size;

        void add(int value) {
            if (size == data.length) {
                int[] grown = new int[data.length << 1];
                System.arraycopy(data, 0, grown, 0, size);
                data = grown;
            }
            data[size++] = value;
        }

        int size() {
            return size;
        }

        int[] trimmed() {
            int[] out = new int[size];
            System.arraycopy(data, 0, out, 0, size);
            return out;
        }
    }

    public record BlockCopy(int x, int y, int z, BlockData data) { }

    public record NexoBlockCopy(int x, int y, int z, String itemId) { }

    public record NexoFurnitureCopy(double x, double y, double z, String itemId, float yaw, BlockFace facing) { }
}
