package me.nakilex.levelplugin.npc.dialog.engine;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.regex.Matcher;

/** Shared source-line processing for renderers and typewriter accounting. */
public final class DialogueTextFormatter {
    private DialogueTextFormatter() {
    }

    public static DisplayText format(Player player, DialogueSession session) {
        String defaultSpeaker = session.npc != null ? session.npc.getName()
                : session.citizensNpc != null ? session.citizensNpc.getName() : "NPC";
        List<DialogueLine> lines = session.currentPage().lines().stream().map(DialogueLine::parse).toList();
        String speaker = lines.stream().map(DialogueLine::speaker).filter(value -> value != null && !value.isBlank())
                .findFirst().orElse(defaultSpeaker);
        String text = lines.stream().map(DialogueLine::text).filter(value -> !value.isBlank()).reduce((left, right) -> left + " " + right).orElse("");
        return new DisplayText(replacePlayer(speaker, player), replacePlayer(text, player));
    }

    private static String replacePlayer(String text, Player player) {
        return text.replaceAll("(?i)<player>", Matcher.quoteReplacement(player.getName()));
    }

    public record DisplayText(String speaker, String text) {
    }
}
