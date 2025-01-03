package me.nakilex.levelplugin.blacksmith.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Random;
import java.util.UUID;

public class ItemUpgradeManager {

    private final Random random = new Random();
    private final Plugin plugin;

    public ItemUpgradeManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public int getUpgradeCost(CustomItem item) {
        int baseCost = 100; // Base cost
        int rarityMultiplier = item.getRarity().ordinal() + 1;
        return baseCost + (item.getUpgradeLevel() * 150 * rarityMultiplier);
    }

    public boolean attemptUpgrade(ItemStack itemStack, CustomItem customItem) {
        if (customItem.getUpgradeLevel() >= 5) {
            Main.getInstance().getLogger().info("Upgrade limit reached for item: " + customItem.getBaseName());
            return false;
        }

        int successChance = calculateSuccessChance(customItem);
        if (random.nextInt(100) < successChance) {
            applyUpgrade(itemStack, customItem);
            return true;
        }
        return false;
    }

    private int calculateSuccessChance(CustomItem item) {
        int baseChance = 50; // Base success chance
        int levelPenalty = item.getUpgradeLevel() * 20;
        return Math.max(5, baseChance - levelPenalty);
    }

    private void applyUpgrade(ItemStack itemStack, CustomItem customItem) {
        UUID uuid = ItemUtil.getItemUUID(itemStack);
        if (uuid == null) {
            Main.getInstance().getLogger().warning("Failed to find UUID for item during upgrade.");
            return;
        }

        int currentUpgradeLevel = customItem.getUpgradeLevel();
        if (currentUpgradeLevel >= 5) return;

        int newUpgradeLevel = currentUpgradeLevel + 1;
        customItem.setUpgradeLevel(newUpgradeLevel);

        // Update stats for this specific item instance
        customItem.increaseStats();

        // Update the PDC and visuals
        ItemUtil.updateUpgradeLevel(itemStack, newUpgradeLevel);
        ItemStack updatedItem = ItemUtil.createItemStackFromCustomItem(customItem, itemStack.getAmount());

        // Replace the original item
        itemStack.setType(updatedItem.getType());
        itemStack.setItemMeta(updatedItem.getItemMeta());
    }
}
