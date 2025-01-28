package me.nakilex.levelplugin.merchant.commands;

import me.nakilex.levelplugin.merchant.gui.MerchantGUI;
import me.nakilex.levelplugin.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MerchantCommand implements CommandExecutor {

    private final Main plugin;

    public MerchantCommand(Main plugin) {
        this.plugin = plugin;
        // Register this class as the executor for "/merchant".
        // Make sure you have "merchant" in your plugin.yml commands section.
        plugin.getCommand("merchant").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use /merchant!");
            return true;
        }

        Player player = (Player) sender;
        // Open the Merchant GUI for the player
        MerchantGUI.openMerchantGUI(player);

        return true;
    }
}
