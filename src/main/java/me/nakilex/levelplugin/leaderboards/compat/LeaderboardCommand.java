package me.nakilex.levelplugin.leaderboards.compat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** Administration command and familiar ajLeaderboards command aliases. */
public final class LeaderboardCommand implements CommandExecutor, TabCompleter {
    private final LeaderboardSystem system;

    public LeaderboardCommand(LeaderboardSystem system) {
        this.system = system;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("levelplugin.leaderboards.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to manage leaderboards.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sender.sendMessage(ChatColor.AQUA + "LevelPlugin leaderboards: " + ChatColor.WHITE
                    + system.boardCount() + " boards, " + system.scoreCount() + " cached scores");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                system.reload();
                sender.sendMessage(ChatColor.GREEN + "Leaderboard settings reloaded.");
            }
            case "update", "refresh" -> {
                if (args.length >= 2 && !args[1].equalsIgnoreCase("all")) {
                    Player target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "That player is not online.");
                    } else {
                        system.updatePlayer(target);
                        sender.sendMessage(ChatColor.GREEN + "Updated " + target.getName() + "'s scores.");
                    }
                } else {
                    int count = system.updateAllOnline();
                    sender.sendMessage(ChatColor.GREEN + "Updated " + count + " online player(s).");
                }
            }
            case "save" -> {
                system.flush();
                sender.sendMessage(ChatColor.GREEN + "Leaderboard data saved.");
            }
            default -> sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " <status|reload|update [player|all]|save>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(args[0], List.of("status", "reload", "update", "save"));
        if (args.length == 2 && (args[0].equalsIgnoreCase("update") || args[0].equalsIgnoreCase("refresh"))) {
            return filter(args[1], java.util.stream.Stream.concat(
                    java.util.stream.Stream.of("all"),
                    Bukkit.getOnlinePlayers().stream().map(Player::getName)).toList());
        }
        return List.of();
    }

    private static List<String> filter(String input, List<String> values) {
        String lower = input.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
