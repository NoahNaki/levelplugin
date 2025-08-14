package me.nakilex.levelplugin.debug.commands;

import java.util.Collections;
import java.util.List;

import me.nakilex.levelplugin.debug.gui.DebugGUI;
import me.nakilex.levelplugin.guild.siege.GuildSiegeManager;
import me.nakilex.levelplugin.mob.managers.PlayerToggleManager;
import me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.ToggleFeedbackUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/**
 * Root debug command that hosts various developer utilities.
 * Supported subcommands:
 * <ul>
 *   <li><code>mobinfo</code> – toggles MythicMob kill debug output</li>
 *   <li><code>tps</code> – toggles TPS display on the sidebar scoreboard</li>
 *   <li><code>siege</code> – toggles fast capture mode for sieges</li>
 * </ul>
 */
public class DebugCommand implements TabExecutor {
    private final PlayerToggleManager mobDebugManager;
    private final PlayerScoreboardManager scoreboardManager;
    private final DebugGUI debugGUI;

    public DebugCommand(PlayerToggleManager mobDebugManager,
                        PlayerScoreboardManager scoreboardManager,
                        DebugGUI debugGUI) {
        this.mobDebugManager = mobDebugManager;
        this.scoreboardManager = scoreboardManager;
        this.debugGUI = debugGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player p) {
                debugGUI.open(p);
            } else {
                sender.sendMessage("Usage: /debug <mobinfo|tps|siege>");
            }
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "mobinfo":
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can toggle mob info debugging.");
                    return true;
                }
                boolean enabled = mobDebugManager.toggle(p);
                ToggleFeedbackUtil.sendToggle(p, "Mob info debug", enabled);
                return true;

            case "tps":
                if (!(sender instanceof Player p2)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                boolean tpsEnabled = scoreboardManager.toggleTps(p2);
                ToggleFeedbackUtil.sendToggle(p2, "TPS display", tpsEnabled);
                return true;

            case "siege":
                if (!(sender instanceof Player p3)) {
                    sender.sendMessage("Only players can toggle siege debugging.");
                    return true;
                }
                if (!p3.isOp() && !p3.hasPermission("siege.debug")) {
                    ChatFormatter.sendCenteredMessage(p3, ChatColor.RED + "You do not have permission to do that.");
                    return true;
                }
                boolean siegeEnabled = GuildSiegeManager.getInstance().toggleDebug();
                ToggleFeedbackUtil.sendToggle(p3, "Siege debug", siegeEnabled);
                return true;

            default:
                sender.sendMessage("Unknown debug subcommand: " + sub);
                sender.sendMessage("Usage: /debug <mobinfo|tps|siege>");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = List.of("mobinfo", "tps", "siege");
            return subs.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
