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
        if (args.length != 1) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /class <Mage|Archer|Rogue|Warrior|Cleric>");
            return true;
        }

        try {
            PlayerClass chosen = PlayerClass.valueOf(args[0].toUpperCase());
            if (chosen != PlayerClass.MAGE && chosen != PlayerClass.ARCHER
                    && chosen != PlayerClass.ROGUE && chosen != PlayerClass.WARRIOR
                    && chosen != PlayerClass.CLERIC) {
                player.sendMessage(ChatColor.RED + "You cannot select that class with /class.");
                return true;
            }

            StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass = chosen;
            boolean flight = chosen == PlayerClass.ARCHER || chosen == PlayerClass.ROGUE;
            player.setAllowFlight(flight);
            if (!flight) player.setFlying(false);
            player.sendMessage(ChatColor.GREEN + "Class set to " + ChatColor.AQUA + chosen.name());
        } catch (IllegalArgumentException ex) {
            player.sendMessage(ChatColor.RED + "Unknown class: " + args[0]);
        }
        return true;
    }
}
