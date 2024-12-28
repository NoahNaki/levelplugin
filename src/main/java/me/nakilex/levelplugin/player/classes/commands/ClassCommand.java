package me.nakilex.levelplugin.player.classes.commands;

import me.nakilex.levelplugin.player.classes.gui.ClassMenu;
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
        player.openInventory(ClassMenu.getClassSelectionMenu());
        return true;
    }
}
