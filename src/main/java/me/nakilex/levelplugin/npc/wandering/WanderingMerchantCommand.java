package me.nakilex.levelplugin.npc.wandering;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Debug command to spawn or despawn the wandering merchant. */
public class WanderingMerchantCommand implements CommandExecutor {
    private final WanderingMerchantManager manager;

    public WanderingMerchantCommand(WanderingMerchantManager manager) {
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
                manager.spawnNear(player);
                player.sendMessage(ChatColor.YELLOW + "Wandering merchant spawned.");
                return true;
            }
            case "despawn" -> {
                manager.despawn();
                player.sendMessage(ChatColor.YELLOW + "Wandering merchant despawned.");
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
