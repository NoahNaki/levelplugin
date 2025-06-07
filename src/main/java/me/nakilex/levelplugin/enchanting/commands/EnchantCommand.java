package me.nakilex.levelplugin.enchanting.commands;

import me.nakilex.levelplugin.enchanting.gui.EnchantingGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EnchantCommand implements CommandExecutor {
    private final EnchantingGUI gui;

    public EnchantCommand(EnchantingGUI gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only!");
            return true;
        }
        gui.open((Player) sender);
        return true;
    }
}
