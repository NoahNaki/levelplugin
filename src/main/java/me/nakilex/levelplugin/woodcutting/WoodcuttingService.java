package me.nakilex.levelplugin.woodcutting;

import me.nakilex.levelplugin.woodcutting.animation.BlockDisplayFactory;
import me.nakilex.levelplugin.woodcutting.animation.DisplayTree;
import me.nakilex.levelplugin.woodcutting.animation.FallingTreeAnimator;
import me.nakilex.levelplugin.woodcutting.drop.TreeDropService;
import me.nakilex.levelplugin.woodcutting.replant.ReplantService;
import me.nakilex.levelplugin.woodcutting.tool.AxeDamageService;
import me.nakilex.levelplugin.woodcutting.tree.TreeDetectionResult;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class WoodcuttingService {
    private final WoodcuttingConfig config;
    private final AxeDamageService axeDamageService;
    private final BlockDisplayFactory blockDisplayFactory;
    private final FallingTreeAnimator fallingTreeAnimator;
    private final TreeDropService treeDropService;
    private final ReplantService replantService;

    public WoodcuttingService(WoodcuttingConfig config, AxeDamageService axeDamageService, BlockDisplayFactory blockDisplayFactory,
                              FallingTreeAnimator fallingTreeAnimator, TreeDropService treeDropService, ReplantService replantService) {
        this.config = config;
        this.axeDamageService = axeDamageService;
        this.blockDisplayFactory = blockDisplayFactory;
        this.fallingTreeAnimator = fallingTreeAnimator;
        this.treeDropService = treeDropService;
        this.replantService = replantService;
    }

    public void startChop(Player player, TreeDetectionResult tree) {
        if (config.slowBreakEnabled()) player.sendMessage(ChatColor.YELLOW + "Slow tree chopping is not implemented yet; chopping instantly.");
        executeChop(player, tree);
    }

    private void executeChop(Player player, TreeDetectionResult tree) {
        axeDamageService.damage(player, tree.logs().size());
        if (config.animationEnabled()) {
            DisplayTree displayTree = blockDisplayFactory.convert(tree);
            fallingTreeAnimator.animate(player, displayTree, () -> {
                treeDropService.drop(player, tree, config.dropMode());
                replantService.replant(tree);
            });
        } else {
            removeOriginalBlocks(tree);
            treeDropService.drop(player, tree, config.dropMode());
            replantService.replant(tree);
        }
    }

    private void removeOriginalBlocks(TreeDetectionResult tree) {
        for (Block block : tree.allBlocks()) block.setType(Material.AIR, false);
    }
}
