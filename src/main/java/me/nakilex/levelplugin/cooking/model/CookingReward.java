package me.nakilex.levelplugin.cooking.model;

import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/** Config-backed cooking reward definition. */
public record CookingReward(Material material, String nexoItemId, int amount) {
    public CookingReward {
        if (material == null || material.isAir()) {
            material = Material.PAPER;
        }
        if (nexoItemId != null && nexoItemId.isBlank()) {
            nexoItemId = null;
        }
        amount = Math.max(1, amount);
    }

    public Optional<String> nexoItemIdOptional() {
        return Optional.ofNullable(nexoItemId);
    }

    public String discoveryKey() {
        return nexoItemId == null ? material.name().toLowerCase(java.util.Locale.ROOT) : nexoItemId.toLowerCase(java.util.Locale.ROOT);
    }

    public ItemStack toItemStack() {
        ItemStack stack = new ItemStack(material, amount);
        nexoItemIdOptional().ifPresent(id -> ItemUtil.applyNexoModel(stack, id));
        return stack;
    }
}
