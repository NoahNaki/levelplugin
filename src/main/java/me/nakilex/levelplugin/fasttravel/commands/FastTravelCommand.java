package me.nakilex.levelplugin.fasttravel.commands;

import me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

public class FastTravelCommand implements CommandExecutor {
    private final FastTravelGUI gui;
    public FastTravelCommand(FastTravelGUI gui) {
        this.gui = gui;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, MessageType.ERROR, "Only players may use this command.");
            return true;
        }
        gui.open(player);
        return true;
    }
}
