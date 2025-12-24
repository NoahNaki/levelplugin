package me.nakilex.levelplugin.fishing.core.action;

import me.nakilex.levelplugin.fishing.api.FishingContext;
import me.nakilex.levelplugin.fishing.api.action.Action;
import me.nakilex.levelplugin.fishing.core.FishingArgs;
import org.bukkit.entity.Player;

import java.util.Map;

public class XpAction implements Action {
    @Override
    public void execute(FishingContext ctx, Map<String, Object> args) {
        Player player = ctx.getPlayer();
        if (player == null) {
            return;
        }
        int amount = FishingArgs.getInt(args, "amount", 0);
        if (amount > 0) {
            player.giveExp(amount);
        }
    }
}
