package me.nakilex.levelplugin.player.classes.commands;

import me.nakilex.levelplugin.player.classes.essence.gui.ClassEssenceResealGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Command to open the essence reseal menu. */
public class EssenceResealCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players may use this command.");
            return true;
        }
        ClassEssenceResealGUI.open(player);
        return true;
    }
}
