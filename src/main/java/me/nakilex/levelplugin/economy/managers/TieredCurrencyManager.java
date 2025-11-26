package me.nakilex.levelplugin.economy.managers;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic manager for multi-tiered item currencies (e.g. fragments → shards → clusters).
 * Existing currency managers like {@link GemsManager} can delegate to this to avoid
 * duplicating stacking logic when new currencies are added.
 */
public class TieredCurrencyManager {

    private final String singularName;
    private final String pluralName;
    private final ChatColor displayColor;
    private final List<CurrencyTier> tiers;
    private final Map<Material, CurrencyTier> tierByMaterial;

    private TieredCurrencyManager(String singularName, String pluralName, ChatColor displayColor, List<CurrencyTier> tiers) {
        this.singularName = singularName;
        this.pluralName = pluralName;
        this.displayColor = displayColor;
        this.tiers = tiers;
        this.tierByMaterial = new HashMap<>();
        for (CurrencyTier tier : tiers) {
            tierByMaterial.put(tier.material(), tier);
        }
    }

    public static Builder builder(String singularName, String pluralName, ChatColor color) {
        return new Builder(singularName, pluralName, color);
    }

    public int getTotalUnits(Player player) {
        PlayerInventory inv = player.getInventory();
        int total = 0;
        for (CurrencyTier tier : tiers) {
            total += inv.all(tier.material()).values().stream().mapToInt(ItemStack::getAmount).sum() * tier.unitValue();
        }
        return total;
    }

    public void setTotalUnits(Player player, int units) {
        PlayerInventory inv = player.getInventory();
        for (CurrencyTier tier : tiers) {
            inv.remove(tier.material());
        }

        if (units <= 0) return;

        int remaining = units;
        for (int i = tiers.size() - 1; i >= 0; i--) {
            CurrencyTier tier = tiers.get(i);
            int qty = remaining / tier.unitValue();
            remaining = remaining % tier.unitValue();
            if (qty > 0) {
                inv.addItem(createCurrencyItem(tier.material(), qty, tier.unitValue()));
            }
        }
    }

    public void addUnits(Player player, int units) {
        setTotalUnits(player, getTotalUnits(player) + units);
    }

    public void deductUnits(Player player, int units) {
        int current = getTotalUnits(player);
        if (current < units) {
            throw new IllegalArgumentException("Not enough " + pluralName.toLowerCase() + "!");
        }
        setTotalUnits(player, current - units);
    }

    public int[] breakdown(Player player) {
        int total = getTotalUnits(player);
        int[] breakdown = new int[tiers.size()];
        int remaining = total;
        for (int i = tiers.size() - 1; i >= 0; i--) {
            CurrencyTier tier = tiers.get(i);
            int qty = remaining / tier.unitValue();
            remaining = remaining % tier.unitValue();
            breakdown[i] = qty;
        }
        return breakdown;
    }

    public ItemStack createCurrencyItem(Material mat, int qty, int unitValue) {
        CurrencyTier tier = tierByMaterial.get(mat);
        if (tier == null || tier.unitValue() != unitValue) {
            return null;
        }
        ItemStack stack = new ItemStack(mat, qty);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayColor + tier.displayName());
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.GRAY + "Currency");
            lore.add("");

            String formatted = String.format("%,d", qty * unitValue);
            String dashFmt = displayColor.toString() + org.bukkit.ChatColor.BOLD + org.bukkit.ChatColor.STRIKETHROUGH;
            String reset = org.bukkit.ChatColor.RESET.toString();
            String midFmt = displayColor.toString() + org.bukkit.ChatColor.BOLD;
            lore.add(dashFmt + "---" + reset + "{ " + midFmt + formatted + reset + " }" + dashFmt + "---");

            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public String getDisplayName(boolean plural) {
        return plural ? pluralName : singularName;
    }

    public ChatColor getDisplayColor() {
        return displayColor;
    }

    public int getUnitValue(Material mat) {
        CurrencyTier tier = tierByMaterial.get(mat);
        return tier == null ? 0 : tier.unitValue();
    }

    public List<CurrencyTier> getTiers() {
        return tiers;
    }

    public record CurrencyTier(String displayName, Material material, int unitValue) {
    }

    public static final class Builder {
        private final String singular;
        private final String plural;
        private final ChatColor color;
        private final List<CurrencyTier> tiers = new ArrayList<>();

        private Builder(String singular, String plural, ChatColor color) {
            this.singular = singular;
            this.plural = plural;
            this.color = color;
        }

        public Builder tier(String displayName, Material material, int unitValue) {
            tiers.add(new CurrencyTier(displayName, material, unitValue));
            return this;
        }

        public TieredCurrencyManager build() {
            tiers.sort(Comparator.comparingInt(CurrencyTier::unitValue));
            return new TieredCurrencyManager(singular, plural, color, tiers);
        }
    }
}

