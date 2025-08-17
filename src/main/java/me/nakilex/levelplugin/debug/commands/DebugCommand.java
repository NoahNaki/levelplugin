package me.nakilex.levelplugin.debug.commands;

import java.util.Collections;
import java.util.List;

import me.nakilex.levelplugin.debug.gui.DebugGUI;
import me.nakilex.levelplugin.debug.AutoCastManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.mob.managers.PlayerToggleManager;
import me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager;
import me.nakilex.levelplugin.utils.ToggleFeedbackUtil;
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
 *   <li><code>siege</code> – toggles fast guild siege capture mode</li>
 * </ul>
 */
public class DebugCommand implements TabExecutor {
    private final PlayerToggleManager mobDebugManager;
    private final PlayerScoreboardManager scoreboardManager;
    private final DebugGUI debugGUI;
    private final AutoCastManager autoCastManager = new AutoCastManager();

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
                sender.sendMessage("Usage: /debug <mobinfo|tps|siege|autocast>");
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
                boolean fast = me.nakilex.levelplugin.guild.siege.GuildSiegeManager.getInstance().toggleFastCapture();
                sender.sendMessage("Fast siege mode " + (fast ? "enabled" : "disabled"));
                return true;

            case "autocast":
                if (!(sender instanceof Player p3)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(p3.getUniqueId());
                if (ps.playerClass != PlayerClass.MAGE) {
                    p3.sendMessage("Mage class required for autocast debug.");
                    return true;
                }
                boolean auto = autoCastManager.toggle(p3, "fireball");
                ToggleFeedbackUtil.sendToggle(p3, "Mage autocast", auto);
                return true;

            default:
                sender.sendMessage("Unknown debug subcommand: " + sub);
                sender.sendMessage("Usage: /debug <mobinfo|tps|siege|autocast>");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = List.of("mobinfo", "tps", "siege", "autocast");
            return subs.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
