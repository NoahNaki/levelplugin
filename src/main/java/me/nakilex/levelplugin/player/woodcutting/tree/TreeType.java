package me.nakilex.levelplugin.player.woodcutting.tree;

import org.bukkit.Material;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class TreeType {
    private final String key;
    private final Set<Material> logs;
    private final Set<Material> leaves;
    private final Set<Material> attachedNaturalBlocks;
    private final Material sapling;
    private final TreeHeuristic heuristic;

    public TreeType(String key, Set<Material> logs, Set<Material> leaves, Set<Material> attachedNaturalBlocks,
                    Material sapling, TreeHeuristic heuristic) {
        this.key = key;
        this.logs = immutableCopy(logs);
        this.leaves = immutableCopy(leaves);
        this.attachedNaturalBlocks = immutableCopy(attachedNaturalBlocks);
        this.sapling = sapling;
        this.heuristic = heuristic;
    }

    public String key() { return key; }
    public Set<Material> logs() { return logs; }
    public Set<Material> leaves() { return leaves; }
    public Set<Material> attachedNaturalBlocks() { return attachedNaturalBlocks; }
    public Material sapling() { return sapling; }
    public TreeHeuristic heuristic() { return heuristic; }
    public boolean isLog(Material material) { return logs.contains(material); }
    public boolean isLeaf(Material material) { return leaves.contains(material); }
    public boolean isAttachedNaturalBlock(Material material) { return attachedNaturalBlocks.contains(material); }
    public boolean isTreePart(Material material) { return isLog(material) || isLeaf(material) || isAttachedNaturalBlock(material); }
    public boolean canBeLargeTree() { return key.equals("DARK_OAK") || key.equals("JUNGLE") || key.equals("SPRUCE") || key.equals("PINE"); }

    private Set<Material> immutableCopy(Set<Material> materials) {
        return materials == null || materials.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(EnumSet.copyOf(materials));
    }
}
