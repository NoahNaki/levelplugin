package me.nakilex.levelplugin.booster;

import me.nakilex.levelplugin.utils.TextUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/** Utility for creating and identifying booster items. */
public final class BoosterItemUtil {

    private BoosterItemUtil() {}

    public static final NamespacedKey BOOSTER_KEY = new NamespacedKey(me.nakilex.levelplugin.Main.getInstance(), "global_booster");

    public static ItemStack createBoosterItem(BoosterType type, int amount, double multiplier) {
        ItemStack item = new ItemStack(type.icon());
        item.setAmount(Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(type.displayName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + "────────────");
            lore.add(ChatColor.GRAY + "Applies: " + ChatColor.WHITE + (type == BoosterType.COIN ? "Coins" : "Combat XP"));
            lore.add(ChatColor.GRAY + "Multiplier: " + ChatColor.WHITE + multiplier + "x");
            lore.add(ChatColor.GRAY + "Duration: " + ChatColor.WHITE + "1 hour");
            lore.add(ChatColor.DARK_GRAY + "────────────");
            lore.add(ChatColor.GREEN + "Use this item to start a serverwide");
            lore.add(ChatColor.GREEN + (type == BoosterType.COIN ? "coin boost." : "combat XP boost."));
            lore.add("");
            lore.addAll(TooltipUtil.clickInstructions(null, "to activate for everyone"));
            meta.setLore(lore);

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(BOOSTER_KEY, PersistentDataType.STRING, type.key());
            item.setItemMeta(meta);
            TextUtil.centerItemTooltip(item, true, false);
        }
        return item;
    }

    public static BoosterType getBoosterType(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
        String key = pdc.get(BOOSTER_KEY, PersistentDataType.STRING);
        if (key == null) return null;
        for (BoosterType type : BoosterType.values()) {
            if (type.key().equalsIgnoreCase(key)) return type;
        }
        return null;
    }
}
