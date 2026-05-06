package me.nakilex.levelplugin.stageddungeon;

import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;

public class GemDungeonCommand implements CommandExecutor {
    private final StagedDungeonGUI gui;

    public GemDungeonCommand(StagedDungeonGUI gui) {
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
