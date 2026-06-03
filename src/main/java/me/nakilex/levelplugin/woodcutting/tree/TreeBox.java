package me.nakilex.levelplugin.woodcutting.tree;

import org.bukkit.block.Block;

public record TreeBox(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
    public static TreeBox fromRoot(Block root, TreeHeuristic heuristic) {
        int diameter = heuristic.diameter();
        return new TreeBox(root.getX() - diameter, root.getX() + diameter,
                root.getY(), root.getY() + heuristic.height(),
                root.getZ() - diameter, root.getZ() + diameter);
    }

    public TreeBox expand(int amount) {
        return new TreeBox(minX - amount, maxX + amount, minY - amount, maxY + amount, minZ - amount, maxZ + amount);
    }

    public boolean contains(Block block) {
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }
}
