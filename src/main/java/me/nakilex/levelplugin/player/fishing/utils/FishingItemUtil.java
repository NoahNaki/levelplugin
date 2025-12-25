package me.nakilex.levelplugin.player.fishing.utils;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.fishing.data.FishDefinition;
import me.nakilex.levelplugin.utils.TextUtil;
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
        ItemStack stack = new ItemStack(Material.COD);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        ItemRarity rarity = definition.rarity();
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
        lore.add(ChatColor.GRAY + "Rarity: " + rarity.getColor() + TextUtil.beautifyWords(rarity.name()));
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);

        ItemUtil.applyRarityTooltipStyle(stack, rarity);
        return stack;
    }

    private static String formatSize(double size) {
        return String.format("%.1f cm", size);
    }
}
