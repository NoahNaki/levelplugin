package me.nakilex.levelplugin.catacombs;

import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;

public class CatacombsCommand implements CommandExecutor, TabCompleter {
    private final CatacombsManager manager;
    private final CatacombsGUI gui;

    public CatacombsCommand(CatacombsManager manager, CatacombsGUI gui) {
        this.manager = manager;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, MessageType.ERROR, "Only players can use this command.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("exit")) {
            manager.exit(player);
            return true;
        }
        gui.open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            if ("exit".startsWith(prefix)) {
                completions.add("exit");
            }
        }
        return completions;
    }
}
