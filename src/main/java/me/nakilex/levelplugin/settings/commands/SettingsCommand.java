package me.nakilex.levelplugin.settings.commands;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.settings.gui.SettingsGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SettingsCommand implements CommandExecutor {

    private final SettingsGUI settingsGUI;

    public SettingsCommand(SettingsGUI settingsGUI) {
        this.settingsGUI = settingsGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can open the settings GUI.");
            return true;
        }

        Player player = (Player) sender;
        settingsGUI.openSettingsMenu(player);
        return true;
    }
}
