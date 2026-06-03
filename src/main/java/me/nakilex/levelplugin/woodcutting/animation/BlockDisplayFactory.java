package me.nakilex.levelplugin.woodcutting.animation;

import me.nakilex.levelplugin.woodcutting.tree.TreeDetectionResult;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class BlockDisplayFactory {
    public DisplayTree convert(TreeDetectionResult tree) {
        List<DisplayTree.DisplayBlock> displayBlocks = new ArrayList<>();
        for (TreeDetectionResult.CapturedBlock snapshot : tree.snapshots()) {
            snapshot.block().setType(Material.AIR, false);
            BlockDisplay display = snapshot.originalLocation().getWorld().spawn(snapshot.originalLocation(), BlockDisplay.class, entity -> {
                entity.setBlock(snapshot.blockData());
                entity.setPersistent(false);
                entity.setInvulnerable(true);
            });
            Vector relativeOffset = snapshot.relativeOffset().clone();
            displayBlocks.add(new DisplayTree.DisplayBlock(display, snapshot.blockData(), snapshot.originalLocation().clone(), relativeOffset));
        }
        return new DisplayTree(tree, displayBlocks);
    }
}
