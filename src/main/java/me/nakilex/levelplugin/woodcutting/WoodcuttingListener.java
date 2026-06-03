package me.nakilex.levelplugin.woodcutting;

import me.nakilex.levelplugin.woodcutting.tool.AxeValidator;
import me.nakilex.levelplugin.woodcutting.tree.TreeDetectionResult;
import me.nakilex.levelplugin.woodcutting.tree.TreeDetector;
import me.nakilex.levelplugin.woodcutting.tree.TreeTypeRegistry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class WoodcuttingListener implements Listener {
    private final WoodcuttingConfig config;
    private final TreeTypeRegistry treeTypeRegistry;
    private final AxeValidator axeValidator;
    private final TreeDetector treeDetector;
    private final WoodcuttingService woodcuttingService;

    public WoodcuttingListener(WoodcuttingConfig config, TreeTypeRegistry treeTypeRegistry, AxeValidator axeValidator,
                               TreeDetector treeDetector, WoodcuttingService woodcuttingService) {
        this.config = config;
        this.treeTypeRegistry = treeTypeRegistry;
        this.axeValidator = axeValidator;
        this.treeDetector = treeDetector;
        this.woodcuttingService = woodcuttingService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!config.enabled()) return;
        if (!treeTypeRegistry.isLog(event.getBlock().getType())) return;
        if (!axeValidator.canUse(player)) return;
        if (!poseAllowed(player)) return;
        TreeDetectionResult result = treeDetector.detect(event.getBlock(), player);
        if (!result.valid()) return;
        event.setCancelled(true);
        woodcuttingService.startChop(player, result);
    }

    private boolean poseAllowed(Player player) {
        return switch (config.poseMode()) {
            case BOTH -> true;
            case CROUCH -> player.isSneaking();
            case STAND -> !player.isSneaking();
        };
    }
}
