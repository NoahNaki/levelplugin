package me.nakilex.levelplugin.quests.dialogue.hud;

import me.nakilex.levelplugin.dialogue.DialogueAnswer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * LuxDialogues-style action-bar HUD composer that treats glyphs as independent pixel layers instead of inline text.
 */
public final class DialogueHudLayout {
    private final JavaPlugin plugin;
    private final DialogueHudResourcePackManager manager;

    public DialogueHudLayout(JavaPlugin plugin, DialogueHudResourcePackManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public Component compose(Component speaker, List<Component> completedLines, Component visibleText,
                             List<DialogueAnswer> answers, int selectedAnswerIndex) {
        int startOffset = plugin.getConfig().getInt("dialogue-hud.layout.start-offset", 320);

        Component hud = Component.empty().append(DialogueHudGlyphs.offset(startOffset));

        HudLayer dialogue = composeDialogueBoxLayer(completedLines, visibleText);
        hud = hud.append(dialogue.component());
        int currentLayerWidth = dialogue.width();

        HudLayer nameplate = composeNameplateLayer(speaker);
        if (nameplate.width() > 0) {
            hud = hud.append(DialogueHudGlyphs.offset(-currentLayerWidth));
            hud = hud.append(nameplate.component());
            currentLayerWidth = nameplate.width();
        }

        HudLayer answerLayer = composeAnswerLayer(answers, selectedAnswerIndex);
        if (answerLayer.width() > 0) {
            hud = hud.append(DialogueHudGlyphs.offset(-currentLayerWidth));
            hud = hud.append(answerLayer.component());
        }

        return hud;
    }

    private HudLayer composeDialogueBoxLayer(List<Component> completedLines, Component visibleText) {
        int width = plugin.getConfig().getInt("dialogue-hud.layout.dialogue.width", 209);
        int textX = plugin.getConfig().getInt("dialogue-hud.layout.dialogue.text-x", 22);
        int textResetExtra = plugin.getConfig().getInt("dialogue-hud.layout.dialogue.text-reset-extra", 0);

        Component text = composeDialogueText(completedLines, visibleText);
        int textWidth = DialogueTextWidth.width(text);
        int reset = Math.max(0, width - textX - textWidth + textResetExtra);

        Component layer = Component.empty()
                .append(DialogueHudGlyphs.background())
                .append(DialogueHudGlyphs.offset(-width + textX))
                .append(text)
                .append(DialogueHudGlyphs.offset(reset));
        return new HudLayer(layer, width + textResetExtra);
    }

    private HudLayer composeNameplateLayer(Component speaker) {
        if (!plugin.getConfig().getBoolean("dialogue-hud.layout.nameplate.enabled", true)) {
            return HudLayer.empty();
        }

        int x = plugin.getConfig().getInt("dialogue-hud.layout.nameplate.x", 18);
        int padding = plugin.getConfig().getInt("dialogue-hud.layout.nameplate.text-padding", 5);
        int midWidth = Math.max(1, plugin.getConfig().getInt("dialogue-hud.layout.nameplate.mid-width", 2));

        Component safeSpeaker = speaker == null
                ? Component.text("NPC", NamedTextColor.YELLOW)
                : speaker.colorIfAbsent(NamedTextColor.YELLOW);

        String plainSpeaker = PlainTextComponentSerializer.plainText().serialize(safeSpeaker);
        int speakerWidth = DialogueTextWidth.width(plainSpeaker);
        int repeats = Math.max(1, (speakerWidth + padding * 2) / midWidth);
        int plateWidth = 3 + repeats * midWidth + 3;

        Component plate = Component.empty()
                .append(DialogueHudGlyphs.offset(x))
                .append(DialogueHudGlyphs.nameplateLeft());

        for (int i = 0; i < repeats; i++) {
            plate = plate.append(DialogueHudGlyphs.nameplateMiddle());
        }

        plate = plate.append(DialogueHudGlyphs.nameplateRight());
        plate = plate.append(DialogueHudGlyphs.offset(-plateWidth + padding));
        plate = plate.append(DialogueHudGlyphs.defaultText(plainSpeaker, NamedTextColor.YELLOW));
        plate = plate.append(DialogueHudGlyphs.offset(Math.max(0, plateWidth - padding - speakerWidth)));

        return new HudLayer(plate, x + plateWidth);
    }

    private HudLayer composeAnswerLayer(List<DialogueAnswer> answers, int selectedAnswerIndex) {
        if (!plugin.getConfig().getBoolean("dialogue-hud.layout.answers.enabled", true)
                || answers == null
                || answers.isEmpty()) {
            return HudLayer.empty();
        }

        int x = plugin.getConfig().getInt("dialogue-hud.layout.answers.x", 245);
        int width = plugin.getConfig().getInt("dialogue-hud.layout.answers.background-width", 134);
        int textX = plugin.getConfig().getInt("dialogue-hud.layout.answers.text-x", 14);
        int gap = plugin.getConfig().getInt("dialogue-hud.layout.answers.gap", 10);
        int maxVisible = plugin.getConfig().getInt("dialogue-hud.layout.answers.max-visible", 2);

        Component result = Component.empty().append(DialogueHudGlyphs.offset(x));
        int limit = Math.min(answers.size(), maxVisible);

        for (int i = 0; i < limit; i++) {
            DialogueAnswer answer = answers.get(i);
            boolean selected = i == selectedAnswerIndex;
            String text = (i + 1) + ". " + answer.text();
            int textWidth = DialogueTextWidth.width(text);

            result = result
                    .append(DialogueHudGlyphs.answerBackground())
                    .append(DialogueHudGlyphs.offset(-width + textX));

            if (selected) {
                result = result
                        .append(DialogueHudGlyphs.selector())
                        .append(DialogueHudGlyphs.offset(6));
            }

            result = result.append(DialogueHudGlyphs.defaultText(text, selected ? NamedTextColor.WHITE : NamedTextColor.GRAY));

            int consumedInsideBox = textX + (selected ? 20 : 0) + textWidth;
            int remaining = Math.max(0, width - consumedInsideBox);
            result = result.append(DialogueHudGlyphs.offset(remaining + gap));
        }

        int layerWidth = x + limit * (width + gap);
        return new HudLayer(result, layerWidth);
    }

    private Component composeDialogueText(List<Component> completedLines, Component visibleText) {
        int maxLines = plugin.getConfig().getInt("dialogue-hud.layout.dialogue.max-lines", 3);
        List<Component> lines = new ArrayList<>();

        if (completedLines != null) {
            lines.addAll(completedLines);
        }

        if (visibleText != null) {
            lines.add(visibleText);
        }

        if (lines.size() > maxLines) {
            lines = lines.subList(lines.size() - maxLines, lines.size());
        }

        Component result = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                result = result.append(DialogueHudGlyphs.defaultText("  ", NamedTextColor.WHITE));
            }

            result = result.append(DialogueHudGlyphs.defaultFont(
                    (lines.get(i) == null ? Component.empty() : lines.get(i)).colorIfAbsent(NamedTextColor.WHITE)
            ));
        }

        return result;
    }

    public DialogueHudResourcePackManager manager() {
        return manager;
    }

    private record HudLayer(Component component, int width) {
        private static HudLayer empty() {
            return new HudLayer(Component.empty(), 0);
        }
    }
}
