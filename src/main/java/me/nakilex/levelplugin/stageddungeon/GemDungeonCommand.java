package me.nakilex.levelplugin.stageddungeon;

import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;

public class GemDungeonCommand implements CommandExecutor {
    private final StagedDungeonGUI gui;
    private final StagedDungeonManager manager;
    private final StagedDungeonDefinition definition;

    public GemDungeonCommand(StagedDungeonGUI gui, StagedDungeonManager manager, StagedDungeonDefinition definition) {
        this.gui = gui;
        this.manager = manager;
        this.definition = definition;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, MessageType.ERROR, "Players only.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("debug")) {
            manager.sendDebug(player, definition);
            return true;
        }
        gui.open(player);
        return true;
    }
}
