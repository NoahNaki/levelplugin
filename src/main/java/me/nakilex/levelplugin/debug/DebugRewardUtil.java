package me.nakilex.levelplugin.debug;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** Factory helpers for developer-facing debug reward items. */
public final class DebugRewardUtil {
    private DebugRewardUtil() {}

    public static ItemStack rollDebugReward() {
        int roll = ThreadLocalRandom.current().nextInt(7);
        return switch (roll) {
            case 0 -> new ItemStack(Material.SPLASH_POTION);
            case 1 -> new ItemStack(Material.POTION);
            case 2 -> new ItemStack(Material.EMERALD, ThreadLocalRandom.current().nextInt(3, 7));
            case 3 -> new ItemStack(Material.PRISMARINE_CRYSTALS, ThreadLocalRandom.current().nextInt(3, 8));
            case 4 -> new ItemStack(Material.GOLDEN_APPLE, 1 + ThreadLocalRandom.current().nextInt(2));
            case 5 -> new ItemStack(Material.DIAMOND_SWORD);
            default -> new ItemStack(Material.RABBIT_FOOT);
        };
    }
}
