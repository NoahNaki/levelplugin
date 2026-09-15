package me.nakilex.levelplugin.environment;

import me.nakilex.levelplugin.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /lbmove <kingdom|mining|farming|fishing> - moves one of the caller's kingdom
 * animated leaderboards to their current position and facing.
 */
public class LeaderboardMoveCommand implements CommandExecutor, TabCompleter {
    private static final List<String> BOARD_IDS = List.of("kingdom", "mining", "farming", "fishing");
    private final Main plugin;

    public LeaderboardMoveCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " <" + String.join("|", BOARD_IDS) + ">");
            return true;
        }
        String result = EnvironmentAreaInstanceManager.getInstance(plugin).moveAnimatedLeaderboardHere(player, args[0]);
        player.sendMessage(ChatColor.AQUA + result);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return BOARD_IDS;
        }
        return List.of();
    }
}
