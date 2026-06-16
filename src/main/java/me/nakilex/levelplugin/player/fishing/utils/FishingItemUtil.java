package me.nakilex.levelplugin.player.fishing.utils;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.player.fishing.data.FishDefinition;
import me.nakilex.levelplugin.player.fishing.data.FishingQuality;
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
    private static final NamespacedKey FISH_QUALITY_KEY = new NamespacedKey(Main.getInstance(), "fish_quality");
    private static final NamespacedKey FISH_VALUE_KEY = new NamespacedKey(Main.getInstance(), "fish_value");
    private static final NamespacedKey FISH_UUID_KEY = new NamespacedKey(Main.getInstance(), "fish_uuid");

    private FishingItemUtil() {}

    public static ItemStack createFishItem(FishDefinition definition, double size) {
        ItemRarity rarity = definition.rarity();
        FishingQuality quality = FishingQuality.fromSize(definition, size);
        String nexoId = selectNexoId(definition, quality);
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
        container.set(FISH_QUALITY_KEY, PersistentDataType.STRING, quality.name());
        container.set(FISH_VALUE_KEY, PersistentDataType.INTEGER, definition.sellValue());
        container.set(FISH_UUID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());

        List<String> lore = new ArrayList<>();
        lore.add(rarity.getSymbol());
        lore.add("");
        lore.add(ChatColor.GRAY + "Fish Type: " + ChatColor.WHITE + definition.displayName());
        lore.add(ChatColor.GRAY + "Size: " + ChatColor.WHITE + formatSize(size));
        lore.add(ChatColor.GRAY + "Quality: " + quality.getColor() + quality.getDisplayName());
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);

        ItemUtil.applyRarityTooltipStyle(stack, rarity);
        return stack;
    }

    private static String formatSize(double size) {
        return String.format("%.1f cm", size);
    }

    private static String selectNexoId(FishDefinition definition, FishingQuality quality) {
        String base = definition.baseNexoId();
        if (base == null || base.isBlank()) {
            return null;
        }
        if (quality == FishingQuality.GOLD && definition.goldNexoId() != null && !definition.goldNexoId().isBlank()) {
            return definition.goldNexoId();
        }
        if (quality.ordinal() >= FishingQuality.SILVER.ordinal() && definition.silverNexoId() != null && !definition.silverNexoId().isBlank()) {
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

    public static String getFishId(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(FISH_ID_KEY, PersistentDataType.STRING);
    }

    public static int getFishValue(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return 0;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return 0;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        int base = container.getOrDefault(FISH_VALUE_KEY, PersistentDataType.INTEGER, 0);
        double size = container.getOrDefault(FISH_SIZE_KEY, PersistentDataType.DOUBLE, 0.0);
        ItemRarity rarity = getFishRarity(stack);
        FishingQuality quality = qualityFromStoredFish(stack);
        double sizeMultiplier = 0.85 + (Math.min(1.0, Math.max(0.0, size / 150.0)) * 0.45);
        double rarityMultiplier = 1.0 + (rarity.ordinal() * 0.18);
        return Math.max(1, (int) Math.round(base * 5 * sizeMultiplier * rarityMultiplier * quality.getValueMultiplier()));
    }

    public static double getFishSize(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return 0.0;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return 0.0;
        return meta.getPersistentDataContainer().getOrDefault(FISH_SIZE_KEY, PersistentDataType.DOUBLE, 0.0);
    }

    public static FishingQuality qualityFromStoredFish(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return FishingQuality.NORMAL;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return FishingQuality.NORMAL;
        String quality = meta.getPersistentDataContainer().get(FISH_QUALITY_KEY, PersistentDataType.STRING);
        if (quality == null) return FishingQuality.NORMAL;
        try {
            return FishingQuality.valueOf(quality);
        } catch (IllegalArgumentException ignored) {
            return FishingQuality.NORMAL;
        }
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
