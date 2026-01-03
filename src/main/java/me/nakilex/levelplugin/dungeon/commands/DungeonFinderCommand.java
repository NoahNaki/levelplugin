package me.nakilex.levelplugin.dungeon.commands;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.gui.DungeonListGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DungeonFinderCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        DungeonListGUI gui = Main.getInstance().getDungeonListGUI();
        if (gui == null) {
            player.sendMessage("§cDungeon finder is currently unavailable.");
            return true;
        }
        gui.open(player);
        return true;
    }
}
