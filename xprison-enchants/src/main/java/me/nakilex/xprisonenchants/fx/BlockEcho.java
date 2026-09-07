package me.nakilex.xprisonenchants.fx;

import dev.drawethree.xprison.api.XPrisonAPI;
import dev.drawethree.xprison.api.blocks.MineBlock;
import dev.drawethree.xprison.api.virtualblocks.XPrisonVirtualBlocksAPI;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A visual stand-in for a block that the break pipeline has already removed.
 *
 * <p>X-Prison settles an area proc in one pass: the blocks are gone the moment
 * {@code resolve} is called. Every one of these enchants is a <i>cinematic</i> — the block has to
 * still appear to be there while the tornado lifts it, the black hole drags it in, or the acid
 * eats through it. So each enchant snapshots its targets <em>before</em> resolving, then animates
 * these client-side copies instead of the real blocks.
 *
 * <p>Snapshotting through this class is also what makes the effects work on X-PrivateMines packet
 * mines. There the ore only exists in X-Prison's virtual-block store, so {@link Block#getBlockData()}
 * reports air; {@link #snapshot(List)} asks the virtual-blocks API first and only falls back to the
 * world.
 */
public final class BlockEcho {

    private final Location location;
    private final BlockData data;
    private BlockDisplay display;

    private BlockEcho(Location location, BlockData data) {
        this.location = location;
        this.data = data;
    }

    /**
     * Captures the appearance of each block. Must be called before the blocks are broken.
     *
     * @param blocks the blocks about to be destroyed
     * @return one echo per block whose appearance could be resolved
     */
    public static List<BlockEcho> snapshot(List<Block> blocks) {
        XPrisonVirtualBlocksAPI virtual = virtualBlocks();
        List<BlockEcho> echoes = new ArrayList<>(blocks.size());
        for (Block block : blocks) {
            BlockData data = resolveData(block, virtual);
            if (data != null && !data.getMaterial().isAir()) {
                echoes.add(new BlockEcho(block.getLocation(), data));
            }
        }
        return echoes;
    }

    private static XPrisonVirtualBlocksAPI virtualBlocks() {
        try {
            return XPrisonAPI.getInstance().getVirtualBlocksApi();
        } catch (RuntimeException | LinkageError unavailable) {
            return null; // older core, or the module is off - fall back to the world
        }
    }

    private static BlockData resolveData(Block block, XPrisonVirtualBlocksAPI virtual) {
        if (virtual != null) {
            try {
                Location at = block.getLocation();
                if (virtual.isVirtualMineArea(at)) {
                    MineBlock mineBlock = virtual.blockAt(at);
                    BlockData fromVirtual = mineBlock == null ? null : toBlockData(mineBlock);
                    if (fromVirtual != null) {
                        return fromVirtual;
                    }
                }
            } catch (RuntimeException ignored) {
                // fall through to the world lookup
            }
        }
        BlockData world = block.getBlockData();
        return world.getMaterial().isAir() ? null : world;
    }

    /**
     * Custom-block providers hand back namespaced ids ("nexo:ruby_ore") that have no vanilla
     * {@link Material}. Those are shown as stone rather than skipped, so a custom-ore mine still
     * animates.
     */
    private static BlockData toBlockData(MineBlock mineBlock) {
        String id = mineBlock.getId();
        if (id == null || id.isEmpty()) {
            return null;
        }
        if (!mineBlock.isVanilla()) {
            return Material.STONE.createBlockData();
        }
        Material material = Material.matchMaterial(id.toUpperCase(Locale.ROOT));
        return material == null || !material.isBlock() ? null : material.createBlockData();
    }

    public Location location() {
        return location.clone();
    }

    /** @return the centre of the block this echo stands in for */
    public Location center() {
        return location.clone().add(0.5, 0.5, 0.5);
    }

    public BlockDisplay display() {
        return display;
    }

    /**
     * Spawns the visual copy, scaled about the block's centre so it sits exactly where the real
     * block was.
     *
     * @param scale the edge length, 1.0 being a full block
     * @param glow  a glow colour, or {@code null} for no glow
     */
    public void spawn(float scale, Color glow) {
        Location at = location.clone().add((1.0 - scale) / 2.0, (1.0 - scale) / 2.0, (1.0 - scale) / 2.0);
        display = at.getWorld().spawn(at, BlockDisplay.class, spawned -> {
            spawned.setBlock(data);
            spawned.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f(0f, 0f, 0f, 1f),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0f, 0f, 0f, 1f)));
            spawned.setBrightness(new Display.Brightness(15, 15));
            spawned.setPersistent(false);
            if (glow != null) {
                spawned.setGlowing(true);
                spawned.setGlowColorOverride(glow);
            }
        });
    }

    /** Moves the visual copy, keeping the spawn-time centring offset. */
    public void moveTo(Location to, float scale) {
        if (display == null || !display.isValid()) {
            return;
        }
        double inset = (1.0 - scale) / 2.0;
        display.teleport(to.clone().add(inset - 0.5, inset - 0.5, inset - 0.5));
    }

    /** Removes the visual copy. Safe to call more than once. */
    public void remove() {
        if (display != null) {
            display.remove();
            display = null;
        }
    }

    public static void removeAll(List<BlockEcho> echoes) {
        for (BlockEcho echo : echoes) {
            echo.remove();
        }
    }
}
