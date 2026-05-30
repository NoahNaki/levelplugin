package me.nakilex.levelplugin.npc.dialog;

import java.util.Map;
import me.clip.placeholderapi.PlaceholderAPI;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;
import org.bukkit.Bukkit;

public final class PlaceholderResolver {
    private PlaceholderResolver() {}

    public static String resolve(String input, InteractionContext context) {
        if (input == null || input.isEmpty()) return input == null ? "" : input;
        String resolved = input
                .replace("<player>", context.player().getName())
                .replace("<npc>", context.npc() == null ? "" : context.npc().name())
                .replace("<quest>", context.quest() == null ? "" : context.quest().getName());
        for (Map.Entry<String, Object> variable : context.variables().entrySet()) {
            String value = String.valueOf(variable.getValue());
            resolved = resolved.replace("<context:" + variable.getKey() + ">", value)
                    .replace("<" + variable.getKey() + ">", value);
        }
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            resolved = PlaceholderAPI.setPlaceholders(context.player(), resolved);
        }
        return resolved;
    }
}
