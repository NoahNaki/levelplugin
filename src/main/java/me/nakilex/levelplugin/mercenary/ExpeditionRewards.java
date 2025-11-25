package me.nakilex.levelplugin.mercenary;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Reward bundle generated at expedition completion. */
public class ExpeditionRewards {
    private final List<ItemStack> loot = new ArrayList<>();
    private int coins;

    public ExpeditionRewards coins(int coins) {
        this.coins += coins;
        return this;
    }

    public ExpeditionRewards addLoot(ItemStack stack) {
        if (stack != null) {
            loot.add(stack);
        }
        return this;
    }

    public int coins() {
        return coins;
    }

    public List<ItemStack> loot() {
        return Collections.unmodifiableList(loot);
    }
}
