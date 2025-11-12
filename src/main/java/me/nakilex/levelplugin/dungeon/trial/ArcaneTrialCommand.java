package me.nakilex.levelplugin.dungeon.trial;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/** Command entry point for arcane trials. */
public final class ArcaneTrialCommand implements CommandExecutor, TabCompleter {

    private final ArcaneTrialManager manager;

    public ArcaneTrialCommand(ArcaneTrialManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can access arcane trials.");
            return true;
        }
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("start")) {
                int tier = manager.getState(player).getHighestTier() + 1;
                if (args.length > 1) {
                    try {
                        tier = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Invalid tier number.");
                        return true;
                    }
                }
                manager.startTrial(player, tier);
                return true;
            }
            if (args[0].equalsIgnoreCase("prestige")) {
                manager.prestige(player);
                return true;
            }
        }
        manager.openBoard(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("start", "prestige");
        }
        return Collections.emptyList();
    }
}

