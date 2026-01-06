package me.nakilex.levelplugin.blacksmith.managers;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.ArmorType;
import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.items.events.ArmorEquipEvent;
import me.nakilex.levelplugin.items.events.WeaponEquipEvent;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.Bukkit;
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
        ItemUtil.applyUpdatedStack(itemStack, updated);

        if (!item.isBroken()) {
            refreshEquippedStats(player, item);
        }

        return true;
    }

    private void refreshEquippedStats(Player player, CustomItem item) {
        if (player == null || item == null) {
            return;
        }
        int id = item.getId();

        for (ItemStack armor : player.getInventory().getArmorContents()) {
            ArmorType type = ArmorType.matchType(armor);
            if (type == null) {
                continue;
            }
            CustomItem armorItem = ItemManager.getInstance().getCustomItemFromItemStack(armor);
            if (armorItem != null && armorItem.getId() == id) {
                ArmorEquipEvent equipEvent = new ArmorEquipEvent(
                        player,
                        ArmorEquipEvent.EquipMethod.HOTBAR,
                        type,
                        null,
                        armor
                );
                Bukkit.getPluginManager().callEvent(equipEvent);
                return;
            }
        }

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (matchesCustomItem(mainHand, id)) {
            fireWeaponRefresh(player, mainHand, WeaponEquipEvent.HandSlot.MAIN_HAND);
            return;
        }

        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (matchesCustomItem(offHand, id)) {
            fireWeaponRefresh(player, offHand, WeaponEquipEvent.HandSlot.OFF_HAND);
        }
    }

    private boolean matchesCustomItem(ItemStack stack, int id) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        CustomItem found = ItemManager.getInstance().getCustomItemFromItemStack(stack);
        return found != null && found.getId() == id;
    }

    private void fireWeaponRefresh(Player player, ItemStack stack, WeaponEquipEvent.HandSlot handSlot) {
        WeaponType weaponType = WeaponType.matchType(stack);
        if (weaponType == null) {
            return;
        }
        WeaponEquipEvent equipEvent = new WeaponEquipEvent(
                player,
                WeaponEquipEvent.EquipMethod.OTHER,
                weaponType,
                handSlot,
                null,
                stack
        );
        Bukkit.getPluginManager().callEvent(equipEvent);
    }
}
