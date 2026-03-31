package me.nakilex.levelplugin.motd;

import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MotdCommand implements CommandExecutor {
    private final MotdManager manager;
    public MotdCommand(MotdManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/motd <reload|preview>");
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            manager.reload();
            sender.sendMessage(ChatColor.GREEN + "MOTD reloaded.");
            return true;
        }
        if (args[0].equalsIgnoreCase("preview")) {
            if (args.length >= 2) {
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                    return true;
                }
                target.sendMessage(manager.getLine1());
                target.sendMessage(manager.getLine2());
                sender.sendMessage(ChatColor.GREEN + "Sent MOTD preview to " + target.getName() + ".");
                return true;
            }
            sender.sendMessage(manager.getLine1());
            sender.sendMessage(manager.getLine2());
            return true;
        }
        sender.sendMessage(ChatColor.YELLOW + "/motd <reload|preview>");
        return true;
    }
}
