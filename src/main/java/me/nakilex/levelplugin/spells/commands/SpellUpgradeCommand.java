package me.nakilex.levelplugin.spells.commands;

import me.nakilex.levelplugin.spells.gui.SpellUpgradeGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpellUpgradeCommand implements CommandExecutor {
    private final SpellUpgradeGUI gui;

    public SpellUpgradeCommand(SpellUpgradeGUI gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        gui.open(player);
        return true;
    }
}
