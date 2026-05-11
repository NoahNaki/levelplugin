package me.nakilex.levelplugin.spells.deck;

import me.nakilex.levelplugin.items.data.ItemRarity;
import org.bukkit.ChatColor;
import org.bukkit.Material;

public enum SpellDeckRarity {
    COMMON(ItemRarity.COMMON, ChatColor.GRAY, Material.LIGHT_GRAY_WOOL),
    UNCOMMON(ItemRarity.UNCOMMON, ChatColor.GREEN, Material.LIME_WOOL),
    RARE(ItemRarity.RARE, ChatColor.BLUE, Material.BLUE_WOOL),
    EPIC(ItemRarity.EPIC, ChatColor.LIGHT_PURPLE, Material.MAGENTA_WOOL),
    LEGENDARY(ItemRarity.LEGENDARY, ChatColor.GOLD, Material.ORANGE_WOOL),
    MYTHIC(ItemRarity.MYTHIC, ChatColor.RED, Material.RED_WOOL);

    private final ItemRarity itemRarity;
    private final ChatColor color;
    private final Material displayMaterial;

    SpellDeckRarity(ItemRarity itemRarity, ChatColor color, Material displayMaterial) {
        this.itemRarity = itemRarity;
        this.color = color;
        this.displayMaterial = displayMaterial;
    }

    public ItemRarity itemRarity() {
        return itemRarity;
    }

    public ChatColor color() {
        return color;
    }

    public Material displayMaterial() {
        return displayMaterial;
    }

    public String displayName() {
        String lower = name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
