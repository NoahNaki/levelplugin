package me.nakilex.levelplugin.quests.dialogue.hud;

import me.nakilex.levelplugin.dialogue.DialogueAnswer;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/** Compatibility facade for the Lux-style dialogue HUD composer used by the resource-pack renderer. */
public final class DialogueHudLayout {
    private final DialogueHudResourcePackManager manager;
    private final LuxDialogueHudComposer composer;

    public DialogueHudLayout(JavaPlugin plugin, DialogueHudResourcePackManager manager) {
        this.manager = manager;
        this.composer = new LuxDialogueHudComposer(plugin);
    }

    public Component compose(Component speaker, List<Component> completedLines, Component visibleText,
                             List<DialogueAnswer> answers, int selectedAnswerIndex) {
        return composer.compose(speaker, completedLines, visibleText, answers, selectedAnswerIndex);
    }

    public DialogueHudResourcePackManager manager() {
        return manager;
    }
}
