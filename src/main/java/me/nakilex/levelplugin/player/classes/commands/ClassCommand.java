package me.nakilex.levelplugin.player.classes.commands;

import me.nakilex.levelplugin.player.classes.gui.ClassMenu;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClassCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use /class");
            return true;
        }

        Player player = (Player) sender;
        if (args.length >= 1) {
            try {
                PlayerClass chosen = PlayerClass.valueOf(args[0].toUpperCase());
                StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass = chosen;
                boolean flight = chosen == PlayerClass.ARCHER
                        || chosen == PlayerClass.ROGUE
                        || chosen == PlayerClass.COOLARCHER;
                player.setAllowFlight(flight);
                if (!flight) player.setFlying(false);
                player.sendMessage(ChatColor.GREEN + "Class set to " + ChatColor.AQUA + chosen.name());
            } catch (IllegalArgumentException ex) {
                player.sendMessage(ChatColor.RED + "Unknown class: " + args[0]);
            }
        } else {
            // Debug: log to console that this command was used
            Bukkit.getLogger().info("[ClassCommand] " + player.getName() + " used /class. Opening class selection menu.");
            player.openInventory(ClassMenu.getClassSelectionMenu(player));
        }
        return true;
    }
}
