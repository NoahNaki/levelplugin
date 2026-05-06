package me.nakilex.levelplugin.stageddungeon;

import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;

/** Opens the GUI for a configured staged currency dungeon. */
public class StagedDungeonCommand implements CommandExecutor {
    private final StagedDungeonGUI gui;

    public StagedDungeonCommand(StagedDungeonGUI gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, MessageType.ERROR, "Players only.");
            return true;
        }
        gui.open(player);
        return true;
    }
}
