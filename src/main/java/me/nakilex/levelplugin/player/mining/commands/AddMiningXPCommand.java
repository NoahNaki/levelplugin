package me.nakilex.levelplugin.player.mining.commands;

import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import me.nakilex.levelplugin.player.attributes.commands.LifeSkillCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import me.nakilex.levelplugin.utils.CommandUtil;
import java.util.Collections;
import java.util.List;

public class AddMiningXPCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return LifeSkillCommand.handleLegacyAddXp(sender, ToolDiscipline.MINING, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtil.onlinePlayerNames(args[0]);
        }
        if (args.length == 2) {
            return CommandUtil.numberOptions(args[1], 1, 5, 10, 25, 50, 100, 250, 500, 1000);
        }
        return Collections.emptyList();
    }
}
