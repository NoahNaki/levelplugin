package me.nakilex.levelplugin.cooking.service;

import me.nakilex.levelplugin.cooking.model.CookingWorkstationType;
import me.nakilex.levelplugin.cooking.registry.CookingWorkstationRegistry;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/** Matches placed blocks against the workstation item that created them. */
public class CookingWorkstationMatcher {
    private final CookingWorkstationRegistry workstationRegistry;
    private final List<PlacedItemMatcher> itemMatchers = List.of(
            new VanillaMaterialPlacedItemMatcher(),
            new NexoModelPlacedItemMatcher()
    );

    public CookingWorkstationMatcher(CookingWorkstationRegistry workstationRegistry) {
        this.workstationRegistry = workstationRegistry;
    }

    public Optional<CookingWorkstationType> findMatchingPlacedWorkstation(Material blockMaterial, ItemStack placedItem) {
        if (blockMaterial == null || placedItem == null || placedItem.getType().isAir()) {
            return Optional.empty();
        }
        return workstationRegistry.findAllByBlockMaterial(blockMaterial).stream()
                .filter(type -> matchesAny(type, placedItem))
                .findFirst();
    }

    private boolean matchesAny(CookingWorkstationType type, ItemStack placedItem) {
        for (PlacedItemMatcher matcher : itemMatchers) {
            if (matcher.matches(type, placedItem)) {
                return true;
            }
        }
        return false;
    }

    /** Extension point for future placed custom item matching strategies. */
    private interface PlacedItemMatcher {
        boolean matches(CookingWorkstationType type, ItemStack placedItem);
    }

    private static class VanillaMaterialPlacedItemMatcher implements PlacedItemMatcher {
        @Override
        public boolean matches(CookingWorkstationType type, ItemStack placedItem) {
            return type.itemMaterial() != null && placedItem.getType() == type.itemMaterial();
        }
    }

    private static class NexoModelPlacedItemMatcher implements PlacedItemMatcher {
        @Override
        public boolean matches(CookingWorkstationType type, ItemStack placedItem) {
            String expectedId = type.nexoItemId();
            return expectedId != null && !expectedId.isBlank()
                    && expectedId.equalsIgnoreCase(ItemUtil.getNexoModelId(placedItem));
        }
    }
}
