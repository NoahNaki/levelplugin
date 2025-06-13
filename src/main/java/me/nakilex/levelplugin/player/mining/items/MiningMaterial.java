package me.nakilex.levelplugin.player.mining.items;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public enum MiningMaterial {
    COAL(org.bukkit.Material.COAL, "Coal"),
    COPPER(org.bukkit.Material.RAW_COPPER, "Raw Copper"),
    IRON(org.bukkit.Material.RAW_IRON, "Raw Iron"),
    GOLD(org.bukkit.Material.RAW_GOLD, "Raw Gold"),
    QUARTZ(org.bukkit.Material.QUARTZ, "Quartz"),
    AMETHYST(org.bukkit.Material.AMETHYST_SHARD, "Amethyst Shard"),
    REDSTONE(org.bukkit.Material.REDSTONE, "Redstone"),
    LAPIS(org.bukkit.Material.LAPIS_LAZULI, "Lapis Lazuli"),
    DIAMOND(org.bukkit.Material.DIAMOND, "Diamond"),
    EMERALD(org.bukkit.Material.EMERALD, "Emerald"),
    NETHERITE(org.bukkit.Material.NETHERITE_SCRAP, "Netherite Scrap");

    private final org.bukkit.Material material;
    private final String display;

    MiningMaterial(org.bukkit.Material mat, String name) {
        this.material = mat;
        this.display = name;
    }

    public ItemStack createItem(int amount) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + display);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static MiningMaterial fromOre(String ore) {
        return switch (ore) {
            case "coal_ore" -> COAL;
            case "copper_ore" -> COPPER;
            case "iron_ore" -> IRON;
            case "gold_ore" -> GOLD;
            case "quartz_ore" -> QUARTZ;
            case "amethyst_ore" -> AMETHYST;
            case "redstone_ore" -> REDSTONE;
            case "lapis_ore" -> LAPIS;
            case "diamond_ore" -> DIAMOND;
            case "emerald_ore" -> EMERALD;
            case "netherite_ore" -> NETHERITE;
            default -> null;
        };
    }
}
