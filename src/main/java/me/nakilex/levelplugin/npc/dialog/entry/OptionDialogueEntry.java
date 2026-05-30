package me.nakilex.levelplugin.npc.dialog.entry;

import me.nakilex.levelplugin.npc.dialog.messenger.DialogueMessenger;
import me.nakilex.levelplugin.npc.dialog.messenger.OptionMessenger;
import me.nakilex.levelplugin.npc.dialog.model.DialogueModifier;
import me.nakilex.levelplugin.npc.dialog.model.DialogueTrigger;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

/** Scroll-wheel choice entry. */
public final class OptionDialogueEntry implements DialogueEntry {
    private final String id;
    private final String name;
    private final String question;
    private final List<Option> options;
    private final Consumer<Integer> callback;
    private final String resultKey;

    public OptionDialogueEntry(String id, String name, String question, List<Option> options,
                               Consumer<Integer> callback, String resultKey) {
        this.id = id;
        this.name = name;
        this.question = question;
        this.options = List.copyOf(options);
        this.callback = callback;
        this.resultKey = resultKey == null || resultKey.isBlank() ? "choice" : resultKey;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getName() { return name; }

    public String question() { return question; }
    public List<Option> options() { return options; }
    public Consumer<Integer> callback() { return callback; }
    public String resultKey() { return resultKey; }

    @Override
    public DialogueMessenger createMessenger(Player player, InteractionContext context) {
        return new OptionMessenger(player, this, context);
    }

    public record Option(String text, List<DialogueModifier> modifiers, List<DialogueTrigger> triggers) {
        public Option(String text) {
            this(text, List.of(), List.of());
        }
    }
}
