package me.nakilex.levelplugin.dialogue;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;

public interface DialogueRenderer {
    void begin(Player player, DialogueSession session);

    void render(Player player, DialogueSession session, DialoguePage page, Component speaker, Component visibleText,
                int lineNumber, int lineCount, List<DialogueAnswer> answers, int selectedAnswerIndex,
                List<Component> replyLines);

    void clear(Player player, DialogueSession session, DialogueEndReason reason);
}
