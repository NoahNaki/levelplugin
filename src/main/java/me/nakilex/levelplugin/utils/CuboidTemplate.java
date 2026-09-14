package me.nakilex.levelplugin.utils;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.generator.ChunkGenerator;
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
    private static final int PASTE_TIME_CHECK_INTERVAL = 256;

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
    private final int relativeChunkColumns;
    private final int[] relativeChunkOffsets;
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
        ChunkOrderedBlocks ordered = orderByRelativeChunk(width, height, depth, positions, paletteIds, blockCount);
        this.positions = ordered.positions();
        this.paletteIds = ordered.paletteIds();
        this.blockCount = blockCount;
        this.relativeChunkColumns = (width + 15) >> 4;
        this.relativeChunkOffsets = ordered.offsets();
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

    /**
     * Preloads every destination chunk and pastes with an adaptive per-tick time budget.
     * Chunks remain ticketed until the paste and Nexo finishing pass have completed.
     */
    public CompletableFuture<PasteStats> pasteBatchedAdaptive(Plugin plugin,
                                                               World world,
                                                               int baseX,
                                                               int baseY,
                                                               int baseZ,
                                                               PasteOptions requestedOptions) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(world, "world");
        PasteOptions options = requestedOptions == null ? PasteOptions.defaults() : requestedOptions.normalized();
        CompletableFuture<PasteStats> result = new CompletableFuture<>();
        long preloadStarted = System.nanoTime();

        preloadDestinationChunks(plugin, world, baseX, baseZ, options.chunkPreloadConcurrency())
                .whenComplete((lease, preloadError) -> {
                    if (preloadError != null) {
                        result.completeExceptionally(preloadError);
                        return;
                    }
                    long preloadNanos = System.nanoTime() - preloadStarted;
                    Runnable beginPaste = () -> startAdaptivePaste(
                            plugin, world, baseX, baseY, baseZ, options, lease, preloadNanos, result);
                    if (Bukkit.isPrimaryThread()) {
                        beginPaste.run();
                    } else {
                        Bukkit.getScheduler().runTask(plugin, beginPaste);
                    }
                });
        return result;
    }

    private void startAdaptivePaste(Plugin plugin,
                                    World world,
                                    int baseX,
                                    int baseY,
                                    int baseZ,
                                    PasteOptions options,
                                    ChunkTicketLease lease,
                                    long preloadNanos,
                                    CompletableFuture<PasteStats> result) {
        long pasteStarted = System.nanoTime();
        new BukkitRunnable() {
            private int index;
            private int batchSize = options.initialBlocksPerTick();
            private int batches;
            private int smallestBatch = Integer.MAX_VALUE;
            private int largestBatch;
            private long activeNanos;
            private long maxBatchNanos;

            @Override
            public void run() {
                long batchStarted = System.nanoTime();
                int batchStartIndex = index;
                try {
                    int hardEnd = Math.min(blockCount, index + batchSize);
                    while (index < hardEnd) {
                        writeBlock(world, baseX, baseY, baseZ, index++);
                        int written = index - batchStartIndex;
                        if (written >= options.minimumBlocksPerTick()
                                && written % PASTE_TIME_CHECK_INTERVAL == 0
                                && System.nanoTime() - batchStarted >= options.targetNanosPerTick()) {
                            break;
                        }
                    }

                    long batchNanos = System.nanoTime() - batchStarted;
                    int written = index - batchStartIndex;
                    if (written > 0) {
                        batches++;
                        activeNanos += batchNanos;
                        maxBatchNanos = Math.max(maxBatchNanos, batchNanos);
                        smallestBatch = Math.min(smallestBatch, written);
                        largestBatch = Math.max(largestBatch, written);
                        batchSize = nextBatchSize(options, batchSize, written, batchNanos);
                    }

                    if (index < blockCount) {
                        return;
                    }

                    cancel();
                    long finishingStarted = System.nanoTime();
                    pasteNexo(world, baseX, baseY, baseZ);
                    long finishingNanos = System.nanoTime() - finishingStarted;
                    lease.release();
                    result.complete(new PasteStats(
                            lease.chunkCount(),
                            blockCount,
                            batches,
                            preloadNanos,
                            System.nanoTime() - pasteStarted,
                            activeNanos,
                            maxBatchNanos,
                            finishingNanos,
                            smallestBatch == Integer.MAX_VALUE ? 0 : smallestBatch,
                            largestBatch));
                } catch (Throwable throwable) {
                    cancel();
                    lease.release();
                    result.completeExceptionally(throwable);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static int nextBatchSize(PasteOptions options,
                                     int currentBatchSize,
                                     int written,
                                     long elapsedNanos) {
        if (written <= 0 || elapsedNanos <= 0L) {
            return currentBatchSize;
        }
        long estimated = Math.round(written * (options.targetNanosPerTick() / (double) elapsedNanos));
        int target = (int) Math.max(options.minimumBlocksPerTick(),
                Math.min(options.maximumBlocksPerTick(), estimated));
        int smoothed = (int) Math.round((currentBatchSize * 0.5D) + (target * 0.5D));
        return Math.max(options.minimumBlocksPerTick(),
                Math.min(options.maximumBlocksPerTick(), smoothed));
    }

    private CompletableFuture<ChunkTicketLease> preloadDestinationChunks(Plugin plugin,
                                                                          World world,
                                                                          int baseX,
                                                                          int baseZ,
                                                                          int concurrency) {
        List<int[]> chunkKeys = new ArrayList<>();
        int minChunkX = baseX >> 4;
        int maxChunkX = (baseX + width - 1) >> 4;
        int minChunkZ = baseZ >> 4;
        int maxChunkZ = (baseZ + depth - 1) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                chunkKeys.add(new int[]{chunkX, chunkZ});
            }
        }

        CompletableFuture<ChunkTicketLease> result = new CompletableFuture<>();
        if (chunkKeys.isEmpty()) {
            result.complete(new ChunkTicketLease(plugin, List.of()));
            return result;
        }

        List<Chunk> ticketed = new ArrayList<>(chunkKeys.size());
        java.util.concurrent.atomic.AtomicInteger next = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger completed = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicBoolean failed = new java.util.concurrent.atomic.AtomicBoolean(false);
        Runnable[] pump = new Runnable[1];
        pump[0] = () -> {
            if (failed.get()) {
                return;
            }
            int index = next.getAndIncrement();
            if (index >= chunkKeys.size()) {
                return;
            }
            int chunkX = chunkKeys.get(index)[0];
            int chunkZ = chunkKeys.get(index)[1];
            world.getChunkAtAsync(chunkX, chunkZ, true).whenComplete((chunk, error) -> {
                if (failed.get()) {
                    return;
                }
                if (error != null || chunk == null) {
                    if (failed.compareAndSet(false, true)) {
                        new ChunkTicketLease(plugin, ticketed).release();
                        result.completeExceptionally(error == null
                                ? new IllegalStateException("Chunk preload returned null for " + chunkX + "," + chunkZ)
                                : error);
                    }
                    return;
                }
                chunk.addPluginChunkTicket(plugin);
                ticketed.add(chunk);
                if (completed.incrementAndGet() == chunkKeys.size()) {
                    result.complete(new ChunkTicketLease(plugin, ticketed));
                    return;
                }
                pump[0].run();
            });
        };
        for (int i = 0; i < Math.min(Math.max(1, concurrency), chunkKeys.size()); i++) {
            pump[0].run();
        }
        return result;
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

    /**
     * Writes only the portion of this template intersecting one generated chunk.
     * The template is chunk-indexed once when captured, so lazy world generation
     * never scans the millions of blocks that belong to other chunks.
     */
    public void populateChunk(ChunkGenerator.ChunkData chunkData,
                              int targetChunkX,
                              int targetChunkZ,
                              int baseX,
                              int baseY,
                              int baseZ) {
        Objects.requireNonNull(chunkData, "chunkData");
        int targetMinX = targetChunkX << 4;
        int targetMinZ = targetChunkZ << 4;
        int relMinX = Math.max(0, targetMinX - baseX);
        int relMaxX = Math.min(width - 1, targetMinX + 15 - baseX);
        int relMinZ = Math.max(0, targetMinZ - baseZ);
        int relMaxZ = Math.min(depth - 1, targetMinZ + 15 - baseZ);
        if (relMinX > relMaxX || relMinZ > relMaxZ) {
            return;
        }

        int minRelativeChunkX = relMinX >> 4;
        int maxRelativeChunkX = relMaxX >> 4;
        int minRelativeChunkZ = relMinZ >> 4;
        int maxRelativeChunkZ = relMaxZ >> 4;
        for (int relativeChunkX = minRelativeChunkX; relativeChunkX <= maxRelativeChunkX; relativeChunkX++) {
            for (int relativeChunkZ = minRelativeChunkZ; relativeChunkZ <= maxRelativeChunkZ; relativeChunkZ++) {
                int relativeChunkIndex = relativeChunkZ * relativeChunkColumns + relativeChunkX;
                int start = relativeChunkOffsets[relativeChunkIndex];
                int end = relativeChunkOffsets[relativeChunkIndex + 1];
                for (int i = start; i < end; i++) {
                    int linear = positions[i];
                    int relZ = linear % depth;
                    int rest = linear / depth;
                    int relY = rest % height;
                    int relX = rest / height;
                    int worldX = baseX + relX;
                    int worldY = baseY + relY;
                    int worldZ = baseZ + relZ;
                    if ((worldX >> 4) != targetChunkX || (worldZ >> 4) != targetChunkZ
                            || worldY < chunkData.getMinHeight() || worldY >= chunkData.getMaxHeight()) {
                        continue;
                    }
                    chunkData.setBlock(worldX & 15, worldY, worldZ & 15, palette[paletteIds[i]]);
                }
            }
        }
    }

    /** Places the Nexo metadata/entities from this template that belong to one loaded chunk. */
    public void pasteNexoInChunk(World world,
                                 int targetChunkX,
                                 int targetChunkZ,
                                 int baseX,
                                 int baseY,
                                 int baseZ) {
        if (world == null) {
            return;
        }
        for (NexoBlockCopy block : nexoBlocks) {
            int worldX = baseX + block.x();
            int worldZ = baseZ + block.z();
            if ((worldX >> 4) != targetChunkX || (worldZ >> 4) != targetChunkZ) {
                continue;
            }
            NexoBlocks.place(block.itemId(), new Location(world, worldX, baseY + block.y(), worldZ));
        }
        for (NexoFurnitureCopy furniture : nexoFurniture) {
            double worldX = baseX + furniture.x();
            double worldZ = baseZ + furniture.z();
            if (((int) Math.floor(worldX) >> 4) != targetChunkX
                    || ((int) Math.floor(worldZ) >> 4) != targetChunkZ) {
                continue;
            }
            Location target = new Location(world, worldX, baseY + furniture.y(), worldZ,
                    furniture.yaw(), 0.0F);
            NexoFurniture.remove(target);
            NexoFurniture.place(furniture.itemId(), target, furniture.yaw(), furniture.facing());
        }
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

    public record PasteOptions(long targetNanosPerTick,
                               int initialBlocksPerTick,
                               int minimumBlocksPerTick,
                               int maximumBlocksPerTick,
                               int chunkPreloadConcurrency) {
        public static PasteOptions defaults() {
            return new PasteOptions(20_000_000L, 20_000, 2_000, 50_000, 16);
        }

        private PasteOptions normalized() {
            long targetNanos = Math.max(1_000_000L, Math.min(45_000_000L, targetNanosPerTick));
            int minimum = Math.max(1, minimumBlocksPerTick);
            int maximum = Math.max(minimum, maximumBlocksPerTick);
            int initial = Math.max(minimum, Math.min(maximum, initialBlocksPerTick));
            int concurrency = Math.max(1, Math.min(64, chunkPreloadConcurrency));
            return new PasteOptions(targetNanos, initial, minimum, maximum, concurrency);
        }
    }

    public record PasteStats(int preloadedChunks,
                             int blocks,
                             int batches,
                             long preloadNanos,
                             long pasteWallNanos,
                             long activeNanos,
                             long maxBatchNanos,
                             long finishingNanos,
                             int smallestBatch,
                             int largestBatch) {
    }

    private static final class ChunkTicketLease {
        private final Plugin plugin;
        private final List<Chunk> chunks;
        private final java.util.concurrent.atomic.AtomicBoolean released = new java.util.concurrent.atomic.AtomicBoolean(false);

        private ChunkTicketLease(Plugin plugin, List<Chunk> chunks) {
            this.plugin = plugin;
            this.chunks = List.copyOf(chunks);
        }

        private int chunkCount() {
            return chunks.size();
        }

        private void release() {
            if (!released.compareAndSet(false, true)) {
                return;
            }
            for (Chunk chunk : chunks) {
                if (chunk != null) {
                    chunk.removePluginChunkTicket(plugin);
                }
            }
        }
    }

    private static int linear(int relX, int relY, int relZ, int height, int depth) {
        return (relX * height + relY) * depth + relZ;
    }

    private static ChunkOrderedBlocks orderByRelativeChunk(int width,
                                                            int height,
                                                            int depth,
                                                            int[] positions,
                                                            int[] paletteIds,
                                                            int blockCount) {
        int columns = (width + 15) >> 4;
        int rows = (depth + 15) >> 4;
        int[] counts = new int[columns * rows];
        for (int i = 0; i < blockCount; i++) {
            int linear = positions[i];
            int relZ = linear % depth;
            int relX = (linear / depth) / height;
            counts[(relZ >> 4) * columns + (relX >> 4)]++;
        }

        int[] offsets = new int[counts.length + 1];
        for (int i = 0; i < counts.length; i++) {
            offsets[i + 1] = offsets[i] + counts[i];
        }
        int[] cursors = java.util.Arrays.copyOf(offsets, counts.length);
        int[] orderedPositions = new int[blockCount];
        int[] orderedPaletteIds = new int[blockCount];
        for (int i = 0; i < blockCount; i++) {
            int linear = positions[i];
            int relZ = linear % depth;
            int relX = (linear / depth) / height;
            int target = cursors[(relZ >> 4) * columns + (relX >> 4)]++;
            orderedPositions[target] = linear;
            orderedPaletteIds[target] = paletteIds[i];
        }
        return new ChunkOrderedBlocks(orderedPositions, orderedPaletteIds, offsets);
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

    private record ChunkOrderedBlocks(int[] positions, int[] paletteIds, int[] offsets) { }

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
