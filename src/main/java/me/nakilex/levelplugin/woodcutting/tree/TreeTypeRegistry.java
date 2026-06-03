package me.nakilex.levelplugin.woodcutting.tree;

import me.nakilex.levelplugin.woodcutting.WoodcuttingConfig;
import org.bukkit.Material;

import java.util.Collection;

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
            if (type.isLog(material) || type.isLeafOrAttachedNaturalBlock(material)) return true;
        }
        return false;
    }

    public Collection<TreeType> types() { return config.treeTypes().values(); }
}
