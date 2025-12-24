package me.nakilex.levelplugin.fishing.core.action;

import me.nakilex.levelplugin.fishing.api.FishingContext;
import me.nakilex.levelplugin.fishing.api.action.Action;
import me.nakilex.levelplugin.fishing.core.FishingArgs;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;

public class CommandAction implements Action {
    @Override
    public void execute(FishingContext ctx, Map<String, Object> args) {
        String command = FishingArgs.getString(args, "command");
        if (command == null) {
            return;
        }
        Player player = ctx.getPlayer();
        if (player == null) {
            return;
        }
        boolean console = FishingArgs.getBoolean(args, "console", true)
                || FishingArgs.getBoolean(args, "as_console", true);
        String resolved = command
                .replace("{player}", player.getName())
                .replace("{uuid}", player.getUniqueId().toString());
        if (console) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
        } else {
            player.performCommand(resolved);
        }
    }
}
