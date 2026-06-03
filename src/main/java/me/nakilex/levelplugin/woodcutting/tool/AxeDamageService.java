package me.nakilex.levelplugin.woodcutting.tool;

import me.nakilex.levelplugin.woodcutting.WoodcuttingConfig;
import org.bukkit.GameMode;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.concurrent.ThreadLocalRandom;

public class AxeDamageService {
    private final WoodcuttingConfig config;
    public AxeDamageService(WoodcuttingConfig config) { this.config = config; }

    public void damage(Player player, int logCount) {
        if (player.getGameMode() == GameMode.CREATIVE) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !(item.getItemMeta() instanceof Damageable damageable)) return;
        int base = config.axeDamageProportional() ? logCount : 1;
        int attempts = (int) Math.ceil(base * config.axeDamageMultiplier());
        int amount = calculateDamageWithUnbreaking(item, attempts);
        if (amount <= 0) return;
        int max = item.getType().getMaxDurability();
        int next = damageable.getDamage() + amount;
        if (config.leaveAtOneDurability()) next = Math.min(next, Math.max(0, max - 1));
        else next = Math.min(next, max);
        damageable.setDamage(next);
        item.setItemMeta(damageable);
        if (!config.leaveAtOneDurability() && next >= max) player.getInventory().setItemInMainHand(null);
    }

    private int calculateDamageWithUnbreaking(ItemStack item, int attempts) {
        int unbreaking = item.getEnchantmentLevel(Enchantment.UNBREAKING);
        int damage = 0;
        for (int i = 0; i < attempts; i++) {
            if (unbreaking <= 0 || ThreadLocalRandom.current().nextInt(unbreaking + 1) == 0) damage++;
        }
        return damage;
    }
}
