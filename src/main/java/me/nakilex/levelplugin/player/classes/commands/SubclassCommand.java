package me.nakilex.levelplugin.player.classes.commands;

import me.nakilex.levelplugin.player.classes.gui.SubclassGUI;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SubclassCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }
        StatsManager.PlayerStats ps = StatsManager.getInstance()
                .getPlayerStats(player.getUniqueId());
        // Ensure all starter classes are available
        PlayerClass[] starters = {
                PlayerClass.WARRIOR,
                PlayerClass.ROGUE,
                PlayerClass.ARCHER,
                PlayerClass.MAGE,
                PlayerClass.CLERIC
        };
        for (PlayerClass cls : starters) {
            ps.unlockedClasses.add(cls);
        }

        SubclassGUI.open(player);
        return true;
    }
}
