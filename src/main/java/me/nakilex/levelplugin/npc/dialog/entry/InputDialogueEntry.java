package me.nakilex.levelplugin.npc.dialog.entry;

import java.util.List;
import java.util.function.Predicate;
import me.nakilex.levelplugin.npc.dialog.messenger.DialogueMessenger;
import me.nakilex.levelplugin.npc.dialog.messenger.InputMessenger;
import me.nakilex.levelplugin.npc.dialog.model.DialogueCriteria;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;
import me.nakilex.levelplugin.npc.dialog.model.SpeakerEntry;
import org.bukkit.entity.Player;

public final class InputDialogueEntry implements DialogueEntry {
    private final String id;
    private final String name;
    private final String prompt;
    private final String resultKey;
    private final Predicate<String> validator;
    private final String invalidMessage;
    private final SpeakerEntry speaker;
    private final List<DialogueCriteria> criteria;
    private final int priority;

    public InputDialogueEntry(String id, String name, String prompt, String resultKey,
                              Predicate<String> validator) {
        this(id, name, prompt, resultKey, validator, "That response is not valid. Try again.");
    }

    public InputDialogueEntry(String id, String name, String prompt, String resultKey,
                              Predicate<String> validator, String invalidMessage) {
        this(id, name, prompt, resultKey, validator, invalidMessage, null, List.of(), 0);
    }

    public InputDialogueEntry(String id, String name, String prompt, String resultKey,
                              Predicate<String> validator, String invalidMessage,
                              SpeakerEntry speaker, List<DialogueCriteria> criteria, int priority) {
        this.id = id;
        this.name = name;
        this.prompt = prompt;
        this.resultKey = resultKey == null || resultKey.isBlank() ? "input" : resultKey;
        this.validator = validator == null ? input -> true : validator;
        this.invalidMessage = invalidMessage;
        this.speaker = speaker;
        this.criteria = List.copyOf(criteria);
        this.priority = priority;
    }

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    public String prompt() { return prompt; }
    public String resultKey() { return resultKey; }
    public Predicate<String> validator() { return validator; }
    public boolean isValid(String value) { return validator == null || validator.test(value); }
    public String invalidMessage() { return invalidMessage; }
    public SpeakerEntry speaker() { return speaker; }
    @Override public List<DialogueCriteria> getCriteria() { return criteria; }
    @Override public int getPriority() { return priority; }

    @Override
    public DialogueMessenger createMessenger(Player player, InteractionContext context) {
        return new InputMessenger(player, this, context);
    }
}
