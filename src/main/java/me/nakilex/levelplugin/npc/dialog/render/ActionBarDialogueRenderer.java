package me.nakilex.levelplugin.npc.dialog.render;

import me.nakilex.levelplugin.npc.dialog.engine.DialogueAnswer;
import me.nakilex.levelplugin.npc.dialog.engine.DialogueConditionEvaluator;
import me.nakilex.levelplugin.npc.dialog.engine.DialogueSession;
import me.nakilex.levelplugin.npc.dialog.engine.DialogueTextFormatter;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

public class ActionBarDialogueRenderer implements DialogueRenderer {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private final DialogueConditionEvaluator conditions;

    public ActionBarDialogueRenderer(DialogueConditionEvaluator conditions) {
        this.conditions = conditions;
    }

    @Override
    public void render(Player player, DialogueSession session) {
        String message = pageText(player, session);
        List<DialogueAnswer> answers = session.visibleAnswers(conditions);
        if (!session.typing && !answers.isEmpty()) {
            int index = Math.floorMod(session.selectedAnswerIndex, answers.size());
            DialogueAnswer selected = answers.get(index);
            String number = session.dialogue.answerNumbers() ? (index + 1) + ". " : "";
            message += ChatColor.DARK_GRAY + "  [" + ChatColor.GREEN + number + selected.text() + ChatColor.DARK_GRAY + "]"
                    + ChatColor.GRAY + " (scroll to cycle)";
        }
        player.sendActionBar(LEGACY.deserialize(message));
    }

    @Override
    public void clear(Player player) {
        player.sendActionBar(LEGACY.deserialize(""));
    }

    protected String pageText(Player player, DialogueSession session) {
        DialogueTextFormatter.DisplayText display = DialogueTextFormatter.format(player, session);
        return ChatColor.YELLOW + display.speaker() + ChatColor.WHITE + ": " + reveal(display.text(), session.visibleCharacterCount);
    }

    static String reveal(String text, int visibleCharacters) {
        StringBuilder result = new StringBuilder();
        int visible = 0;
        for (int index = 0; index < text.length() && visible < visibleCharacters; index++) {
            char current = text.charAt(index);
            result.append(current);
            if (current == ChatColor.COLOR_CHAR && index + 1 < text.length()) result.append(text.charAt(++index));
            else visible++;
        }
        return result.toString();
    }
}
