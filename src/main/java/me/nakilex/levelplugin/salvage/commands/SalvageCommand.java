package me.nakilex.levelplugin.salvage.commands;

import me.nakilex.levelplugin.salvage.gui.SalvageGUI;
import me.nakilex.levelplugin.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SalvageCommand implements CommandExecutor {

    private final Main plugin;

    public SalvageCommand(Main plugin) {
        this.plugin = plugin;
        // Register this class as the executor for "/salvage".
        // Make sure you have "salvage" in your plugin.yml commands section.
        plugin.getCommand("salvage").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use /salvage!");
            return true;
        }

        Player player = (Player) sender;
        // Open the Merchant GUI for the player
        SalvageGUI.openMerchantGUI(player);

        return true;
    }
}
