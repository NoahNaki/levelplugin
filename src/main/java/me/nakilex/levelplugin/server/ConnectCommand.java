package me.nakilex.levelplugin.server;

import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class ConnectCommand implements CommandExecutor, TabCompleter {
    private final ServerSelectionManager manager;

    public ConnectCommand(ServerSelectionManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                    "Only players can use this command.");
            return true;
        }
        if (args.length == 0) {
            manager.openSelector(player);
            return true;
        }
        String target = args[0].toLowerCase();
        switch (target) {
            case "alpha" -> manager.sendToAlpha(player);
            case "build" -> manager.sendToBuild(player);
            case "hub" -> manager.sendToHub(player, true);
            default -> ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Unknown server. Use /connect <alpha|build|hub>.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("alpha", "build", "hub");
        }
        return List.of();
    }
}
