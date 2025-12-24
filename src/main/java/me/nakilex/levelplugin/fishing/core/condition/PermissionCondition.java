package me.nakilex.levelplugin.fishing.core.condition;

import me.nakilex.levelplugin.fishing.api.FishingContext;
import me.nakilex.levelplugin.fishing.api.condition.Condition;
import me.nakilex.levelplugin.fishing.core.FishingArgs;
import org.bukkit.entity.Player;

import java.util.Map;

public class PermissionCondition implements Condition {
    @Override
    public boolean test(FishingContext ctx, Map<String, Object> args) {
        Player player = ctx.getPlayer();
        if (player == null) {
            return false;
        }
        String permission = FishingArgs.getString(args, "permission", FishingArgs.getString(args, "value"));
        if (permission == null || permission.isBlank()) {
            return false;
        }
        return player.hasPermission(permission);
    }
}
