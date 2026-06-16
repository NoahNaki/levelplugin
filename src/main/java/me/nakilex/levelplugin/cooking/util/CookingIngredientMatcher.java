package me.nakilex.levelplugin.cooking.util;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import me.nakilex.levelplugin.cooking.model.CookingIngredientRequirement;
import me.nakilex.levelplugin.cooking.model.CookingRecipe;
import me.nakilex.levelplugin.cooking.model.CookingStage;
import me.nakilex.levelplugin.cooking.model.CookingStageType;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.fishing.utils.FishingItemUtil;
import me.nakilex.levelplugin.utils.TextUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Shared cooking ingredient helpers for insertion, GUI filtering, and requirement tooltips. */
public final class CookingIngredientMatcher {
    private CookingIngredientMatcher() {}

    public static boolean matches(CookingIngredientRequirement requirement, ItemStack stack) {
        if (requirement == null || stack == null || stack.getType().isAir()) {
            return false;
        }
        String expectedNexo = requirement.nexoItemId();
        if (expectedNexo != null && !expectedNexo.isBlank()) {
            return matchesNexoRequirement(expectedNexo, stack);
        }
        return requirement.material() != null && stack.getType() == requirement.material();
    }

    public static int countMatching(Inventory inventory, CookingIngredientRequirement requirement) {
        if (inventory == null || requirement == null) {
            return 0;
        }
        int count = 0;
        for (ItemStack stack : inventory.getStorageContents()) {
            if (matches(requirement, stack)) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    public static boolean hasIngredients(Inventory inventory, CookingRecipe recipe) {
        return aggregateRequirements(recipe).stream()
                .allMatch(requirement -> countMatching(inventory, requirement) >= requirement.amount());
    }

    public static List<CookingIngredientRequirement> aggregateRequirements(CookingRecipe recipe) {
        if (recipe == null) {
            return List.of();
        }
        Map<String, AggregatedRequirement> aggregated = new LinkedHashMap<>();
        for (CookingStage stage : recipe.stages()) {
            if (stage.type() != CookingStageType.INSERT_ITEM) {
                continue;
            }
            for (CookingIngredientRequirement requirement : stage.requirements()) {
                aggregated.compute(requirement.progressKey(), (ignored, current) -> {
                    if (current == null) {
                        return new AggregatedRequirement(requirement, requirement.amount());
                    }
                    return current.withAmount(current.amount + requirement.amount());
                });
            }
        }
        List<CookingIngredientRequirement> requirements = new ArrayList<>();
        for (AggregatedRequirement aggregatedRequirement : aggregated.values()) {
            CookingIngredientRequirement base = aggregatedRequirement.requirement;
            requirements.add(new CookingIngredientRequirement(base.material(), base.nexoItemId(), aggregatedRequirement.amount));
        }
        return List.copyOf(requirements);
    }

    public static String formatRequirement(CookingIngredientRequirement requirement) {
        if (requirement == null) {
            return "Unknown";
        }
        return requirement.amount() + "x " + formatRequirementName(requirement);
    }

    public static String formatRequirementName(CookingIngredientRequirement requirement) {
        if (requirement == null) {
            return "Unknown";
        }
        return requirement.nexoItemIdOptional()
                .map(TextUtil::beautifyWords)
                .orElseGet(() -> formatMaterial(requirement.material()));
    }

    private static boolean matchesNexoRequirement(String expectedNexo, ItemStack stack) {
        if (matchesFishRequirement(expectedNexo, stack)) {
            return true;
        }
        String modelId = ItemUtil.getNexoModelId(stack);
        if (expectedNexo.equalsIgnoreCase(modelId)) {
            return true;
        }
        if (matchesDisplayName(expectedNexo, stack)) {
            return true;
        }
        return matchesNexoVisualModel(expectedNexo, stack);
    }

    private static boolean matchesFishRequirement(String expectedNexo, ItemStack stack) {
        String fishId = FishingItemUtil.getFishId(stack);
        if (fishId == null || fishId.isBlank()) {
            return false;
        }
        String normalizedExpected = normalizeName(expectedNexo);
        String normalizedFishId = normalizeName(fishId);
        if (normalizedExpected.equals(normalizedFishId)) {
            return true;
        }
        return normalizedExpected.equals(normalizeName(fishId + "_fish"));
    }

    private static boolean matchesDisplayName(String expectedNexo, ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        String displayName = ChatColor.stripColor(meta.getDisplayName());
        return normalizeName(TextUtil.beautifyWords(expectedNexo)).equals(normalizeName(displayName));
    }

    private static boolean matchesNexoVisualModel(String expectedNexo, ItemStack stack) {
        ItemBuilder builder = NexoItems.itemFromId(expectedNexo);
        if (builder == null) {
            return false;
        }
        ItemStack expected = builder.build();
        if (expected == null || expected.getType() != stack.getType()) {
            return false;
        }
        ItemMeta expectedMeta = expected.getItemMeta();
        ItemMeta actualMeta = stack.getItemMeta();
        if (expectedMeta == null || actualMeta == null || !expectedMeta.hasCustomModelData() || !actualMeta.hasCustomModelData()) {
            return false;
        }
        return expectedMeta.getCustomModelData() == actualMeta.getCustomModelData();
    }

    private static String formatMaterial(Material material) {
        if (material == null) {
            return "Unknown";
        }
        return TextUtil.beautifyWords(material.name().toLowerCase(Locale.ROOT));
    }

    private static String normalizeName(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private record AggregatedRequirement(CookingIngredientRequirement requirement, int amount) {
        private AggregatedRequirement withAmount(int amount) {
            return new AggregatedRequirement(requirement, amount);
        }
    }
}
