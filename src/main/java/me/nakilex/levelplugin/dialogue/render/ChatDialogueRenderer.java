package me.nakilex.levelplugin.dialogue.render;

import me.nakilex.levelplugin.dialogue.DialogueAnswer;
import me.nakilex.levelplugin.dialogue.DialogueEndReason;
import me.nakilex.levelplugin.dialogue.DialoguePage;
import me.nakilex.levelplugin.dialogue.DialogueRenderer;
import me.nakilex.levelplugin.dialogue.DialogueSession;
import me.nakilex.levelplugin.utils.ChatUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ChatDialogueRenderer implements DialogueRenderer {
    private static final int CLEAR_LINES = 18;
    private final Set<UUID> conversations = new HashSet<>();

    @Override
    public void begin(Player player, DialogueSession session) {
        if (player != null) conversations.add(player.getUniqueId());
    }

    @Override
    public void render(Player player, DialogueSession session, DialoguePage page, Component speaker,
                       List<Component> completedPageLines, Component visibleText, int pageLineIndex, int pageLineCount,
                       List<DialogueAnswer> answers, int selectedAnswerIndex, List<Component> replyLines) {
        if (player == null) return;
        conversations.add(player.getUniqueId());

        Component message = Component.text("\n".repeat(CLEAR_LINES))
                .append(Component.text("[", NamedTextColor.DARK_GRAY))
                .append(speaker.colorIfAbsent(NamedTextColor.YELLOW))
                .append(Component.text("]", NamedTextColor.DARK_GRAY))
                .append(Component.text("  ", NamedTextColor.DARK_GRAY))
                .append(Component.text(pageLineIndex + "/" + Math.max(1, pageLineCount), NamedTextColor.GRAY));

        for (Component completedLine : completedPageLines) {
            message = message.append(Component.newline())
                    .append(completedLine.colorIfAbsent(NamedTextColor.WHITE));
        }

        if (visibleText != null && !visibleText.equals(Component.empty())) {
            message = message.append(Component.newline())
                    .append(visibleText.colorIfAbsent(NamedTextColor.WHITE));
        }

        for (Component reply : replyLines) {
            message = message.append(Component.newline())
                    .append(Component.text("↳ ", NamedTextColor.DARK_GRAY))
                    .append(reply.colorIfAbsent(NamedTextColor.GRAY));
        }

        if (answers != null && !answers.isEmpty()) {
            message = message.append(Component.newline()).append(Component.newline())
                    .append(Component.text("Answers:", NamedTextColor.AQUA));
            for (int i = 0; i < answers.size(); i++) {
                DialogueAnswer answer = answers.get(i);
                boolean selected = i == selectedAnswerIndex;
                message = message.append(Component.newline())
                        .append(Component.text(selected ? "➤ " : "  ", selected ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY))
                        .append(Component.text((i + 1) + ". ", selected ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                        .append(ChatUtil.formattedComponent(answer.text()).colorIfAbsent(selected ? NamedTextColor.GREEN : NamedTextColor.WHITE));
            }
            message = message.append(Component.newline())
                    .append(Component.text("(Scroll to cycle, click NPC to confirm)", NamedTextColor.GRAY));
        } else if (session.state() == DialogueSession.State.WAITING) {
            message = message.append(Component.newline())
                    .append(Component.text("[Click to continue]", NamedTextColor.DARK_GRAY));
        }

        player.sendMessage(message);
    }

    @Override
    public void clear(Player player, DialogueSession session, DialogueEndReason reason) {
        if (player != null) conversations.remove(player.getUniqueId());
    }
}
