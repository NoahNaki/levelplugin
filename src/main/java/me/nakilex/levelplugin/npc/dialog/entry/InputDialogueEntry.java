package me.nakilex.levelplugin.npc.dialog.entry;

import me.nakilex.levelplugin.npc.dialog.messenger.DialogueMessenger;
import me.nakilex.levelplugin.npc.dialog.messenger.InputMessenger;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;
import org.bukkit.entity.Player;

import java.util.function.Predicate;

/** Chat-input dialogue entry that stores a validated response in the interaction context. */
public final class InputDialogueEntry implements DialogueEntry {
    private final String id;
    private final String name;
    private final String prompt;
    private final String resultKey;
    private final Predicate<String> validator;

    public InputDialogueEntry(String id, String name, String prompt, String resultKey, Predicate<String> validator) {
        this.id = id;
        this.name = name;
        this.prompt = prompt;
        this.resultKey = resultKey == null || resultKey.isBlank() ? "input" : resultKey;
        this.validator = validator == null ? input -> true : validator;
    }

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    public String prompt() { return prompt; }
    public String resultKey() { return resultKey; }
    public boolean isValid(String input) { return validator.test(input); }

    @Override
    public DialogueMessenger createMessenger(Player player, InteractionContext context) {
        return new InputMessenger(player, this, context);
    }
}
