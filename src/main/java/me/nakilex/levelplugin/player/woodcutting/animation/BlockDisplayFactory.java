package me.nakilex.levelplugin.player.woodcutting.animation;

import me.nakilex.levelplugin.player.woodcutting.WoodcuttingConfig;
import me.nakilex.levelplugin.player.woodcutting.tree.TreeDetectionResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class BlockDisplayFactory {
    private final WoodcuttingConfig config;

    public BlockDisplayFactory(WoodcuttingConfig config) {
        this.config = config;
    }

    public DisplayTree convert(TreeDetectionResult tree) {
        Location pivot = tree.pivotLocation();
        List<DisplayTree.DisplayBlock> displayBlocks = new ArrayList<>();
        for (TreeDetectionResult.CapturedBlock snapshot : tree.snapshots()) {
            Location original = snapshot.originalLocation().clone();
            Vector offset = original.toVector().subtract(pivot.toVector());
            Vector3f originalOffsetFromPivot = new Vector3f((float) offset.getX(), (float) offset.getY(), (float) offset.getZ());
            snapshot.block().setType(Material.AIR, false);
            BlockDisplay display = original.getWorld().spawn(original, BlockDisplay.class, entity -> {
                entity.setBlock(snapshot.blockData());
                entity.setPersistent(false);
                entity.setInvulnerable(true);
                entity.setInterpolationDelay(0);
                entity.setInterpolationDuration((int) config.ticksPerFrame());
            });
            displayBlocks.add(new DisplayTree.DisplayBlock(display, snapshot.blockData(), original, originalOffsetFromPivot));
        }
        return new DisplayTree(tree, pivot, displayBlocks);
    }
}
