package me.nakilex.levelplugin.stronghold.commands;

import me.nakilex.levelplugin.stronghold.StrongholdQueueManager;
import me.nakilex.levelplugin.stronghold.gui.StrongholdQueueGUI;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class StrongholdCommand implements CommandExecutor, TabCompleter {
    private final StrongholdQueueGUI gui;
    private final StrongholdQueueManager queueManager;

    public StrongholdCommand(StrongholdQueueGUI gui, StrongholdQueueManager queueManager) {
        this.gui = gui;
        this.queueManager = queueManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("leave")) {
            boolean left = queueManager.leave(player.getUniqueId());
            ChatMessageUtil.send(player, left ? ChatMessageUtil.MessageType.INFO : ChatMessageUtil.MessageType.WARNING,
                    left ? "Left stronghold queue." : "You are not queued.");
            gui.refresh();
            return true;
        }
        gui.open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("leave");
        }
        return List.of();
    }
}
