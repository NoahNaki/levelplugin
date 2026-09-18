package me.nakilex.levelplugin.playerspoofer;

import me.nakilex.levelplugin.Main;
import me.nakilex.playerspoofer.PlayerSpooferPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Bridges {@code /spoofer} to the merged PlayerSpoofer module.
 *
 * <p>PlayerSpoofer used to receive this command directly as a {@code JavaPlugin}. Now that it runs
 * inside LevelPlugin, the command is declared in LevelPlugin's {@code plugin.yml} and handed over
 * here, so the module's own argument handling is untouched.</p>
 */
public final class SpooferCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public SpooferCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        PlayerSpooferPlugin spoofer = plugin.getPlayerSpoofer();
        if (spoofer == null) {
            sender.sendMessage(ChatColor.RED + "PlayerSpoofer is not running. It needs PacketEvents "
                    + "and Citizens, and the standalone PlayerSpoofer plugin must not be installed.");
            return true;
        }
        return spoofer.onCommand(sender, command, label, args);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, String[] args) {
        PlayerSpooferPlugin spoofer = plugin.getPlayerSpoofer();
        return spoofer == null ? List.of() : spoofer.onTabComplete(sender, command, alias, args);
    }
}
