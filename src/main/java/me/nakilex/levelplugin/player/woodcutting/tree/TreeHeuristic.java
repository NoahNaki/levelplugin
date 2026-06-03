package me.nakilex.levelplugin.player.woodcutting.tree;

public record TreeHeuristic(int diameter, int height) {
    public TreeHeuristic {
        diameter = Math.max(1, diameter);
        height = Math.max(4, height);
    }
}
