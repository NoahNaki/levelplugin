package me.nakilex.levelplugin.mercenary;

import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a friendship gift that players can hand to mercenary NPCs.
 * Gifts are defined in {@code mercenaries.yml} so designers can tweak
 * their look, feel, and affinity value without code changes.
 */
public final class MercenaryGift {
    private final String id;
    private final ItemStack icon;
    private final int affinityValue;

    public MercenaryGift(String id, Material material, String displayName, List<String> lore, int affinityValue) {
        this.id = id;
        this.icon = new ItemStack(material);
        this.affinityValue = affinityValue;

        ItemMeta meta = this.icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            List<String> finalLore = new ArrayList<>();
            if (lore != null) {
                for (String line : lore) {
                    finalLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
            }
            finalLore.addAll(TooltipUtil.bulletList(
                    "Gives " + affinityValue + " affinity",
                    "Sneak-right-click a mercenary to gift"
            ));
            meta.setLore(finalLore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            this.icon.setItemMeta(meta);
        }
    }

    public String getId() {
        return id;
    }

    public ItemStack getIcon() {
        return icon.clone();
    }

    public int getAffinityValue() {
        return affinityValue;
    }
}
