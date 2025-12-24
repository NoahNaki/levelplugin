package me.nakilex.levelplugin.fishing.core.action;

import me.nakilex.levelplugin.fishing.api.FishingContext;
import me.nakilex.levelplugin.fishing.api.action.Action;
import me.nakilex.levelplugin.fishing.core.FishingArgs;
import me.nakilex.levelplugin.utils.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;

public class MessageAction implements Action {
    @Override
    public void execute(FishingContext ctx, Map<String, Object> args) {
        Player player = ctx.getPlayer();
        if (player == null) {
            return;
        }
        String text = FishingArgs.getString(args, "text");
        if (text == null) {
            return;
        }
        String formatted = ChatColor.translateAlternateColorCodes('&', ChatUtil.applyEmojis(text));
        player.sendMessage(formatted);
    }
}
