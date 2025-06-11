package me.nakilex.levelplugin.environment.stage;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.nakilex.levelplugin.environment.stage.StageSelectionStore;

/**
 * Allows setting stage selection positions like WorldEdit's //pos1 and //pos2.
 */
public class TownPosCommand implements CommandExecutor {
    private final boolean first;

    public TownPosCommand(boolean first) {
        this.first = first;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Location loc;
        Block target = p.getTargetBlockExact(100);
        if (target != null) {
            loc = target.getLocation();
        } else {
            loc = p.getLocation().getBlock().getLocation();
        }
        if (first) {
            StageSelectionStore.setPos1(p.getUniqueId(), loc);
            p.sendMessage(ChatColor.AQUA + "Pos1 set " + format(loc));
        } else {
            StageSelectionStore.setPos2(p.getUniqueId(), loc);
            p.sendMessage(ChatColor.AQUA + "Pos2 set " + format(loc));
        }
        return true;
    }

    private static String format(Location loc) {
        return loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ();
    }
}
