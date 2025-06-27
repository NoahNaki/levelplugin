package me.nakilex.levelplugin.player.classes.commands;

import me.nakilex.levelplugin.player.classes.managers.AwakeningManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AwakenWarriorCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }
        AwakeningManager.check(player);
        return true;
    }
}
