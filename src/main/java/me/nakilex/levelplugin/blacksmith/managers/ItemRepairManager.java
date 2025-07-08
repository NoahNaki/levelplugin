package me.nakilex.levelplugin.blacksmith.managers;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemRepairManager {

    /**
     * Repair cost per durability point per rarity tier.
     * Index = rarity.ordinal()
     */
    private static final int[] COST_PER_DURABILITY = {
        1,   // COMMON
        2,   // UNCOMMON
        3,   // RARE
        5,   // EPIC
        8,   // LEGENDARY
        12,  // MYTHIC
        15   // FABLED
    };

    private static final int BASE_COST = 25; // Minimum cost to repair anything

    public int getRepairCost(CustomItem item) {
        int durabilityLost = item.getMaxDurability() - item.getCurrentDurability();
        if (durabilityLost <= 0) return 0;

        int rarityIndex = item.getRarity().ordinal();
        int costPerPoint = rarityIndex < COST_PER_DURABILITY.length
            ? COST_PER_DURABILITY[rarityIndex]
            : COST_PER_DURABILITY[COST_PER_DURABILITY.length - 1]; // fallback

        return BASE_COST + (durabilityLost * costPerPoint);
    }

    /**
     * Repairs the item to full durability and updates its ItemStack metadata.
     */
    public boolean repairItem(Player player, ItemStack itemStack, CustomItem item) {
        if (item.getCurrentDurability() >= item.getMaxDurability()) return false;

        // Repair logic
        item.setDurability(item.getMaxDurability()); // new setter we’ll add
        ItemUtil.updateDurability(itemStack, item.getCurrentDurability());

        // Rebuild display (if needed)
        ItemStack updated = ItemUtil.createItemStackFromCustomItem(item, itemStack.getAmount(), player);
        ItemUtil.copyEgoData(itemStack, updated, item, player);
        ItemUtil.applyUpdatedStack(itemStack, updated);

        return true;
    }
}
