package me.nakilex.levelplugin.stronghold.command;

import me.nakilex.levelplugin.stronghold.StrongholdQueueManager;
import me.nakilex.levelplugin.stronghold.gui.StrongholdQueueGUI;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StrongholdCommand implements CommandExecutor, TabCompleter {
    private final StrongholdQueueManager queueManager;
    private final StrongholdQueueGUI gui;

    public StrongholdCommand(StrongholdQueueManager queueManager, StrongholdQueueGUI gui) {
        this.queueManager = queueManager;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "Only players can use this command.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("leave")) {
            if (!queueManager.leave(player.getUniqueId())) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "You are not queued for Stronghold.");
            }
            return true;
        }
        gui.open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            if ("leave".startsWith(prefix)) {
                options.add("leave");
            }
        }
        return options;
    }
}
