package me.nakilex.xprisonenchants.enchant;

import dev.drawethree.xprison.api.enchants.area.AreaBounds;
import dev.drawethree.xprison.api.enchants.area.AreaBreakEnchant;
import me.nakilex.xprisonenchants.fx.BlockEcho;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Base for the four cinematic area enchants.
 *
 * <p>All of them follow the same shape, which is the only shape the area pipeline allows: pick the
 * blocks, snapshot how they look, resolve the break <em>once</em>, then play the animation over the
 * snapshot. Nothing about a proc is stored on the instance — enchant objects are shared between
 * every player using them, so per-proc state lives in the {@link #dispatchWithEffect} closure.
 */
public abstract class CinematicAreaEnchant extends AreaBreakEnchant {

    /**
     * Blocks that must never be swept up by an area effect even if they sit inside the region.
     * The pipeline already filters air and out-of-region blocks; this guards the mine's own shell.
     */
    private static final Set<Material> PROTECTED = Set.of(
            Material.BEDROCK, Material.BARRIER, Material.STRUCTURE_BLOCK, Material.JIGSAW,
            Material.END_PORTAL_FRAME, Material.END_PORTAL, Material.NETHER_PORTAL,
            Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
            Material.LIGHT, Material.MOVING_PISTON);

    protected CinematicAreaEnchant(File configFile) {
        super(configFile);
    }

    @Override
    public String getAuthor() {
        return "Nakilex";
    }

    @Override
    public void unload() {
        // Nothing held between procs; animations clean themselves up.
    }

    /**
     * Snapshots the targets, resolves the break, then hands the snapshot to the subclass to animate.
     *
     * <p>The order matters and is the whole trick: {@link BlockEcho#snapshot} has to run while the
     * blocks still exist, and {@code resolve} has to be called exactly once. Playing the animation
     * after resolving means a failed animation can never cost a player their payout.
     */
    @Override
    public final void dispatchWithEffect(Player player, Block origin, List<Block> targets, int level,
                                         Consumer<List<Block>> resolve) {
        List<BlockEcho> echoes;
        try {
            echoes = BlockEcho.snapshot(targets);
        } catch (RuntimeException ex) {
            echoes = Collections.emptyList();
        }

        resolve.accept(targets);

        if (!echoes.isEmpty()) {
            try {
                playEffect(player, origin, echoes, level);
            } catch (RuntimeException ex) {
                BlockEcho.removeAll(echoes);
                throw ex;
            }
        }
    }

    /**
     * Plays this enchant's animation over blocks that have already been broken and paid out.
     *
     * @param echoes visual copies of the destroyed blocks; the implementation owns removing them
     */
    protected abstract void playEffect(Player player, Block origin, List<BlockEcho> echoes, int level);

    // ------------------------------------------------------------------
    // Shared target selection
    // ------------------------------------------------------------------

    /** Vertical cylinder around the broken block. */
    protected List<Block> cylinder(Block origin, AreaBounds region, double radius, int height, int cap) {
        List<Block> blocks = new ArrayList<>();
        int r = (int) Math.ceil(radius);
        double rSquared = radius * radius;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (x * x + z * z > rSquared) {
                    continue;
                }
                for (int y = 0; y < height; y++) {
                    if (!accept(origin.getRelative(x, y, z), region, blocks, cap)) {
                        return blocks;
                    }
                }
            }
        }
        return blocks;
    }

    /** Sphere centred on the broken block. */
    protected List<Block> sphere(Block origin, AreaBounds region, double radius, int cap) {
        List<Block> blocks = new ArrayList<>();
        int r = (int) Math.ceil(radius);
        double rSquared = radius * radius;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + y * y + z * z > rSquared) {
                        continue;
                    }
                    if (!accept(origin.getRelative(x, y, z), region, blocks, cap)) {
                        return blocks;
                    }
                }
            }
        }
        return blocks;
    }

    /**
     * Adds a block if it is worth breaking, and reports whether collection should continue.
     *
     * @return {@code false} once the cap is reached
     */
    protected boolean accept(Block block, @Nullable AreaBounds region, List<Block> into, int cap) {
        if (cap > 0 && into.size() >= cap) {
            return false;
        }
        if (block == null || PROTECTED.contains(block.getType())) {
            return true;
        }
        if (region != null && !region.contains(block)) {
            return true;
        }
        into.add(block);
        return true;
    }
}
