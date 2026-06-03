package me.nakilex.levelplugin.player.woodcutting;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.woodcutting.tool.AxeValidator;
import me.nakilex.levelplugin.player.woodcutting.tree.TreeDetectionResult;
import me.nakilex.levelplugin.player.woodcutting.tree.TreeDetector;
import me.nakilex.levelplugin.player.woodcutting.tree.TreeTypeRegistry;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class WoodcuttingListener implements Listener {
    private final Main plugin;
    private final WoodcuttingConfig config;
    private final TreeTypeRegistry treeTypeRegistry;
    private final AxeValidator axeValidator;
    private final TreeDetector treeDetector;
    private final WoodcuttingService woodcuttingService;

    public WoodcuttingListener(Main plugin, WoodcuttingConfig config, TreeTypeRegistry treeTypeRegistry, AxeValidator axeValidator,
                               TreeDetector treeDetector, WoodcuttingService woodcuttingService) {
        this.plugin = plugin;
        this.config = config;
        this.treeTypeRegistry = treeTypeRegistry;
        this.axeValidator = axeValidator;
        this.treeDetector = treeDetector;
        this.woodcuttingService = woodcuttingService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block clicked = event.getBlock();
        ItemStack tool = player.getInventory().getItemInMainHand();
        plugin.getLogger().info("[Woodcutting] BlockBreakEvent: player=" + player.getName()
                + " block=" + clicked.getType()
                + " tool=" + (tool == null ? "AIR" : tool.getType())
                + " mode=" + player.getGameMode());

        if (!config.enabled()) {
            plugin.getLogger().info("[Woodcutting] Return: disabled");
            return;
        }
        if (!config.canChopIn(player.getGameMode())) {
            plugin.getLogger().info("[Woodcutting] Return: creative blocked");
            return;
        }
        if (!treeTypeRegistry.isLog(clicked.getType())) {
            plugin.getLogger().info("[Woodcutting] Return: block is not configured log");
            return;
        }
        if (!axeValidator.canUseAxe(player, tool)) {
            plugin.getLogger().info("[Woodcutting] Return: invalid axe");
            return;
        }
        if (!poseAllowed(player)) {
            plugin.getLogger().info("[Woodcutting] Return: invalid pose");
            return;
        }

        TreeDetectionResult result = treeDetector.detect(clicked, player);
        if (!result.valid()) {
            plugin.getLogger().info("[Woodcutting] Detection failed for " + clicked.getType() + " at "
                    + clicked.getX() + "," + clicked.getY() + "," + clicked.getZ() + ": "
                    + result.invalidReason() + " logs=" + result.logs().size() + " leaves=" + result.leaves().size());
            plugin.getLogger().info("[Woodcutting] Return: detection invalid reason=" + result.invalidReason()
                    + " logs=" + result.logs().size() + " leaves=" + result.leaves().size());
            return;
        }
        plugin.getLogger().info("[Woodcutting] Detection valid logs=" + result.logs().size()
                + " leaves=" + result.leaves().size() + " type=" + result.type().key());
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
