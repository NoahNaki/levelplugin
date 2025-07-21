package me.nakilex.levelplugin.scoreboard;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Command to toggle TPS display on the sidebar scoreboard. */
public class ShowTpsCommand implements CommandExecutor {
    private final PlayerScoreboardManager scoreboardManager;
    public ShowTpsCommand(PlayerScoreboardManager mgr) {
        this.scoreboardManager = mgr;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only");
            return true;
        }
        Player p = (Player) sender;
        boolean enabled = scoreboardManager.toggleTps(p);
        p.sendMessage(ChatColor.YELLOW + "TPS display " + (enabled ? "enabled" : "disabled"));
        return true;
    }
}
