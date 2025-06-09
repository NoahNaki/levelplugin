package me.nakilex.levelplugin.fakeblock;

import me.nakilex.levelplugin.Main;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Simple command to create and toggle model gates using Nexo furniture.
 */
public class ModelGateCommand implements CommandExecutor {

    private final ModelGateManager manager;

    public ModelGateCommand(Main plugin) {
        this.manager = plugin.getModelGateManager();
        plugin.getCommand("modelgate").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length == 0) return false;
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create":
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "Usage: /modelgate create <id> <openModel> <closedModel>");
                    return true;
                }
                Block target = player.getTargetBlockExact(5);
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Look at a block within 5 blocks.");
                    return true;
                }
                String id = args[1].toLowerCase();
                String open = args[2];
                String closed = args[3];
                boolean town = args.length >= 5 && Boolean.parseBoolean(args[4]);
                Location loc = target.getLocation();
                ModelGate gate = new ModelGate(id, loc, open, closed, town, true);
                manager.createGate(gate);
                player.sendMessage(ChatColor.YELLOW + "Model gate " + id + " created.");
                return true;
            case "toggle":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /modelgate toggle <id>");
                    return true;
                }
                if (manager.toggleGate(player, args[1])) {
                    player.sendMessage(ChatColor.GREEN + "Toggled gate " + args[1] + ".");
                } else {
                    player.sendMessage(ChatColor.RED + "Gate not found.");
                }
                return true;
            case "remove":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /modelgate remove <id>");
                    return true;
                }
                if (manager.removeGate(args[1])) {
                    player.sendMessage(ChatColor.GREEN + "Gate removed.");
                } else {
                    player.sendMessage(ChatColor.RED + "Gate not found.");
                }
                return true;
            case "list":
                var ids = manager.getGateIds();
                if (ids.isEmpty()) {
                    player.sendMessage(ChatColor.YELLOW + "No gates defined.");
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Gates: " + String.join(", ", ids));
                }
                return true;
            default:
                return false;
        }
    }
}
