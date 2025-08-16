package me.nakilex.levelplugin.music.commands;

import me.nakilex.levelplugin.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to manually stop the currently playing location song.
 */
public class SkipSongCommand implements CommandExecutor {
    private final Main plugin;

    public SkipSongCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can skip songs.");
            return true;
        }
        Player player = (Player) sender;
        if (plugin.getLocationMusicManager() != null) {
            plugin.getLocationMusicManager().skip(player);
        }
        return true;
    }
}
