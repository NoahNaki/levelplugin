package me.nakilex.levelplugin.woodcutting.tree;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class TreeRootFinder {
    public Block findRoot(Block clicked, TreeType type) {
        Block current = clicked;
        while (current.getY() > current.getWorld().getMinHeight()) {
            Block below = current.getRelative(BlockFace.DOWN);
            if (!type.isLog(below.getType())) break;
            current = below;
        }
        return current;
    }
}
