package me.nakilex.levelplugin.debug.commands;

import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Scans the loaded chunks of the "mmorpg" world for Nexo furniture
 * blocks and outputs their IDs and coordinates.
 */
public class NexoScanCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        World world = Bukkit.getWorld("mmorpg");
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "World 'mmorpg' not found.");
            return true;
        }

        int total = 0;
        for (Chunk chunk : world.getLoadedChunks()) {
            int minY = world.getMinHeight();
            int maxY = world.getMaxHeight();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = minY; y <= maxY; y++) {
                        Block block = chunk.getBlock(x, y, z);
                        FurnitureMechanic mech = NexoFurniture.furnitureMechanic(block);
                        if (mech != null) {
                            total++;
                            Location loc = block.getLocation();
                            sender.sendMessage(ChatColor.GRAY + "- " + mech.getItemID()
                                    + " @ " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
                        }
                    }
                }
            }
        }

        sender.sendMessage(ChatColor.GREEN + "Total furniture found: " + total);
        return true;
    }
}
