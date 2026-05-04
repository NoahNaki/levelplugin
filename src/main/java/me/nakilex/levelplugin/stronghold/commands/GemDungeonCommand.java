package me.nakilex.levelplugin.stronghold.commands;

import me.nakilex.levelplugin.stronghold.run.GemDungeonManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;

public class GemDungeonCommand implements TabExecutor {
    private final GemDungeonManager manager;
    private final me.nakilex.levelplugin.stronghold.gui.GemDungeonGUI gui;
    public GemDungeonCommand(GemDungeonManager manager, me.nakilex.levelplugin.stronghold.gui.GemDungeonGUI gui) { this.manager = manager; this.gui = gui; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "Only players can use this command.");
            return true;
        }
        if (args.length == 0 || "open".equalsIgnoreCase(args[0])) {
            gui.open(player);
            return true;
        }
        if ("challenge".equalsIgnoreCase(args[0])) manager.challenge(player);
        else if ("sweep".equalsIgnoreCase(args[0])) manager.sweep(player);
        else ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "Usage: /gemdungeon <challenge|sweep>");
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? List.of("open", "challenge", "sweep") : List.of();
    }
}
