package me.nakilex.levelplugin.friend;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Simple command to open the friends GUI. */
public class FriendsCommand implements CommandExecutor {
    private final FriendGUI gui;

    public FriendsCommand(FriendGUI gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        gui.open((Player) sender);
        return true;
    }
}

