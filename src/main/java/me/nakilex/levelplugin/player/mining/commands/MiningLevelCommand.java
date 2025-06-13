package me.nakilex.levelplugin.player.mining.commands;

import me.nakilex.levelplugin.player.mining.managers.MiningManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MiningLevelCommand implements CommandExecutor {

    private final MiningManager miningManager;

    public MiningLevelCommand(MiningManager manager) {
        this.miningManager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        int level = miningManager.getLevel(player);
        int xp = miningManager.getXP(player);
        int needed = level >= miningManager.getMaxLevel() ? 0 : miningManager.getXpRequired(level);

        player.sendMessage("§6Mining Level: §e" + level);
        if (level < miningManager.getMaxLevel()) {
            player.sendMessage("§7XP: §e" + xp + "§7/§e" + needed);
        } else {
            player.sendMessage("§7XP: §eMAX");
        }
        return true;
    }
}
