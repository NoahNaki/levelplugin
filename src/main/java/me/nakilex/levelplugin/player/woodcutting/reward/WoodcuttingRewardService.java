package me.nakilex.levelplugin.player.woodcutting.reward;

import me.nakilex.levelplugin.player.woodcutting.WoodcuttingConfig;
import me.nakilex.levelplugin.player.woodcutting.managers.WoodcuttingManager;
import me.nakilex.levelplugin.player.woodcutting.tree.TreeDetectionResult;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class WoodcuttingRewardService {
    private final WoodcuttingConfig config;

    public WoodcuttingRewardService(WoodcuttingConfig config) {
        this.config = config;
    }

    public boolean meetsLevelRequirement(Player player, TreeDetectionResult tree) {
        WoodcuttingManager manager = WoodcuttingManager.getInstance();
        if (manager == null || tree == null || !tree.valid()) return true;
        int required = config.levelRequired(tree.type());
        int current = manager.getLevel(player);
        if (current < required) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "You need Woodcutting level " + required + " to chop " + tree.type().key() + " trees.");
            return false;
        }
        return true;
    }

    public void reward(Player player, TreeDetectionResult tree) {
        WoodcuttingManager manager = WoodcuttingManager.getInstance();
        if (manager == null || tree == null || !tree.valid()) return;
        int xp = (tree.logs().size() * config.xpPerLog(tree.type()))
                + (tree.leaves().size() * config.defaultXpPerLeaf());
        xp = (int) Math.round(xp * config.xpMultiplier());
        xp = Math.max(config.minimumXp(), xp);
        xp = Math.min(config.maximumXpPerTree(), xp);
        if (xp <= 0) return;
        manager.addXP(player, xp);
        if (config.debug()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.REWARD,
                    "Woodcutting +" + ChatColor.YELLOW + xp + ChatColor.GOLD + " XP");
        }
    }
}
