package me.nakilex.levelplugin.dungeon.rift;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/** Command entry point for frontier rift interactions. */
public final class FrontierRiftCommand implements CommandExecutor, TabCompleter {

    private final FrontierRiftManager manager;

    public FrontierRiftCommand(FrontierRiftManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players may use frontier commands.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("start")) {
            manager.startNextStage(player);
            return true;
        }
        manager.openBoard(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("start");
        }
        return Collections.emptyList();
    }
}

