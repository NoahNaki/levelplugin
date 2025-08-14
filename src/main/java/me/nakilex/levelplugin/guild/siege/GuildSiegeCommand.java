package me.nakilex.levelplugin.guild.siege;

import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.ToggleFeedbackUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class GuildSiegeCommand implements CommandExecutor, TabCompleter {
    private final GuildSiegeManager manager;

    public GuildSiegeCommand(GuildSiegeManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("announce")) {
            manager.broadcastSignupMessage();
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players may use this command.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length > 0 && args[0].equalsIgnoreCase("debug")) {
            if (!player.isOp() && !player.hasPermission("siege.debug")) {
                ChatFormatter.sendCenteredMessage(player, ChatColor.RED + "You do not have permission to do that.");
                return true;
            }
            boolean enabled = manager.toggleDebug();
            ToggleFeedbackUtil.sendToggle(player, "Siege debug", enabled);
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("join")) {
            manager.signUp(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("leave")) {
            manager.leave(player.getUniqueId());
            ChatFormatter.sendCenteredMessage(player, ChatColor.GRAY + "You have left the siege queue.");
            return true;
        }
        ChatFormatter.sendCenteredMessage(player, ChatColor.RED + "Usage: /siege <join|leave|announce|debug>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            list.add("join");
            list.add("leave");
            list.add("announce");
            list.add("debug");
            return list;
        }
        return new ArrayList<>();
    }
}
