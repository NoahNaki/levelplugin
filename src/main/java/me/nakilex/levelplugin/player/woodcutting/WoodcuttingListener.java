package me.nakilex.levelplugin.player.woodcutting;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.woodcutting.tool.AxeValidator;
import me.nakilex.levelplugin.player.woodcutting.tree.TreeDetectionResult;
import me.nakilex.levelplugin.player.woodcutting.tree.TreeDetector;
import me.nakilex.levelplugin.player.woodcutting.tree.TreeTypeRegistry;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        debugLog("[Woodcutting] BlockBreakEvent: player=" + player.getName()
                + " block=" + clicked.getType()
                + " tool=" + (tool == null ? "AIR" : tool.getType())
                + " mode=" + player.getGameMode());

        if (!config.enabled()) {
            debugLog("[Woodcutting] Return: disabled");
            return;
        }
        if (!config.canChopIn(player.getGameMode())) {
            debugLog("[Woodcutting] Return: creative blocked");
            return;
        }
        if (!treeTypeRegistry.isLog(clicked.getType())) {
            debugLog("[Woodcutting] Return: block is not configured log");
            return;
        }
        if (!axeValidator.canUseAxe(player, tool)) {
            debugLog("[Woodcutting] Return: invalid axe");
            return;
        }
        if (!poseAllowed(player)) {
            debugLog("[Woodcutting] Return: invalid pose");
            return;
        }

        TreeDetectionResult result = treeDetector.detect(clicked, player);
        if (!result.valid()) {
            debugLog("[Woodcutting] Detection failed for " + clicked.getType() + " at "
                    + clicked.getX() + "," + clicked.getY() + "," + clicked.getZ() + ": "
                    + result.invalidReason() + " logs=" + result.logs().size()
                    + " leaves=" + result.leaves().size() + " attached=" + result.attachedBlocks().size());
            debugLog("[Woodcutting] Return: detection invalid reason=" + result.invalidReason()
                    + " logs=" + result.logs().size() + " leaves=" + result.leaves().size()
                    + " attached=" + result.attachedBlocks().size());
            logMaterialSummary(result);
            return;
        }
        debugLog("[Woodcutting] Detection valid logs=" + result.logs().size()
                + " leaves=" + result.leaves().size() + " attached=" + result.attachedBlocks().size()
                + " type=" + result.type().key());
        logMaterialSummary(result);
        event.setCancelled(true);
        woodcuttingService.startChop(player, result);
    }

    private void debugLog(String message) {
        if (config.debug()) plugin.getLogger().info(message);
    }

    private void logMaterialSummary(TreeDetectionResult result) {
        if (!config.debug()) return;
        String type = result.type() == null ? "UNKNOWN" : result.type().key();
        plugin.getLogger().info("[Woodcutting] Detected tree type=" + type
                + " logs=" + result.logs().size()
                + " leaves=" + result.leaves().size()
                + " attached=" + result.attachedBlocks().size());
        plugin.getLogger().info("[Woodcutting] Leaf materials: " + materialSummary(result.leaves()));
        plugin.getLogger().info("[Woodcutting] Attached materials: " + materialSummary(result.attachedBlocks()));
    }

    private String materialSummary(Set<Block> blocks) {
        if (blocks.isEmpty()) return "none";
        Map<Material, Long> counts = blocks.stream()
                .collect(Collectors.groupingBy(Block::getType, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey().name() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    private boolean poseAllowed(Player player) {
        return switch (config.poseMode()) {
            case BOTH -> true;
            case CROUCH -> player.isSneaking();
            case STAND -> !player.isSneaking();
        };
    }
}
