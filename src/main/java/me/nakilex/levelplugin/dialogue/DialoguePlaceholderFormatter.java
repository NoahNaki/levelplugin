package me.nakilex.levelplugin.dialogue;

import me.clip.placeholderapi.PlaceholderAPI;
import me.nakilex.levelplugin.utils.ChatUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DialoguePlaceholderFormatter {
    private static final Pattern HEX = Pattern.compile("(?i)(?<![<\\w])#([0-9a-f]{6})");

    public String format(Player player, String raw) {
        String formatted = raw == null ? "" : raw;
        String name = player == null ? "" : Matcher.quoteReplacement(player.getName());
        formatted = formatted.replaceAll("(?i)<player>", name)
                .replaceAll("(?i)%player%", name)
                .replaceAll("(?i)%player_name%", name);
        if (player != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            formatted = PlaceholderAPI.setPlaceholders(player, formatted);
        }
        formatted = translateHex(formatted);
        return ChatColor.translateAlternateColorCodes('&', formatted);
    }

    public Component component(Player player, String raw) {
        return ChatUtil.formattedComponent(format(player, raw));
    }

    private static String translateHex(String input) {
        Matcher matcher = HEX.matcher(input);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) replacement.append('§').append(c);
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(output);
        return output.toString();
    }
}
