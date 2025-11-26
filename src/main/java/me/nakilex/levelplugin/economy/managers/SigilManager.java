package me.nakilex.levelplugin.economy.managers;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Currency manager for tower rewards. Uses the same tiered stacking system as gems
 * but with distinct theming to avoid overlapping existing currencies.
 */
public class SigilManager {

    private static final Material DUST = Material.GLOWSTONE_DUST;
    private static final Material SHARD = Material.PRISMARINE_SHARD;
    private static final Material CORE = Material.HEART_OF_THE_SEA;

    private static final int PER_SHARD = 64;
    private static final int PER_CORE = PER_SHARD * PER_SHARD; // 4096

    private final TieredCurrencyManager delegate = TieredCurrencyManager
            .builder("Soul Sigil", "Soul Sigils", ChatColor.AQUA)
            .tier("Sigil Dust", DUST, 1)
            .tier("Sigil Shard", SHARD, PER_SHARD)
            .tier("Sigil Core", CORE, PER_CORE)
            .build();

    public int getTotalUnits(Player player) {
        return delegate.getTotalUnits(player);
    }

    public void addUnits(Player player, int units) {
        delegate.addUnits(player, units);
    }

    public void setTotalUnits(Player player, int units) {
        delegate.setTotalUnits(player, units);
    }

    public void deductUnits(Player player, int units) {
        delegate.deductUnits(player, units);
    }

    public int[] breakdown(Player player) {
        return delegate.breakdown(player);
    }

    public ItemStack createCurrencyItem(Material mat, int qty, int unitValue) {
        return delegate.createCurrencyItem(mat, qty, unitValue);
    }

    public TieredCurrencyManager getDelegate() {
        return delegate;
    }
}

