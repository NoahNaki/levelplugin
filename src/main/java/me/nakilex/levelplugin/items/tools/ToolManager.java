package me.nakilex.levelplugin.items.tools;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ToolManager {
    private static ToolManager instance;
    private final List<CustomTool> tools = new ArrayList<>();

    public ToolManager() {
        instance = this;
        loadDefaults();
    }

    public static ToolManager getInstance() {
        return instance;
    }

    private void loadDefaults() {
        addTool("Tier I Pickaxe", Material.WOODEN_PICKAXE, ToolTier.TIER_I);
        addTool("Tier II Pickaxe", Material.STONE_PICKAXE, ToolTier.TIER_II);
        addTool("Tier III Pickaxe", Material.GOLDEN_PICKAXE, ToolTier.TIER_III);
        addTool("Tier IV Pickaxe", Material.IRON_PICKAXE, ToolTier.TIER_IV);
        addTool("Tier V Pickaxe", Material.DIAMOND_PICKAXE, ToolTier.TIER_V);
        addTool("Tier VI Pickaxe", Material.NETHERITE_PICKAXE, ToolTier.TIER_VI);
    }

    private void addTool(String name, Material mat, ToolTier tier) {
        tools.add(new CustomTool(UUID.randomUUID(), name, mat, tier));
    }

    public List<CustomTool> getTools() {
        return Collections.unmodifiableList(tools);
    }
}
