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
        return baseCost + (item.getUpgradeLevel() * 50 * rarityMultiplier);
    }

    public boolean attemptUpgrade(ItemStack itemStack, CustomItem customItem) {
        if (customItem.getUpgradeLevel() >= 5) {
            plugin.getLogger().info("Upgrade limit reached for item: " + customItem.getBaseName());
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
        // Success chance decreases as upgrade level increases
        int baseChance = 75; // Base success chance
        int levelPenalty = item.getUpgradeLevel() * 10; // 10% penalty per upgrade level
        return Math.max(10, baseChance - levelPenalty);
    }

    private void applyUpgrade(ItemStack itemStack, CustomItem customItem) {
        // Increment upgrade level
        customItem.setUpgradeLevel(customItem.getUpgradeLevel() + 1);

        // Update stats based on the new level
        customItem.increaseStats();

        // Regenerate the item with updated properties
        ItemStack upgradedItem = ItemUtil.createItemStackFromCustomItem(customItem, itemStack.getAmount());

        // Update the item in the inventory
        itemStack.setType(upgradedItem.getType());
        itemStack.setItemMeta(upgradedItem.getItemMeta());
    }

}
