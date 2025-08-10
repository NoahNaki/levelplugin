package me.nakilex.levelplugin.debug.commands;

import me.nakilex.levelplugin.mob.managers.PlayerToggleManager;
import me.nakilex.levelplugin.utils.ToggleFeedbackUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles miscellaneous debug subcommands.
 * Currently supports toggling MythicMob kill debugging via
 * "/debug mobinfo".
 */
public class DebugCommand implements CommandExecutor {
    private final PlayerToggleManager mobDebugManager;

    public DebugCommand(PlayerToggleManager mobDebugManager) {
        this.mobDebugManager = mobDebugManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("mobinfo")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("Only players can toggle mob info debugging.");
                return true;
            }
            boolean enabled = mobDebugManager.toggle(p);
            ToggleFeedbackUtil.sendToggle(p, "Mob info debug", enabled);
            return true;
        }
        sender.sendMessage("Usage: /debug mobinfo");
        return true;
    }
}
