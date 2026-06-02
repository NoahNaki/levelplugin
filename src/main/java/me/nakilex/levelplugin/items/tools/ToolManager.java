package me.nakilex.levelplugin.items.tools;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
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
    private final NamespacedKey farmingEnchantKey = new NamespacedKey(Main.getInstance(), "farming_enchant");
    private final NamespacedKey farmingEnchantCountKey = new NamespacedKey(Main.getInstance(), "farming_enchant_count");
    private final NamespacedKey woodcuttingEnchantKey = new NamespacedKey(Main.getInstance(), "woodcutting_enchant");
    private final NamespacedKey woodcuttingEnchantCountKey = new NamespacedKey(Main.getInstance(), "woodcutting_enchant_count");
    private final NamespacedKey miningEnchantKey = new NamespacedKey(Main.getInstance(), "mining_enchant");
    private final NamespacedKey miningEnchantCountKey = new NamespacedKey(Main.getInstance(), "mining_enchant_count");
    private final NamespacedKey fishingEnchantKey = new NamespacedKey(Main.getInstance(), "fishing_enchant");
    private final NamespacedKey fishingEnchantCountKey = new NamespacedKey(Main.getInstance(), "fishing_enchant_count");

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

        addTool(Material.WOODEN_AXE, ToolTier.TIER_I, ToolDiscipline.WOODCUTTING);
        addTool(Material.STONE_AXE, ToolTier.TIER_II, ToolDiscipline.WOODCUTTING);
        addTool(Material.GOLDEN_AXE, ToolTier.TIER_III, ToolDiscipline.WOODCUTTING);
        addTool(Material.IRON_AXE, ToolTier.TIER_IV, ToolDiscipline.WOODCUTTING);
        addTool(Material.DIAMOND_AXE, ToolTier.TIER_V, ToolDiscipline.WOODCUTTING);
        addTool(Material.NETHERITE_AXE, ToolTier.TIER_VI, ToolDiscipline.WOODCUTTING);

        addTool(Material.WOODEN_HOE, ToolTier.TIER_I, ToolDiscipline.FARMING);
        addTool(Material.STONE_HOE, ToolTier.TIER_II, ToolDiscipline.FARMING);
        addTool(Material.GOLDEN_HOE, ToolTier.TIER_III, ToolDiscipline.FARMING);
        addTool(Material.IRON_HOE, ToolTier.TIER_IV, ToolDiscipline.FARMING);
        addTool(Material.DIAMOND_HOE, ToolTier.TIER_V, ToolDiscipline.FARMING);
        addTool(Material.NETHERITE_HOE, ToolTier.TIER_VI, ToolDiscipline.FARMING);

        addTool(Material.FISHING_ROD, ToolTier.TIER_I, ToolDiscipline.FISHING, null);
        addTool(Material.FISHING_ROD, ToolTier.TIER_II, ToolDiscipline.FISHING, "beginner_rod");
        addTool(Material.FISHING_ROD, ToolTier.TIER_III, ToolDiscipline.FISHING, "silver_rod");
        addTool(Material.FISHING_ROD, ToolTier.TIER_IV, ToolDiscipline.FISHING, "star_rod");
        addTool(Material.FISHING_ROD, ToolTier.TIER_V, ToolDiscipline.FISHING, "bone_rod");
        addTool(Material.FISHING_ROD, ToolTier.TIER_VI, ToolDiscipline.FISHING, "magical_rod");
    }

    private void addTool(Material mat, ToolTier tier, ToolDiscipline discipline) {
        addTool(mat, tier, discipline, null);
    }

    private void addTool(Material mat, ToolTier tier, ToolDiscipline discipline, String nexoId) {
        String suffix = switch (discipline) {
            case MINING -> " Pickaxe";
            case FARMING -> " Scythe";
            case FISHING -> " Fishing Rod";
            case WOODCUTTING -> " Axe";
        };
        String name = "Tier " + tier.getTierName() + suffix;
        CustomTool tool = new CustomTool(UUID.randomUUID(), name, mat, tier, discipline, nexoId);
        tools.add(tool);
        toolsByDiscipline.computeIfAbsent(discipline, k -> new ArrayList<>()).add(tool);
        materialLookup.putIfAbsent(mat, tool);
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
        return getTool(stack, true);
    }

    public CustomTool getTool(ItemStack stack, boolean allowMaterialFallback) {
        if (stack == null || !stack.hasItemMeta()) {
            return allowMaterialFallback ? getTool(stack != null ? stack.getType() : null) : null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return allowMaterialFallback ? getTool(stack.getType()) : null;
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
        return allowMaterialFallback ? getTool(stack.getType()) : null;
    }

    public int getPlayerLevel(Player player, ToolDiscipline discipline) {
        if (player == null || discipline == null) {
            return 0;
        }
        return switch (discipline) {
            case FARMING -> Main.getInstance().getFarmingManager().getLevel(player);
            case FISHING -> Main.getInstance().getFishingManager().getLevel(player);
            case MINING -> Main.getInstance().getMiningManager().getLevel(player);
            case WOODCUTTING -> Main.getInstance().getWoodcuttingManager().getLevel(player);
        };
    }

    public boolean meetsLevelRequirement(Player player, CustomTool tool) {
        if (player == null || tool == null) {
            return true;
        }
        int level = getPlayerLevel(player, tool.getDiscipline());
        return level >= tool.getTier().getLevelRequirement();
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

    public FarmingToolEnchant getFarmingEnchant(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        String value = meta.getPersistentDataContainer().get(farmingEnchantKey, PersistentDataType.STRING);
        return FarmingToolEnchant.fromKey(value);
    }

    public int getFarmingEnchantCount(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return 0;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return 0;
        return meta.getPersistentDataContainer().getOrDefault(farmingEnchantCountKey, PersistentDataType.INTEGER, 0);
    }

    public void setFarmingEnchant(ItemStack stack, FarmingToolEnchant enchant) {
        if (stack == null || !stack.hasItemMeta()) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (enchant != null) {
            container.set(farmingEnchantKey, PersistentDataType.STRING, enchant.getKey());
        } else {
            container.remove(farmingEnchantKey);
        }
        stack.setItemMeta(meta);
    }

    public void incrementFarmingEnchantCount(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        int current = container.getOrDefault(farmingEnchantCountKey, PersistentDataType.INTEGER, 0);
        container.set(farmingEnchantCountKey, PersistentDataType.INTEGER, current + 1);
        stack.setItemMeta(meta);
    }


    public WoodcuttingToolEnchant getWoodcuttingEnchant(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        String value = meta.getPersistentDataContainer().get(woodcuttingEnchantKey, PersistentDataType.STRING);
        return WoodcuttingToolEnchant.fromKey(value);
    }

    public int getWoodcuttingEnchantCount(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return 0;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return 0;
        return meta.getPersistentDataContainer().getOrDefault(woodcuttingEnchantCountKey, PersistentDataType.INTEGER, 0);
    }

    public void setWoodcuttingEnchant(ItemStack stack, WoodcuttingToolEnchant enchant) {
        if (stack == null || !stack.hasItemMeta()) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (enchant != null) {
            container.set(woodcuttingEnchantKey, PersistentDataType.STRING, enchant.getKey());
        } else {
            container.remove(woodcuttingEnchantKey);
        }
        stack.setItemMeta(meta);
    }

    public void incrementWoodcuttingEnchantCount(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        int current = container.getOrDefault(woodcuttingEnchantCountKey, PersistentDataType.INTEGER, 0);
        container.set(woodcuttingEnchantCountKey, PersistentDataType.INTEGER, current + 1);
        stack.setItemMeta(meta);
    }


    public MiningToolEnchant getMiningEnchant(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        String value = meta.getPersistentDataContainer().get(miningEnchantKey, PersistentDataType.STRING);
        return MiningToolEnchant.fromKey(value);
    }

    public int getMiningEnchantCount(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return 0;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return 0;
        return meta.getPersistentDataContainer().getOrDefault(miningEnchantCountKey, PersistentDataType.INTEGER, 0);
    }

    public void setMiningEnchant(ItemStack stack, MiningToolEnchant enchant) {
        if (stack == null || !stack.hasItemMeta()) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (enchant != null) container.set(miningEnchantKey, PersistentDataType.STRING, enchant.getKey());
        else container.remove(miningEnchantKey);
        stack.setItemMeta(meta);
    }

    public void incrementMiningEnchantCount(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        int current = container.getOrDefault(miningEnchantCountKey, PersistentDataType.INTEGER, 0);
        container.set(miningEnchantCountKey, PersistentDataType.INTEGER, current + 1);
        stack.setItemMeta(meta);
    }

    public FishingToolEnchant getFishingEnchant(ItemStack stack) {
        return FishingToolEnchant.fromKey(getStringData(stack, fishingEnchantKey));
    }

    public int getFishingEnchantCount(ItemStack stack) {
        return getIntData(stack, fishingEnchantCountKey);
    }

    public void setFishingEnchant(ItemStack stack, FishingToolEnchant enchant) {
        setStringData(stack, fishingEnchantKey, enchant == null ? null : enchant.getKey());
    }

    public void incrementFishingEnchantCount(ItemStack stack) {
        incrementIntData(stack, fishingEnchantCountKey);
    }

    private String getStringData(ItemStack stack, NamespacedKey key) {
        if (stack == null || !stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        return meta == null ? null : meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    private int getIntData(ItemStack stack, NamespacedKey key) {
        if (stack == null || !stack.hasItemMeta()) return 0;
        ItemMeta meta = stack.getItemMeta();
        return meta == null ? 0 : meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.INTEGER, 0);
    }

    private void setStringData(ItemStack stack, NamespacedKey key, String value) {
        if (stack == null || !stack.hasItemMeta()) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        if (value == null) meta.getPersistentDataContainer().remove(key);
        else meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        stack.setItemMeta(meta);
    }

    private void incrementIntData(ItemStack stack, NamespacedKey key) {
        if (stack == null || !stack.hasItemMeta()) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(key, PersistentDataType.INTEGER, container.getOrDefault(key, PersistentDataType.INTEGER, 0) + 1);
        stack.setItemMeta(meta);
    }

    public ItemStack createToolItem(CustomTool tool, org.bukkit.entity.Player viewer) {
        ItemStack stack;
        String name = tool.getTier().getRarity().getColor() + tool.getName();
        if (tool.getNexoId() != null) {
            stack = GuiUtil.getNexoItem(tool.getNexoId(), name);
        } else {
            stack = new ItemStack(tool.getMaterial());
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(name);
                stack.setItemMeta(meta);
            }
        }
        applyToolData(stack, tool);
        ItemUtil.updateCustomToolTooltip(stack, viewer);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES, org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public boolean isToolMaterial(Material material) {
        return materialLookup.containsKey(material);
    }
}
