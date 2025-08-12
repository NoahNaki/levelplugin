package me.nakilex.levelplugin.utils;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockState;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/** Utility methods for saving/loading FAWE schematics for selected regions. */
public final class SchematicUtil {
    private SchematicUtil() {}

    /**
     * Save the blocks within the cuboid defined by the two locations to a schematic file.
     */
    public static void saveSchematic(Location p1, Location p2, File file, Logger logger) {
        try {
            int minX = Math.min(p1.getBlockX(), p2.getBlockX());
            int minY = Math.min(p1.getBlockY(), p2.getBlockY());
            int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
            int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
            int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
            int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

            CuboidRegion region = new CuboidRegion(
                    BukkitAdapter.adapt(p1.getWorld()),
                    BlockVector3.at(minX, minY, minZ),
                    BlockVector3.at(maxX, maxY, maxZ)
            );
            Clipboard clipboard = new BlockArrayClipboard(region);
            try (EditSession session = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(p1.getWorld()))) {
                ForwardExtentCopy copy = new ForwardExtentCopy(session, region, clipboard, region.getMinimumPoint());
                copy.setCopyingEntities(false);
                Operations.complete(copy);
            }
            ClipboardFormat format = ClipboardFormats.findByFile(file);
            if (format == null) format = ClipboardFormats.findByExtension("schem");
            if (format == null) {
                if (logger != null) {
                    logger.warning("Unknown schematic format for " + file.getName());
                }
                return;
            }
            try (ClipboardWriter writer = format.getWriter(new FileOutputStream(file))) {
                writer.write(clipboard);
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.warning("Failed to save schematic " + file.getName() + ": " + e.getMessage());
            }
            e.printStackTrace();
        }
    }

    /**
     * Load a schematic file into a map of relative block vectors to block data.
     */
    public static Map<BlockVector3, BlockData> loadSchematic(File file, Logger logger) {
        Map<BlockVector3, BlockData> blocks = new HashMap<>();
        try {
            if (!file.exists()) {
                if (logger != null) logger.warning("Schematic not found: " + file.getName());
                return blocks;
            }
            ClipboardFormat format = ClipboardFormats.findByFile(file);
            if (format == null) format = ClipboardFormats.findByExtension("schem");
            if (format == null) return blocks;
            try (var reader = format.getReader(new FileInputStream(file))) {
                Clipboard clipboard = reader.read();
                BlockVector3 min = clipboard.getRegion().getMinimumPoint();
                for (BlockVector3 vec : clipboard.getRegion()) {
                    BlockState state = clipboard.getBlock(vec);
                    BlockData data = BukkitAdapter.adapt(state.toImmutableState());
                    if (data.getMaterial().isAir()) continue;
                    blocks.put(BlockVector3.at(vec.getBlockX() - min.getBlockX(),
                            vec.getBlockY() - min.getBlockY(),
                            vec.getBlockZ() - min.getBlockZ()), data);
                }
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.warning("Failed to load schematic " + file.getName() + ": " + e.getMessage());
            }
            e.printStackTrace();
        }
        return blocks;
    }

    /**
     * Convert a map of relative block vectors to absolute world locations.
     */
    public static Map<Location, BlockData> toLocationMap(Map<BlockVector3, BlockData> rel, World world,
                                                        int baseX, int baseY, int baseZ) {
        Map<Location, BlockData> map = new HashMap<>();
        for (var entry : rel.entrySet()) {
            BlockVector3 v = entry.getKey();
            Location loc = new Location(world, baseX + v.getBlockX(), baseY + v.getBlockY(), baseZ + v.getBlockZ());
            map.put(loc, entry.getValue());
        }
        return map;
    }
}
