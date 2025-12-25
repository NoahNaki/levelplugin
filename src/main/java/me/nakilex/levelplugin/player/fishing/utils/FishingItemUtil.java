package me.nakilex.levelplugin.player.fishing.utils;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.player.fishing.data.FishDefinition;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FishingItemUtil {

    private static final NamespacedKey FISH_ID_KEY = new NamespacedKey(Main.getInstance(), "fish_id");
    private static final NamespacedKey FISH_SIZE_KEY = new NamespacedKey(Main.getInstance(), "fish_size");
    private static final NamespacedKey FISH_RARITY_KEY = new NamespacedKey(Main.getInstance(), "fish_rarity");
    private static final NamespacedKey FISH_VALUE_KEY = new NamespacedKey(Main.getInstance(), "fish_value");
    private static final NamespacedKey FISH_UUID_KEY = new NamespacedKey(Main.getInstance(), "fish_uuid");

    private FishingItemUtil() {}

    public static ItemStack createFishItem(FishDefinition definition, double size) {
        ItemRarity rarity = definition.rarity();
        String nexoId = selectNexoId(definition, size);
        ItemStack stack = nexoId != null
                ? GuiUtil.getNexoItem(nexoId, rarity.getColor() + definition.displayName())
                : new ItemStack(Material.COD);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        meta.setDisplayName(rarity.getColor() + definition.displayName());

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(FISH_ID_KEY, PersistentDataType.STRING, definition.id());
        container.set(FISH_SIZE_KEY, PersistentDataType.DOUBLE, size);
        container.set(FISH_RARITY_KEY, PersistentDataType.STRING, rarity.name());
        container.set(FISH_VALUE_KEY, PersistentDataType.INTEGER, definition.sellValue());
        container.set(FISH_UUID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());

        List<String> lore = new ArrayList<>();
        lore.add(rarity.getSymbol());
        lore.add("");
        lore.add(ChatColor.GRAY + "Fish Type: " + ChatColor.WHITE + definition.displayName());
        lore.add(ChatColor.GRAY + "Size: " + ChatColor.WHITE + formatSize(size));
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);

        ItemUtil.applyRarityTooltipStyle(stack, rarity);
        return stack;
    }

    private static String formatSize(double size) {
        return String.format("%.1f cm", size);
    }

    private static String selectNexoId(FishDefinition definition, double size) {
        String base = definition.baseNexoId();
        if (base == null || base.isBlank()) {
            return null;
        }
        double min = definition.minSize();
        double max = Math.max(min, definition.maxSize());
        double range = Math.max(1.0, max - min);
        double normalized = (size - min) / range;
        if (normalized >= 0.8 && definition.goldNexoId() != null && !definition.goldNexoId().isBlank()) {
            return definition.goldNexoId();
        }
        if (normalized >= 0.5 && definition.silverNexoId() != null && !definition.silverNexoId().isBlank()) {
            return definition.silverNexoId();
        }
        return base;
    }

    public static boolean isFish(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(FISH_ID_KEY, PersistentDataType.STRING);
    }

    public static int getFishValue(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return 0;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return 0;
        return meta.getPersistentDataContainer().getOrDefault(FISH_VALUE_KEY, PersistentDataType.INTEGER, 0);
    }

    public static double getFishSize(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return 0.0;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return 0.0;
        return meta.getPersistentDataContainer().getOrDefault(FISH_SIZE_KEY, PersistentDataType.DOUBLE, 0.0);
    }

    public static ItemRarity getFishRarity(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return ItemRarity.COMMON;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return ItemRarity.COMMON;
        String rarity = meta.getPersistentDataContainer().get(FISH_RARITY_KEY, PersistentDataType.STRING);
        if (rarity == null) return ItemRarity.COMMON;
        try {
            return ItemRarity.valueOf(rarity);
        } catch (IllegalArgumentException e) {
            return ItemRarity.COMMON;
        }
    }

    public static String getFishDisplayName(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return "Fish";
        ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return "Fish";
        return meta.getDisplayName();
    }
}
