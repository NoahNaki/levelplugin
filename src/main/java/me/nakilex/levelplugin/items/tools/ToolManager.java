package me.nakilex.levelplugin.items.tools;

import me.nakilex.levelplugin.Main;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

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
    private final NamespacedKey toolTierKey = new NamespacedKey(Main.getInstance(), "tool_tier");
    private final NamespacedKey toolDisciplineKey = new NamespacedKey(Main.getInstance(), "tool_discipline");

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
        addTool(Material.FISHING_ROD, ToolTier.TIER_II, ToolDiscipline.FISHING);
        addTool(Material.FISHING_ROD, ToolTier.TIER_III, ToolDiscipline.FISHING);
        addTool(Material.FISHING_ROD, ToolTier.TIER_IV, ToolDiscipline.FISHING);
        addTool(Material.FISHING_ROD, ToolTier.TIER_V, ToolDiscipline.FISHING);
        addTool(Material.FISHING_ROD, ToolTier.TIER_VI, ToolDiscipline.FISHING);
    }

    private void addTool(Material mat, ToolTier tier, ToolDiscipline discipline) {
        String suffix = switch (discipline) {
            case MINING -> " Pickaxe";
            case FARMING -> " Scythe";
            case FISHING -> " Fishing Rod";
        };
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

    public CustomTool getTool(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return getTool(stack != null ? stack.getType() : null);
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return getTool(stack.getType());
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String tierName = container.get(toolTierKey, PersistentDataType.STRING);
        String disciplineName = container.get(toolDisciplineKey, PersistentDataType.STRING);
        if (tierName != null && disciplineName != null) {
            try {
                ToolTier tier = ToolTier.valueOf(tierName);
                ToolDiscipline discipline = ToolDiscipline.valueOf(disciplineName);
                CustomTool tool = getTool(tier, discipline);
                if (tool != null) {
                    return tool;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return getTool(stack.getType());
    }

    public void applyToolData(ItemStack stack, CustomTool tool) {
        if (stack == null || tool == null) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(toolTierKey, PersistentDataType.STRING, tool.getTier().name());
        container.set(toolDisciplineKey, PersistentDataType.STRING, tool.getDiscipline().name());
        stack.setItemMeta(meta);
    }

    public boolean isToolMaterial(Material material) {
        return materialLookup.containsKey(material);
    }
}
