package me.nakilex.levelplugin.environment;

import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SpeedUpScrollUtil {
    private static final NamespacedKey KEY_TYPE = new NamespacedKey(me.nakilex.levelplugin.Main.getInstance(), "speedup_scroll");
    private static final NamespacedKey KEY_SECONDS = new NamespacedKey(me.nakilex.levelplugin.Main.getInstance(), "speedup_seconds");

    public enum Tier {
        COMMON(ItemRarity.COMMON, 5 * 60, Material.PAPER),
        UNCOMMON(ItemRarity.UNCOMMON, 15 * 60, Material.MAP),
        RARE(ItemRarity.RARE, 45 * 60, Material.WRITABLE_BOOK),
        EPIC(ItemRarity.EPIC, 2 * 60 * 60, Material.ENCHANTED_BOOK);

        final ItemRarity rarity;
        final int seconds;
        final Material material;
        Tier(ItemRarity rarity, int seconds, Material material) { this.rarity = rarity; this.seconds = seconds; this.material = material; }
    }

    private SpeedUpScrollUtil() {}

    public static ItemStack create(Tier tier) {
        ItemStack item = new ItemStack(tier.material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(tier.rarity.getColor() + "Speed Up Scroll");
        List<String> lore = new ArrayList<>();
        lore.addAll(TooltipUtil.bulletList(
                ChatColor.GRAY + "Skips " + ChatColor.WHITE + formatDuration(tier.seconds) + ChatColor.GRAY + " of build time.",
                ChatColor.YELLOW + "Hold in hand and right-click build hologram"));
        lore.add(TooltipUtil.rarityGlyphLine(tier.rarity, "scroll"));
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(KEY_TYPE, PersistentDataType.STRING, tier.name());
        meta.getPersistentDataContainer().set(KEY_SECONDS, PersistentDataType.INTEGER, tier.seconds);
        item.setItemMeta(meta);
        ItemUtil.applyRarityTooltipStyle(item, tier.rarity);
        return item;
    }

    public static int getSeconds(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return 0;
        Integer seconds = stack.getItemMeta().getPersistentDataContainer().get(KEY_SECONDS, PersistentDataType.INTEGER);
        return seconds == null ? 0 : Math.max(0, seconds);
    }

    public static boolean isSpeedUpScroll(ItemStack stack) { return getSeconds(stack) > 0; }

    public static String formatDuration(long seconds) {
        long s = Math.max(0, seconds);
        long h = s / 3600; s %= 3600;
        long m = s / 60; s %= 60;
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }
}
