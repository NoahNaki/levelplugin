package me.nakilex.levelplugin.commands;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.UUID;

public class SetLevelCommand implements CommandExecutor {

    private final Main plugin;

    public SetLevelCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // /setlevel <player> <level>
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /setlevel <player> <level>");
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
        if (newLevel > plugin.getLevelManager().getMaxLevel()) {
            newLevel = plugin.getLevelManager().getMaxLevel();
        }

        // Force the player's level in LevelManager
        UUID uuid = target.getUniqueId();
        plugin.getLevelManager().setLevel(uuid, newLevel);

        sender.sendMessage("§aSet " + target.getName() + "'s level to " + newLevel);
        target.sendMessage("§aYour level has been set to " + newLevel + " by an admin.");
        return true;
    }
}
