package me.nakilex.levelplugin.player.battlepass.command;

import me.nakilex.levelplugin.player.battlepass.BattlePassManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Administrative command that grants raw battle pass XP to a player.
 */
public class BattlePassXpCommand implements CommandExecutor, TabCompleter {

    private final BattlePassManager manager;

    public BattlePassXpCommand(BattlePassManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            ChatMessageUtil.send(sender, MessageType.ERROR, "Usage: /" + label + " <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            ChatMessageUtil.send(sender, MessageType.ERROR, "Could not find player " + ChatColor.YELLOW + args[0] + ChatColor.RED + ".");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            ChatMessageUtil.send(sender, MessageType.ERROR, "Invalid amount: " + ChatColor.YELLOW + args[1]);
            return true;
        }

        if (amount <= 0) {
            ChatMessageUtil.send(sender, MessageType.ERROR, "Please specify a positive XP amount.");
            return true;
        }

        manager.addProgress(target, amount);
        ChatMessageUtil.send(sender, MessageType.SUCCESS,
                ChatColor.GRAY + "Granted " + ChatColor.GOLD + amount + ChatColor.GRAY + " Battle Pass XP to "
                        + ChatColor.YELLOW + target.getName());
        ChatMessageUtil.send(target, MessageType.REWARD,
                ChatColor.GRAY + "You received " + ChatColor.GOLD + amount + ChatColor.GRAY + " Battle Pass XP.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String search = args[0].toLowerCase(Locale.ROOT);
            List<String> matches = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                String name = player.getName();
                if (search.isEmpty() || name.toLowerCase(Locale.ROOT).startsWith(search)) {
                    matches.add(name);
                }
            }
            Collections.sort(matches, String.CASE_INSENSITIVE_ORDER);
            return matches;
        }
        return Collections.emptyList();
    }
}
