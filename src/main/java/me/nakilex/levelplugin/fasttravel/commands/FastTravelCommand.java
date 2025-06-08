package me.nakilex.levelplugin.fasttravel.commands;

import me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FastTravelCommand implements CommandExecutor {
    private final FastTravelGUI gui;
    public FastTravelCommand(FastTravelGUI gui) {
        this.gui = gui;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players may use this command.");
            return true;
        }
        gui.open(player);
        return true;
    }
}
