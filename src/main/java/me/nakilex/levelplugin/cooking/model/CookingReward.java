package me.nakilex.levelplugin.cooking.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** Config-backed cooking reward definition. */
public record CookingReward(Material material, int amount) {
    public CookingReward {
        amount = Math.max(1, amount);
    }

    public ItemStack toItemStack() {
        return new ItemStack(material, amount);
    }
}
