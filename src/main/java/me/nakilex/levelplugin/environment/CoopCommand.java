package me.nakilex.levelplugin.environment;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class CoopCommand implements CommandExecutor, TabCompleter {
    private final EnvironmentManager manager;
    private final EnvironmentAreaInstanceManager areaManager;

    public CoopCommand(EnvironmentManager manager) {
        this.manager = manager;
        this.areaManager = EnvironmentAreaInstanceManager.getInstance(Main.getInstance());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length == 0) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Usage: /coop <invite|accept|deny|kick|transfer|leave|list> [player]");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "invite", "kick", "transfer" -> {
                if (args.length < 2) {
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Missing player name.");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Player not found.");
                    return true;
                }
                if ("invite".equals(sub)) {
                    if (areaManager.hasSession(player.getUniqueId())) {
                        areaManager.invite(player, target);
                    } else {
                        manager.invite(player, target);
                    }
                }
                if ("kick".equals(sub)) manager.kick(player, target);
                if ("transfer".equals(sub)) manager.transfer(player, target);
            }
            case "accept" -> {
                if (areaManager.hasPendingInvite(player.getUniqueId())) {
                    areaManager.accept(player);
                } else {
                    manager.accept(player);
                }
            }
            case "deny" -> manager.deny(player);
            case "leave" -> manager.leave(player);
            case "list" -> manager.sendInfo(player);
            default -> ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Usage: /coop <invite|accept|deny|kick|transfer|leave|list> [player]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("invite", "accept", "deny", "kick", "transfer", "leave", "list");
        }
        if (args.length == 2 && ("invite".equalsIgnoreCase(args[0]) || "kick".equalsIgnoreCase(args[0]) || "transfer".equalsIgnoreCase(args[0]))) {
            return null;
        }
        return List.of();
    }
}
