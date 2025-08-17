package me.nakilex.levelplugin.enchanting.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
            StatType st = mapKey(key);
            prefixMap.put(prefix, st);
            prefixList.add(prefix);
        }
    }

    private StatType mapKey(String key) {
        return switch (key.toLowerCase()) {
            case "strength" -> StatType.STR;
            case "agility" -> StatType.AGI;
            case "dexterity" -> StatType.DEX;
            case "intelligence" -> StatType.INT;
            case "defense", "hp", "vitality" -> StatType.VIT;
            case "will" -> StatType.WIL;
            case "technique" -> StatType.TEC;
            default -> StatType.VIT;
        };
    }

    public int getEnchantCost(CustomItem item) {
        int count = item.getEnchantCount();
        return BASE_COST * (int) Math.pow(2, count);
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
