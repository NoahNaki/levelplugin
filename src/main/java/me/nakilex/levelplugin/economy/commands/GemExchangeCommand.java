package me.nakilex.levelplugin.economy.commands;

import me.nakilex.levelplugin.economy.gui.GemExchangeGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GemExchangeCommand implements CommandExecutor {
    private final GemExchangeGUI gui;

    public GemExchangeCommand(GemExchangeGUI gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only!");
            return true;
        }
        gui.open((Player)sender);
        return true;
    }
}
