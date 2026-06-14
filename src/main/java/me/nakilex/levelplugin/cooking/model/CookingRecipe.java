package me.nakilex.levelplugin.cooking.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Immutable config-backed cooking recipe definition. */
public record CookingRecipe(
        String id,
        Material displayMaterial,
        List<CookingStage> stages,
        List<ItemStack> rewards
) {
    public CookingRecipe {
        stages = List.copyOf(stages == null ? List.of() : stages);
        rewards = List.copyOf(rewards == null ? List.of() : rewards);
    }
}
