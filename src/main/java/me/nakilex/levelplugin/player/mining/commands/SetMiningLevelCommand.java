package me.nakilex.levelplugin.player.mining.commands;

import me.nakilex.levelplugin.player.mining.managers.MiningManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.UUID;

/**
 * Command to directly set a player's mining level.
 */
public class SetMiningLevelCommand implements CommandExecutor {

    private final MiningManager miningManager;

    public SetMiningLevelCommand(MiningManager miningManager) {
        this.miningManager = miningManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // /setmininglevel <player> <level>
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /setmininglevel <player> <level>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[0]);
            return true;
        }

        int newLevel;
        try {
            newLevel = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid level: " + args[1]);
            return true;
        }

        if (newLevel < 1) {
            sender.sendMessage("§cLevel must be >= 1");
            return true;
        }
        if (newLevel > miningManager.getMaxLevel()) {
            newLevel = miningManager.getMaxLevel();
        }

        UUID uuid = target.getUniqueId();
        miningManager.setLevel(uuid, newLevel);

        sender.sendMessage("§aSet " + target.getName() + "'s mining level to " + newLevel);
        target.sendMessage("§aYour mining level has been set to " + newLevel + " by an admin.");
        return true;
    }
}
