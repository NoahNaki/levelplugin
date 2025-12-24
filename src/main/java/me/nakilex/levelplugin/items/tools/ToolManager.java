package me.nakilex.levelplugin.items.tools;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ToolManager {
    private static ToolManager instance;
    private final List<CustomTool> tools = new ArrayList<>();
    private final Map<ToolDiscipline, List<CustomTool>> toolsByDiscipline = new EnumMap<>(ToolDiscipline.class);
    private final Map<Material, CustomTool> materialLookup = new HashMap<>();

    public ToolManager() {
        instance = this;
        loadDefaults();
    }

    public static ToolManager getInstance() {
        return instance;
    }

    private void loadDefaults() {
        addTool(Material.WOODEN_PICKAXE, ToolTier.TIER_I, ToolDiscipline.MINING);
        addTool(Material.STONE_PICKAXE, ToolTier.TIER_II, ToolDiscipline.MINING);
        addTool(Material.GOLDEN_PICKAXE, ToolTier.TIER_III, ToolDiscipline.MINING);
        addTool(Material.IRON_PICKAXE, ToolTier.TIER_IV, ToolDiscipline.MINING);
        addTool(Material.DIAMOND_PICKAXE, ToolTier.TIER_V, ToolDiscipline.MINING);
        addTool(Material.NETHERITE_PICKAXE, ToolTier.TIER_VI, ToolDiscipline.MINING);

        addTool(Material.WOODEN_HOE, ToolTier.TIER_I, ToolDiscipline.FARMING);
        addTool(Material.STONE_HOE, ToolTier.TIER_II, ToolDiscipline.FARMING);
        addTool(Material.GOLDEN_HOE, ToolTier.TIER_III, ToolDiscipline.FARMING);
        addTool(Material.IRON_HOE, ToolTier.TIER_IV, ToolDiscipline.FARMING);
        addTool(Material.DIAMOND_HOE, ToolTier.TIER_V, ToolDiscipline.FARMING);
        addTool(Material.NETHERITE_HOE, ToolTier.TIER_VI, ToolDiscipline.FARMING);

        addTool(Material.FISHING_ROD, ToolTier.TIER_I, ToolDiscipline.FISHING);
    }

    private void addTool(Material mat, ToolTier tier, ToolDiscipline discipline) {
        String suffix = discipline == ToolDiscipline.MINING ? " Pickaxe" : " Scythe";
        String name = "Tier " + tier.getTierName() + suffix;
        CustomTool tool = new CustomTool(UUID.randomUUID(), name, mat, tier, discipline);
        tools.add(tool);
        toolsByDiscipline.computeIfAbsent(discipline, k -> new ArrayList<>()).add(tool);
        materialLookup.put(mat, tool);
    }

    public List<CustomTool> getTools() {
        return Collections.unmodifiableList(tools);
    }

    public List<CustomTool> getTools(ToolDiscipline discipline) {
        return toolsByDiscipline.getOrDefault(discipline, Collections.emptyList());
    }

    public CustomTool getTool(ToolTier tier) {
        for (CustomTool tool : tools) {
            if (tool.getTier() == tier) {
                return tool;
            }
        }
        return null;
    }

    public CustomTool getTool(ToolTier tier, ToolDiscipline discipline) {
        for (CustomTool tool : toolsByDiscipline.getOrDefault(discipline, Collections.emptyList())) {
            if (tool.getTier() == tier) {
                return tool;
            }
        }
        return null;
    }

    public CustomTool getTool(Material material) {
        return materialLookup.get(material);
    }

    public boolean isToolMaterial(Material material) {
        return materialLookup.containsKey(material);
    }
}
