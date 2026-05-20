package me.nakilex.levelplugin.environment;

import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class KingdomCommand implements CommandExecutor, TabCompleter {
    private final EnvironmentAreaInstanceManager manager;
    public KingdomCommand(EnvironmentAreaInstanceManager manager) { this.manager = manager; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length == 0) {
            manager.initialize(player);
            return true;
        }
        if ("visit".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Usage: /kingdom visit <player>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Player not found.");
                return true;
            }
            manager.visit(player, target);
            return true;
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Usage: /kingdom [visit <player>]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("visit");
        return null;
    }
}
