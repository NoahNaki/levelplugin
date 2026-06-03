package me.nakilex.levelplugin.woodcutting.tool;

import me.nakilex.levelplugin.woodcutting.WoodcuttingConfig;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AxeValidator {
    private final WoodcuttingConfig config;
    public AxeValidator(WoodcuttingConfig config) { this.config = config; }
    public boolean canUse(Player player) {
        if (!config.canChopIn(player.getGameMode())) return false;
        ItemStack item = player.getInventory().getItemInMainHand();
        return item != null && config.tools().contains(item.getType());
    }
}
