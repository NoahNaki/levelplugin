package me.nakilex.levelplugin.npc.dialog.engine;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Shared placeholder and display formatting for dialogue content. */
public final class DialogueTextFormatter {
    private DialogueTextFormatter() {
    }

    public static DisplayText format(Player player, DialogueSession session) {
        String defaultSpeaker = session.npc != null ? session.npc.getName()
                : session.citizensNpc != null ? session.citizensNpc.getName() : "NPC";
        List<DialogueLine> lines = session.currentPage().lines().stream().map(DialogueLine::parse).toList();
        String speaker = lines.stream().map(DialogueLine::speaker).filter(value -> value != null && !value.isBlank())
                .findFirst().orElse(defaultSpeaker);
        String text = lines.stream().map(DialogueLine::text).filter(value -> !value.isBlank())
                .reduce((left, right) -> left + " " + right).orElse("");
        return new DisplayText(formatDisplay(player, speaker), formatDisplay(player, text));
    }

    /** Replaces built-in and PlaceholderAPI values without altering command syntax or action arguments. */
    public static String formatPlaceholders(Player player, String text) {
        if (text == null) return "";
        String result = replaceIgnoreCase(text, "<player>", player.getName());
        result = replaceIgnoreCase(result, "%player%", player.getName());
        result = replaceIgnoreCase(result, "%player_name%", player.getName());
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            result = PlaceholderAPI.setPlaceholders(player, result);
        }
        return result;
    }

    private static String replaceIgnoreCase(String text, String placeholder, String replacement) {
        return text.replaceAll("(?i)" + Pattern.quote(placeholder), Matcher.quoteReplacement(replacement));
    }

    /** Formats visible dialogue content after placeholder replacement. */
    public static String formatDisplay(Player player, String text) {
        return ChatColor.translateAlternateColorCodes('&', formatPlaceholders(player, text));
    }

    public record DisplayText(String speaker, String text) {
    }
}
