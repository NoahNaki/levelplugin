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
        addTool(Material.WOODEN_PICKAXE, ToolTier.TIER_I);
        addTool(Material.STONE_PICKAXE, ToolTier.TIER_II);
        addTool(Material.GOLDEN_PICKAXE, ToolTier.TIER_III);
        addTool(Material.IRON_PICKAXE, ToolTier.TIER_IV);
        addTool(Material.DIAMOND_PICKAXE, ToolTier.TIER_V);
        addTool(Material.NETHERITE_PICKAXE, ToolTier.TIER_VI);
    }

    private void addTool(Material mat, ToolTier tier) {
        String name = "Tier " + tier.getTierName() + " Pickaxe";
        tools.add(new CustomTool(UUID.randomUUID(), name, mat, tier));
    }

    public List<CustomTool> getTools() {
        return Collections.unmodifiableList(tools);
    }
}
