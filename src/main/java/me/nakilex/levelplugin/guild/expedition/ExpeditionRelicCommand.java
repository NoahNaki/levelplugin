package me.nakilex.levelplugin.guild.expedition;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Command entry point for expedition relic interactions. */
public final class ExpeditionRelicCommand implements CommandExecutor, TabCompleter {

    private final ExpeditionRelicManager manager;

    public ExpeditionRelicCommand(ExpeditionRelicManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can manage expedition relics.");
            return true;
        }
        if (args.length == 0) {
            manager.openBoard(player);
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "invest" -> manager.invest(player);
            case "start" -> manager.startExpedition(player);
            case "maintain", "upkeep" -> manager.depositMaintenance(player);
            default -> player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /expedition [invest|start|maintain].");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("invest", "start", "maintain");
        }
        return Collections.emptyList();
    }
}
