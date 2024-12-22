package me.nakilex.levelplugin.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.CustomItem;
import me.nakilex.levelplugin.items.ItemUtil;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Random;

public class ItemUpgradeManager {

    private final Random random = new Random();
    private final Plugin plugin;

    public ItemUpgradeManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public int getUpgradeCost(CustomItem item) {
        // Upgrade cost scales based on item upgrade level and rarity
        int baseCost = 100; // Base cost
        int rarityMultiplier = item.getRarity().ordinal() + 1; // Common=1, Rare=2, etc.
        return baseCost + (item.getUpgradeLevel() * 150 * rarityMultiplier);
    }

    public boolean attemptUpgrade(ItemStack itemStack, CustomItem customItem) {
        if (customItem.getUpgradeLevel() >= 5) {
            // Send a message to the player and log the issue
            Main.getInstance().getLogger().info("Upgrade limit reached for item: " + customItem.getBaseName());
            return false; // Upgrade limit reached
        }

        int successChance = calculateSuccessChance(customItem);
        if (random.nextInt(100) < successChance) {
            applyUpgrade(itemStack, customItem);
            return true; // Upgrade succeeded
        }
        return false; // Upgrade failed
    }


    private int calculateSuccessChance(CustomItem item) {
        // Success chance decreases drastically as upgrade level increases
        int baseChance = 50; // Start with a lower base success chance (e.g., 50%)
        int levelPenalty = item.getUpgradeLevel() * 20; // Apply a harsher penalty (20% per level)
        return Math.max(5, baseChance - levelPenalty); // Ensure a minimum success chance of 5%
    }

    private void applyUpgrade(ItemStack itemStack, CustomItem customItem) {
        int currentUpgradeLevel = ItemUtil.getUpgradeLevel(itemStack);
        if (currentUpgradeLevel >= 5) return;

        int newUpgradeLevel = currentUpgradeLevel + 1;
        customItem.setUpgradeLevel(newUpgradeLevel);

        // The single place we multiply stats:
        customItem.increaseStats();

        // Now update the PDC and lore/visuals
        ItemUtil.updateUpgradeLevel(itemStack, newUpgradeLevel);
        ItemStack updatedItem = ItemUtil.createItemStackFromCustomItem(customItem, itemStack.getAmount());

        // Overwrite the original item with updated name/lore
        itemStack.setType(updatedItem.getType());
        itemStack.setItemMeta(updatedItem.getItemMeta());
    }
}
