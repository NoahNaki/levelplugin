package me.nakilex.levelplugin.cooking.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.util.List;

/** Immutable config-backed cooking recipe definition. */
public record CookingRecipe(
        String id,
        String displayName,
        Material displayMaterial,
        List<String> lore,
        List<CookingStage> stages,
        List<CookingReward> rewards
) {
    public CookingRecipe {
        if (displayName == null || displayName.isBlank()) {
            displayName = id;
        }
        lore = List.copyOf(lore == null ? List.of() : lore);
        stages = List.copyOf(stages == null ? List.of() : stages);
        rewards = List.copyOf(rewards == null ? List.of() : rewards);
    }

    public ItemStack displayItem() {
        return new ItemStack(displayMaterial);
    }
}
