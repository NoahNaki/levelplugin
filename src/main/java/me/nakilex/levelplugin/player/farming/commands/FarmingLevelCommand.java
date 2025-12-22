package me.nakilex.levelplugin.player.farming.commands;

import me.nakilex.levelplugin.player.farming.managers.FarmingManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FarmingLevelCommand implements CommandExecutor {

    private final FarmingManager farmingManager;

    public FarmingLevelCommand(FarmingManager manager) {
        this.farmingManager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        int level = farmingManager.getLevel(player);
        int xp = farmingManager.getXP(player);
        int needed = level >= farmingManager.getMaxLevel() ? 0 : farmingManager.getXpRequired(level);

        player.sendMessage("§eFarming Level: §f" + level);
        player.sendMessage("§eXP: §f" + xp + "§7/§f" + (needed == 0 ? "Max" : needed));
        return true;
    }
}
