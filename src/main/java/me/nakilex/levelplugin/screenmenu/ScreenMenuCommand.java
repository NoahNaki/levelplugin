package me.nakilex.levelplugin.screenmenu;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Command to control screen menus. Usage:
 * /cursormenu run <section>
 * /cursormenu stop
 * /cursormenu reload
 * /cursormenu items <material>
 */
public class ScreenMenuCommand implements CommandExecutor {

    private final ScreenMenuManager manager;

    public ScreenMenuCommand(ScreenMenuManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("Usage: /" + label + " <run|stop|reload|items> [section]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "run" -> {
                if (args.length < 2) {
                    player.sendMessage("Specify a section id.");
                    return true;
                }
                boolean shown = manager.showMenu(player, args[1]);
                if (!shown) player.sendMessage("Unknown section: " + args[1]);
            }
            case "stop" -> manager.hideMenu(player);
            case "reload" -> {
                manager.reload();
                player.sendMessage("Screen menus reloaded.");
            }
            case "items" -> {
                if (args.length < 2) {
                    player.sendMessage("Specify a material.");
                    return true;
                }
                Material mat = Material.matchMaterial(args[1]);
                if (mat == null) {
                    player.sendMessage("Unknown material: " + args[1]);
                } else {
                    manager.showItem(player, new ItemStack(mat));
                }
            }
            default -> player.sendMessage("Usage: /" + label + " <run|stop|reload|items> [section]");
        }
        return true;
    }
}

