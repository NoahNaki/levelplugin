package me.nakilex.levelplugin.settings.commands;

import me.nakilex.levelplugin.settings.environment.PersonalTimeType;
import me.nakilex.levelplugin.settings.environment.PlayerEnvironmentService;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PersonalTimeCommand implements CommandExecutor {

    private final PlayerEnvironmentService environmentService;

    public PersonalTimeCommand(PlayerEnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "Only players can use this command.");
            return true;
        }
        if (args.length < 1) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Usage: /" + label + " <day|night|sunset|reset>");
            return true;
        }

        PersonalTimeType type = PersonalTimeType.fromInput(args[0]);
        if (type == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Usage: /" + label + " <day|night|sunset|reset>");
            return true;
        }

        environmentService.applyTime(player, type);
        if (type == PersonalTimeType.RESET) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Your personal time was reset to world default.");
        } else {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Your personal time is now " + type.name().toLowerCase(java.util.Locale.ROOT) + ".");
        }
        return true;
    }
}
