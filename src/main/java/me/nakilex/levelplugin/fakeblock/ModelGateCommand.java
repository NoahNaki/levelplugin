package me.nakilex.levelplugin.fakeblock;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.lootchests.utils.LocationUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import me.nakilex.levelplugin.utils.CommandUtil;

/**
 * Simple command to create and toggle model gates using Nexo furniture.
 */
public class ModelGateCommand implements TabExecutor {

    private final ModelGateManager manager;

    public ModelGateCommand(Main plugin) {
        this.manager = plugin.getModelGateManager();
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
                Location loc = LocationUtils.centerOnBlock(target.getLocation());
                ModelGate gate = new ModelGate(id, loc, open, closed, true);
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtil.filterStartingWith(List.of("create", "toggle", "remove", "list"), args[0]);
        }
        if (args.length == 2 && ("toggle".equalsIgnoreCase(args[0]) || "remove".equalsIgnoreCase(args[0]))) {
            return CommandUtil.filterStartingWith(manager.getGateIds(), args[1]);
        }
        return Collections.emptyList();
    }
}
