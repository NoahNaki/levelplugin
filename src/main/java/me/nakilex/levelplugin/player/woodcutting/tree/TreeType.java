package me.nakilex.levelplugin.player.woodcutting.tree;

import org.bukkit.Material;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class TreeType {
    private final String key;
    private final Set<Material> logs;
    private final Set<Material> leaves;
    private final Material sapling;
    private final TreeHeuristic heuristic;

    public TreeType(String key, Set<Material> logs, Set<Material> leaves, Material sapling, TreeHeuristic heuristic) {
        this.key = key;
        this.logs = logs.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(EnumSet.copyOf(logs));
        this.leaves = leaves.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(EnumSet.copyOf(leaves));
        this.sapling = sapling;
        this.heuristic = heuristic;
    }

    public String key() { return key; }
    public Set<Material> logs() { return logs; }
    public Set<Material> leaves() { return leaves; }
    public Material sapling() { return sapling; }
    public TreeHeuristic heuristic() { return heuristic; }
    public boolean isLog(Material material) { return logs.contains(material); }
    public boolean isLeafOrAttachedNaturalBlock(Material material) { return leaves.contains(material); }
    public boolean canBeLargeTree() { return key.equals("DARK_OAK") || key.equals("JUNGLE") || key.equals("SPRUCE"); }
}
