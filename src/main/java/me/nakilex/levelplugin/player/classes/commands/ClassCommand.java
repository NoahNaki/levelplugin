package me.nakilex.levelplugin.player.classes.commands;

import me.nakilex.levelplugin.player.classes.gui.ClassMenu;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClassCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // /class
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use /class");
            return true;
        }

        Player player = (Player) sender;

        // Debug: log to console that this command was used
        Bukkit.getLogger().info("[ClassCommand] " + player.getName() + " used /class. Opening class selection menu.");

        // Open your class selection menu
        player.openInventory(ClassMenu.getClassSelectionMenu());
        return true;
    }
}
