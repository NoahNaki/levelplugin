package me.nakilex.levelplugin.arena;

import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType.ERROR;

/**
 * Command to open the arena GUI or directly join/leave the queue.
 */
public class ArenaCommand implements TabExecutor {
    private final ArenaGUI gui;

    public ArenaCommand(ArenaGUI gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ERROR, "Only players can use this command.");
            return true;
        }
        ArenaManager arena = ArenaManager.getInstance();
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("join")) {
                arena.joinQueue(player);
            } else if (args[0].equalsIgnoreCase("leave")) {
                arena.leaveQueue(player);
            } else {
                gui.open(player);
            }
        } else {
            gui.open(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String arg = args[0].toLowerCase();
            if ("join".startsWith(arg)) completions.add("join");
            if ("leave".startsWith(arg)) completions.add("leave");
        }
        return completions;
    }
}
