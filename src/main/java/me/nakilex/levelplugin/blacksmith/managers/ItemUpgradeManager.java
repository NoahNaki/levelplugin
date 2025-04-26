package me.nakilex.levelplugin.blacksmith.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Random;

public class ItemUpgradeManager {

    private final Random random = new Random();
    private final Plugin plugin;

    public ItemUpgradeManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Calculate the coins cost to upgrade an item.
     */
    public int getUpgradeCost(CustomItem item) {
        int baseCost = 200;
        int rarityMultiplier = item.getRarity().ordinal() + 1;
        return baseCost + (item.getUpgradeLevel() * 250 * rarityMultiplier);
    }

    /**
     * Attempts to upgrade the given item. If successful, applies the upgrade
     * (bumping its upgrade level and scaling its stats), updates the PDC and
     * the ItemStack visuals, and returns true. Otherwise returns false.
     */
    public boolean attemptUpgrade(Player player, ItemStack itemStack, CustomItem customItem) {
        if (customItem.getUpgradeLevel() >= 5) {
            Main.getInstance().getLogger()
                .info("Upgrade limit reached for item: " + customItem.getBaseName());
            return false;
        }

        int successChance = calculateSuccessChance(customItem);
        if (random.nextInt(100) < successChance) {
            // Use the new applyUpgrade() that handles both level bump and stat scaling
            customItem.applyUpgrade();

            // Persist the new upgrade level in the item's PDC
            ItemUtil.updateUpgradeLevel(itemStack, customItem.getUpgradeLevel());

            // Rebuild the ItemStack so lore, name, and stats reflect the upgrade
            ItemStack updated = ItemUtil.createItemStackFromCustomItem(
                customItem,
                itemStack.getAmount(),
                player
            );
            itemStack.setType(updated.getType());
            itemStack.setItemMeta(updated.getItemMeta());

            return true;
        }

        return false;
    }

    /**
     * Compute success chance: starts at 50%, minus 20% per current upgrade level,
     * floored at 5%.
     */
    private int calculateSuccessChance(CustomItem item) {
        int baseChance = 50;
        int penalty    = item.getUpgradeLevel() * 20;
        return Math.max(5, baseChance - penalty);
    }
}
