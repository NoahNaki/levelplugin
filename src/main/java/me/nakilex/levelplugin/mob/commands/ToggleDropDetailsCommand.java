package me.nakilex.levelplugin.mob.commands;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mob.listeners.MythicMobDeathListener;
import me.nakilex.levelplugin.utils.ToggleFeedbackUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ToggleDropDetailsCommand implements CommandExecutor {
    private final Main plugin;

    public ToggleDropDetailsCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length != 1 || !args[0].equalsIgnoreCase("dropdetails")) {
            player.sendMessage("Usage: /toggle dropdetails");
            return true;
        }

        boolean nowEnabled = MythicMobDeathListener.toggleDropDetails(player);

        ToggleFeedbackUtil.sendToggle(player, "Drop details", nowEnabled);
        return true;
    }
}
