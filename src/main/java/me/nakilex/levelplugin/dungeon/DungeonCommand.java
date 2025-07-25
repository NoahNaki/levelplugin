package me.nakilex.levelplugin.dungeon;

import me.nakilex.levelplugin.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DungeonCommand implements CommandExecutor {
    private final DungeonManager manager;

    public DungeonCommand(Main plugin) {
        this.manager = plugin.getDungeonManager();
        plugin.getCommand("dungeon").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length < 1) return false;
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create" -> {
                manager.getEditor().open(player);
                player.sendMessage(ChatColor.YELLOW + "Opened dungeon editor.");
                return true;
            }
            case "play" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /dungeon play <name>");
                    return true;
                }
                boolean ok = manager.playDungeon(player, args[1]);
                if (ok) player.sendMessage(ChatColor.YELLOW + "Dungeon spawned.");
                else player.sendMessage(ChatColor.RED + "Layout not found.");
                return true;
            }
            case "delete" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /dungeon delete <name>");
                    return true;
                }
                boolean ok = manager.deleteDungeon(args[1]);
                if (ok) player.sendMessage(ChatColor.GREEN + "Dungeon removed.");
                else player.sendMessage(ChatColor.RED + "Dungeon not found.");
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
