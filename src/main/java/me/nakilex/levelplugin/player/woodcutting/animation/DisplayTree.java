package me.nakilex.levelplugin.player.woodcutting.animation;

import me.nakilex.levelplugin.player.woodcutting.tree.TreeDetectionResult;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Vector;

import java.util.List;

public class DisplayTree {
    private final TreeDetectionResult result;
    private final List<DisplayBlock> blocks;

    public DisplayTree(TreeDetectionResult result, List<DisplayBlock> blocks) {
        this.result = result;
        this.blocks = List.copyOf(blocks);
    }

    public TreeDetectionResult result() { return result; }
    public List<DisplayBlock> blocks() { return blocks; }
    public void removeDisplays() { blocks.forEach(block -> { if (!block.display().isDead()) block.display().remove(); }); }

    public record DisplayBlock(BlockDisplay display, BlockData data, Location originalLocation, Vector relativeOffset) {}
}
