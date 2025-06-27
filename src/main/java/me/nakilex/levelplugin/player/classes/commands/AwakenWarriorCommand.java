package me.nakilex.levelplugin.player.classes.commands;

import me.nakilex.levelplugin.player.classes.gui.AwakenWarriorMenu;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AwakenWarriorCommand implements CommandExecutor {
    private static final int REQUIRED_LEVEL = 25;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        int level = LevelManager.getInstance().getLevel(player);
        if (level < REQUIRED_LEVEL) {
            player.sendMessage(ChatColor.RED + "You must be level " + REQUIRED_LEVEL + " to awaken.");
            return true;
        }

        AwakenWarriorMenu.open(player);
        return true;
    }
}
