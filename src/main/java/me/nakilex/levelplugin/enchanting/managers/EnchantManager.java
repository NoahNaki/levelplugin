package me.nakilex.levelplugin.enchanting.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.tools.FarmingToolEnchant;
import me.nakilex.levelplugin.items.tools.WoodcuttingToolEnchant;
import me.nakilex.levelplugin.items.tools.MiningToolEnchant;
import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import me.nakilex.levelplugin.items.tools.ToolManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.items.managers.ItemManager;

import java.io.File;
import java.util.*;

/**
 * Handles applying random stat prefixes to items via the enchanting GUI.
 */
public class EnchantManager {
    private static final int BASE_COST = 200;

    private final Map<String, StatType> prefixMap = new HashMap<>();
    private final List<String> prefixList = new ArrayList<>();
    private final Random random = new Random();

    public EnchantManager() {
        File file = new File(Main.getInstance().getDataFolder(), "prefixes.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String key : cfg.getKeys(false)) {
            String prefix = cfg.getString(key);
            if (prefix == null) continue;
            StatType st = StatType.fromKey(key);
            if (st == null) st = StatType.VIT;
            prefixMap.put(prefix, st);
            prefixList.add(prefix);
        }
    }

    public int getEnchantCost(CustomItem item) {
        int count = item.getEnchantCount();
        return BASE_COST * (int) Math.pow(2, count);
    }

    public int getNextEnchantCost(CustomItem item) {
        if (item == null) return 0;
        return BASE_COST * (int) Math.pow(2, item.getEnchantCount() + 1);
    }

    public int getEnchantCost(ItemStack stack) {
        if (stack == null) return 0;
        me.nakilex.levelplugin.items.tools.CustomTool tool = ToolManager.getInstance().getTool(stack);
        if (tool != null) {
            if (tool.getDiscipline() == ToolDiscipline.FARMING) {
                int count = ToolManager.getInstance().getFarmingEnchantCount(stack);
                return BASE_COST * (int) Math.pow(2, count);
            }
            if (tool.getDiscipline() == ToolDiscipline.WOODCUTTING) {
                int count = ToolManager.getInstance().getWoodcuttingEnchantCount(stack);
                return BASE_COST * (int) Math.pow(2, count);
            }
            if (tool.getDiscipline() == ToolDiscipline.MINING) {
                int count = ToolManager.getInstance().getMiningEnchantCount(stack);
                return BASE_COST * (int) Math.pow(2, count);
            }
        }
        CustomItem item = ItemManager.getInstance().getCustomItemFromItemStack(stack);
        return item != null ? getEnchantCost(item) : 0;
    }

    public int getNextEnchantCost(ItemStack stack) {
        if (stack == null) return 0;
        me.nakilex.levelplugin.items.tools.CustomTool tool = ToolManager.getInstance().getTool(stack);
        if (tool != null) {
            if (tool.getDiscipline() == ToolDiscipline.FARMING) {
                int count = ToolManager.getInstance().getFarmingEnchantCount(stack);
                return BASE_COST * (int) Math.pow(2, count + 1);
            }
            if (tool.getDiscipline() == ToolDiscipline.WOODCUTTING) {
                int count = ToolManager.getInstance().getWoodcuttingEnchantCount(stack);
                return BASE_COST * (int) Math.pow(2, count + 1);
            }
            if (tool.getDiscipline() == ToolDiscipline.MINING) {
                int count = ToolManager.getInstance().getMiningEnchantCount(stack);
                return BASE_COST * (int) Math.pow(2, count + 1);
            }
        }
        CustomItem item = ItemManager.getInstance().getCustomItemFromItemStack(stack);
        return item != null ? getNextEnchantCost(item) : 0;
    }

    /** Apply a random prefix to the item, replacing any existing one. */
    public String enchant(Player player, ItemStack stack, CustomItem item) {
        if (item == null || stack == null) return null;
        // Strip any existing prefixes before applying a new one. In some cases
        // multiple prefixes may have been applied by older versions, so we
        // keep removing until none remain.
        String oldPrefix = getCurrentPrefix(item.getBaseName());
        while (oldPrefix != null) {
            StatType st = prefixMap.get(oldPrefix);
            if (st != null) {
                // remove the old bonus before stripping the text
                applyBonus(item, st, -20);
            }
            item.setBaseName(item.getBaseName().substring(oldPrefix.length()).trim());
            oldPrefix = getCurrentPrefix(item.getBaseName());
        }
        String prefix = prefixList.get(random.nextInt(prefixList.size()));
        StatType stat = prefixMap.get(prefix);
        applyBonus(item, stat, 20);
        item.setBaseName(prefix + " " + item.getBaseName());
        item.incrementEnchantCount();

        ItemStack updated = ItemUtil.createItemStackFromCustomItem(item, stack.getAmount(), player);
        ItemUtil.applyUpdatedStack(stack, updated);
        ItemManager.getInstance().addInstance(item);
        return prefix;
    }

    public FarmingToolEnchant enchantFarmingTool(Player player, ItemStack stack) {
        if (stack == null) return null;
        me.nakilex.levelplugin.items.tools.CustomTool tool = ToolManager.getInstance().getTool(stack);
        if (tool == null || tool.getDiscipline() != ToolDiscipline.FARMING) return null;
        FarmingToolEnchant[] enchants = FarmingToolEnchant.values();
        FarmingToolEnchant enchant = enchants[random.nextInt(enchants.length)];
        applyFarmingEnchant(stack, enchant);
        ToolManager.getInstance().incrementFarmingEnchantCount(stack);
        ItemUtil.updateCustomToolTooltip(stack, player);
        return enchant;
    }


    public WoodcuttingToolEnchant enchantWoodcuttingTool(Player player, ItemStack stack) {
        if (stack == null) return null;
        me.nakilex.levelplugin.items.tools.CustomTool tool = ToolManager.getInstance().getTool(stack);
        if (tool == null || tool.getDiscipline() != ToolDiscipline.WOODCUTTING) return null;
        WoodcuttingToolEnchant[] enchants = WoodcuttingToolEnchant.values();
        WoodcuttingToolEnchant enchant = enchants[random.nextInt(enchants.length)];
        applyWoodcuttingEnchant(stack, enchant);
        ToolManager.getInstance().incrementWoodcuttingEnchantCount(stack);
        ItemUtil.updateCustomToolTooltip(stack, player);
        return enchant;
    }

    private void applyWoodcuttingEnchant(ItemStack stack, WoodcuttingToolEnchant enchant) {
        if (stack == null || enchant == null) return;
        ToolManager toolManager = ToolManager.getInstance();
        WoodcuttingToolEnchant existing = toolManager.getWoodcuttingEnchant(stack);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String displayName = meta.getDisplayName();
            if (displayName == null || displayName.isBlank()) {
                displayName = stack.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
                displayName = displayName.substring(0, 1).toUpperCase(Locale.ROOT) + displayName.substring(1);
            }
            if (existing != null && displayName != null) {
                String prefix = existing.getDisplayName() + " ";
                if (displayName.startsWith(prefix)) {
                    displayName = displayName.substring(prefix.length());
                }
            }
            meta.setDisplayName(enchant.getDisplayName() + " " + displayName);
            stack.setItemMeta(meta);
        }
        toolManager.setWoodcuttingEnchant(stack, enchant);
    }

    private void applyFarmingEnchant(ItemStack stack, FarmingToolEnchant enchant) {
        if (stack == null || enchant == null) return;
        ToolManager toolManager = ToolManager.getInstance();
        FarmingToolEnchant existing = toolManager.getFarmingEnchant(stack);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String displayName = meta.getDisplayName();
            if (displayName == null || displayName.isBlank()) {
                displayName = stack.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
                displayName = displayName.substring(0, 1).toUpperCase(Locale.ROOT) + displayName.substring(1);
            }
            if (existing != null && displayName != null) {
                String prefix = existing.getDisplayName() + " ";
                if (displayName.startsWith(prefix)) {
                    displayName = displayName.substring(prefix.length());
                }
            }
            meta.setDisplayName(enchant.getDisplayName() + " " + displayName);
            stack.setItemMeta(meta);
        }
        toolManager.setFarmingEnchant(stack, enchant);
    }


    public MiningToolEnchant enchantMiningTool(Player player, ItemStack stack) {
        if (stack == null) return null;
        me.nakilex.levelplugin.items.tools.CustomTool tool = ToolManager.getInstance().getTool(stack);
        if (tool == null || tool.getDiscipline() != ToolDiscipline.MINING) return null;
        MiningToolEnchant[] enchants = MiningToolEnchant.values();
        MiningToolEnchant enchant = enchants[random.nextInt(enchants.length)];
        applyMiningEnchant(stack, enchant);
        ToolManager.getInstance().incrementMiningEnchantCount(stack);
        ItemUtil.updateCustomToolTooltip(stack, player);
        return enchant;
    }

    private void applyMiningEnchant(ItemStack stack, MiningToolEnchant enchant) {
        if (stack == null || enchant == null) return;
        ToolManager toolManager = ToolManager.getInstance();
        MiningToolEnchant existing = toolManager.getMiningEnchant(stack);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String displayName = meta.getDisplayName();
            if (displayName == null || displayName.isBlank()) {
                displayName = stack.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
                displayName = displayName.substring(0, 1).toUpperCase(Locale.ROOT) + displayName.substring(1);
            }
            if (existing != null && displayName != null) {
                String prefix = existing.getDisplayName() + " ";
                if (displayName.startsWith(prefix)) displayName = displayName.substring(prefix.length());
            }
            meta.setDisplayName(enchant.getDisplayName() + " " + displayName);
            stack.setItemMeta(meta);
        }
        toolManager.setMiningEnchant(stack, enchant);
    }

    private void applyBonus(CustomItem item, StatType stat, int amount) {
        item.adjustBonusStat(stat, amount);
    }

    private String getCurrentPrefix(String name) {
        for (String pre : prefixList) {
            if (name.startsWith(pre + " ")) return pre;
            if (name.equals(pre)) return pre;
        }
        return null;
    }
}
