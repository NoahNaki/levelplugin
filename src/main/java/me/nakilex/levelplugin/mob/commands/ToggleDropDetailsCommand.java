package me.nakilex.levelplugin.mob.commands;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mob.listeners.MythicMobDeathListener;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ToggleDropDetailsCommand implements CommandExecutor {

    private final Main plugin;

    public ToggleDropDetailsCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Only players can toggle
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this.");
            return true;
        }
        Player p = (Player) sender;

        // Expect exactly "/toggle dropdetails"
        if (args.length != 1 || !args[0].equalsIgnoreCase("dropdetails")) {
            p.sendMessage(ChatColor.RED + "Usage: /toggle dropdetails");
            return true;
        }

        // Toggle the flag
        MythicMobDeathListener.toggleDropDetails(p);

        // Report status
        if (MythicMobDeathListener.isDropDetailsEnabled(p)) {
            p.sendMessage(
                ChatColor.GRAY  + "Drop-details: "
                    + ChatColor.GREEN + "" + ChatColor.BOLD + "ON"
            );
        } else {
            p.sendMessage(
                ChatColor.GRAY  + "Drop-details: "
                    + ChatColor.RED   + "" + ChatColor.BOLD + "OFF"
            );
        }
        return true;
    }
}
