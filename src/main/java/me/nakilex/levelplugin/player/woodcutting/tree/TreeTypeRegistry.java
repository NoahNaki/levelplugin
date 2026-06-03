package me.nakilex.levelplugin.player.woodcutting.tree;

import me.nakilex.levelplugin.player.woodcutting.WoodcuttingConfig;
import org.bukkit.Material;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

public class TreeTypeRegistry {
    private final WoodcuttingConfig config;

    public TreeTypeRegistry(WoodcuttingConfig config) { this.config = config; }

    public TreeType fromLog(Material material) {
        for (TreeType type : config.treeTypes().values()) if (type.isLog(material)) return type;
        return null;
    }

    public boolean isLog(Material material) { return fromLog(material) != null; }

    public boolean isWoodLike(Material material) {
        for (TreeType type : config.treeTypes().values()) {
            if (type.isTreePart(material)) return true;
        }
        return false;
    }

    public Set<Material> allConfiguredLeaves() {
        Set<Material> leaves = EnumSet.noneOf(Material.class);
        for (TreeType type : config.treeTypes().values()) leaves.addAll(type.leaves());
        return leaves;
    }

    public boolean isAnyConfiguredLeaf(Material material) {
        for (TreeType type : config.treeTypes().values()) if (type.isLeaf(material)) return true;
        return false;
    }

    public Collection<TreeType> types() { return config.treeTypes().values(); }
}
