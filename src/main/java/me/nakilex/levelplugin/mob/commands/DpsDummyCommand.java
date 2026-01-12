package me.nakilex.levelplugin.mob.commands;

import me.nakilex.levelplugin.mob.dps.DpsDummyManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DpsDummyCommand implements TabExecutor {
    private final DpsDummyManager manager;

    public DpsDummyCommand(DpsDummyManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, "Usage: /dpsdummy <spawn|select|despawn|list> [id]");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "spawn":
                manager.spawn(player);
                break;
            case "select":
                manager.select(player);
                break;
            case "despawn":
                manager.despawn(player, args.length > 1 ? args[1] : null);
                break;
            case "list":
                manager.list(player);
                break;
            default:
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "Unknown subcommand.");
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, "Usage: /dpsdummy <spawn|select|despawn|list> [id]");
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("spawn", "select", "despawn", "list").stream()
                    .filter(opt -> opt.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && "despawn".equalsIgnoreCase(args[0])) {
            return manager.getDummyIds().stream()
                    .filter(id -> id.startsWith(args[1]))
                    .toList();
        }
        return Collections.emptyList();
    }
}
