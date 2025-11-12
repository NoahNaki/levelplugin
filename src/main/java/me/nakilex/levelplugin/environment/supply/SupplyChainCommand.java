package me.nakilex.levelplugin.environment.supply;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/** Command entry point for supply chain interactions. */
public final class SupplyChainCommand implements CommandExecutor, TabCompleter {

    private final SupplyChainManager manager;

    public SupplyChainCommand(SupplyChainManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can manage supply chains.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("deposit")) {
            manager.handleDeposit(player);
            return true;
        }
        manager.openBoard(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("deposit");
        }
        return Collections.emptyList();
    }
}

