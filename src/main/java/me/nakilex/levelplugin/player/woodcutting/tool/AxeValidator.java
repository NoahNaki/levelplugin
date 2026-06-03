package me.nakilex.levelplugin.player.woodcutting.tool;

import me.nakilex.levelplugin.items.tools.CustomTool;
import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import me.nakilex.levelplugin.items.tools.ToolManager;
import me.nakilex.levelplugin.player.woodcutting.WoodcuttingConfig;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AxeValidator {
    private final WoodcuttingConfig config;
    public AxeValidator(WoodcuttingConfig config) { this.config = config; }

    public boolean canUse(Player player) {
        if (!config.canChopIn(player.getGameMode())) return false;
        ItemStack item = player.getInventory().getItemInMainHand();
        return canUseAxe(player, item);
    }

    public boolean canUseAxe(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ToolManager toolManager = ToolManager.getInstance();
        CustomTool customTool = toolManager == null ? null : toolManager.getTool(item, false);
        if (customTool != null) {
            return customTool.getDiscipline() == ToolDiscipline.WOODCUTTING
                    && config.isConfiguredTool(item.getType())
                    && toolManager.meetsLevelRequirement(player, customTool);
        }
        return config.isConfiguredTool(item.getType());
    }
}
