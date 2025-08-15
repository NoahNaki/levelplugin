package me.nakilex.levelplugin.music.commands;

import me.nakilex.levelplugin.music.LocationMusicManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Command to manually skip the current location song. */
public class SkipSongCommand implements CommandExecutor {
    private final LocationMusicManager musicManager;

    public SkipSongCommand(LocationMusicManager musicManager) {
        this.musicManager = musicManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }
        Player player = (Player) sender;
        musicManager.skipSong(player);
        return true;
    }
}
