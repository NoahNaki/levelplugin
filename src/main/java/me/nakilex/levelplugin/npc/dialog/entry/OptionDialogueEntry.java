package me.nakilex.levelplugin.npc.dialog.entry;

import java.util.List;
import java.util.function.Consumer;
import me.nakilex.levelplugin.npc.dialog.messenger.DialogueMessenger;
import me.nakilex.levelplugin.npc.dialog.messenger.OptionMessenger;
import me.nakilex.levelplugin.npc.dialog.model.DialogNpcRef;
import me.nakilex.levelplugin.npc.dialog.model.DialogueCriteria;
import me.nakilex.levelplugin.npc.dialog.model.DialogueModifier;
import me.nakilex.levelplugin.npc.dialog.model.DialogueTrigger;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;
import me.nakilex.levelplugin.npc.dialog.model.SpeakerEntry;
import org.bukkit.entity.Player;

public final class OptionDialogueEntry implements DialogueEntry {
    public record Option(String text, List<DialogueCriteria> criteria,
                         List<DialogueModifier> modifiers, List<DialogueTrigger> triggers) {
        public Option {
            criteria = List.copyOf(criteria);
            modifiers = List.copyOf(modifiers);
            triggers = List.copyOf(triggers);
        }

        public Option(String text) {
            this(text, List.of(), List.of(), List.of());
        }

        public Option(String text, List<DialogueModifier> modifiers, List<DialogueTrigger> triggers) {
            this(text, List.of(), modifiers, triggers);
        }

        public boolean matches(InteractionContext context) {
            return criteria.stream().allMatch(criteria -> criteria.matches(context));
        }
    }

    private final String id;
    private final String name;
    private final DialogNpcRef npc;
    private final String question;
    private final String resultKey;
    private final List<Option> options;
    private final Consumer<Integer> selectionCallback;
    private final SpeakerEntry speaker;
    private final List<DialogueCriteria> criteria;
    private final int priority;

    public OptionDialogueEntry(String id, String name, String question, List<Option> options,
                               Consumer<Integer> selectionCallback, String resultKey) {
        this(id, name, null, question, resultKey, options, selectionCallback);
    }

    public OptionDialogueEntry(String id, String name, DialogNpcRef npc, String question,
                               String resultKey, List<Option> options, Consumer<Integer> selectionCallback) {
        this(id, name, npc, question, resultKey, options, selectionCallback, null, List.of(), 0);
    }

    public OptionDialogueEntry(String id, String name, DialogNpcRef npc, String question,
                               String resultKey, List<Option> options, Consumer<Integer> selectionCallback,
                               SpeakerEntry speaker, List<DialogueCriteria> criteria, int priority) {
        this.id = id;
        this.name = name;
        this.npc = npc;
        this.question = question;
        this.resultKey = resultKey == null || resultKey.isBlank() ? "choice" : resultKey;
        this.options = List.copyOf(options);
        this.selectionCallback = selectionCallback;
        this.speaker = speaker;
        this.criteria = List.copyOf(criteria);
        this.priority = priority;
    }

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    public DialogNpcRef npc() { return npc; }
    public String question() { return question; }
    public String resultKey() { return resultKey; }
    public List<Option> options() { return options; }
    public Consumer<Integer> selectionCallback() { return selectionCallback; }
    public Consumer<Integer> callback() { return selectionCallback; }
    public SpeakerEntry speaker() { return speaker; }
    @Override public List<DialogueCriteria> getCriteria() { return criteria; }
    @Override public int getPriority() { return priority; }

    @Override
    public DialogueMessenger createMessenger(Player player, InteractionContext context) {
        return new OptionMessenger(player, this, context);
    }
}
