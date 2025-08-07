package me.nakilex.levelplugin.npc.dungeon;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Debug command to spawn or despawn a dungeon-clearing NPC.
 */
public class DungeonNpcCommand implements CommandExecutor {

    private final DungeonNpcManager manager;

    public DungeonNpcCommand(DungeonNpcManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length < 1) return false;
        switch (args[0].toLowerCase()) {
            case "spawn" -> {
                if (manager.spawn(player)) {
                    player.sendMessage(ChatColor.YELLOW + "Dungeon NPC spawned.");
                } else {
                    player.sendMessage(ChatColor.RED + "Unable to spawn dungeon NPC.");
                }
                return true;
            }
            case "despawn" -> {
                if (manager.despawn(player)) {
                    player.sendMessage(ChatColor.YELLOW + "Dungeon NPC despawned.");
                } else {
                    player.sendMessage(ChatColor.RED + "No dungeon NPC to despawn.");
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
