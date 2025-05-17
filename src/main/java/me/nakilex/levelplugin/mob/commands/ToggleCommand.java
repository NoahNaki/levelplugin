package me.nakilex.levelplugin.mob.commands;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mob.listeners.MythicMobDeathListener;
import me.nakilex.levelplugin.utils.ToggleFeedbackUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ToggleCommand implements CommandExecutor {
    private final Main plugin;

    public ToggleCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage("Usage: /toggle <dropdetails|dropdetailschat>");
            return true;
        }

        String feature = args[0].toLowerCase();
        switch (feature) {
            case "dropdetails":
                boolean nowHolo = MythicMobDeathListener.toggleDropDetails(player);
                ToggleFeedbackUtil.sendToggle(player, "Drop details (holograms)", nowHolo);
                break;

            case "dropdetailschat":
                boolean nowChat = MythicMobDeathListener.toggleChat(player);
                ToggleFeedbackUtil.sendToggle(player, "Drop details chat", nowChat);
                break;

            default:
                player.sendMessage("Unknown feature: " + feature);
                player.sendMessage("Usage: /toggle <dropdetails|dropdetailschat>");
        }

        return true;
    }
}
