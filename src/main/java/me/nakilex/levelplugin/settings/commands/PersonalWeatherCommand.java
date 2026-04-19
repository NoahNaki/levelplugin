package me.nakilex.levelplugin.settings.commands;

import me.nakilex.levelplugin.settings.environment.PersonalWeatherType;
import me.nakilex.levelplugin.settings.environment.PlayerEnvironmentService;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PersonalWeatherCommand implements CommandExecutor {

    private final PlayerEnvironmentService environmentService;

    public PersonalWeatherCommand(PlayerEnvironmentService environmentService) {
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
                    "Usage: /" + label + " <clear|rain|thunder|reset>");
            return true;
        }

        PersonalWeatherType type = PersonalWeatherType.fromInput(args[0]);
        if (type == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Usage: /" + label + " <clear|rain|thunder|reset>");
            return true;
        }

        environmentService.applyWeather(player, type);
        if (type == PersonalWeatherType.RESET) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Your personal weather was reset to world default.");
        } else {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Your personal weather is now " + type.name().toLowerCase(java.util.Locale.ROOT) + ".");
        }
        return true;
    }
}
