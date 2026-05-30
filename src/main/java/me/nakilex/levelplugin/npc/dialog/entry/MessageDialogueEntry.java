package me.nakilex.levelplugin.npc.dialog.entry;

import java.util.List;
import me.nakilex.levelplugin.npc.dialog.messenger.DialogueMessenger;
import me.nakilex.levelplugin.npc.dialog.messenger.MessageMessenger;
import me.nakilex.levelplugin.npc.dialog.model.DialogueCriteria;
import me.nakilex.levelplugin.npc.dialog.model.DialogueModifier;
import me.nakilex.levelplugin.npc.dialog.model.DialogueTrigger;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;
import me.nakilex.levelplugin.npc.dialog.model.SpeakerEntry;
import org.bukkit.entity.Player;

public final class MessageDialogueEntry implements DialogueEntry {
    private final String id;
    private final String name;
    private final String line;
    private final int index;
    private final int total;
    private final SpeakerEntry speaker;
    private final List<DialogueCriteria> criteria;
    private final List<DialogueModifier> modifiers;
    private final List<DialogueTrigger> triggers;
    private final int priority;

    public MessageDialogueEntry(String id, String name, String line, int index, int total) {
        this(id, name, line, index, total, null, List.of(), List.of(), List.of(), 0);
    }

    public MessageDialogueEntry(String id, String name, String line, int index, int total,
                                SpeakerEntry speaker, List<DialogueCriteria> criteria,
                                List<DialogueModifier> modifiers, List<DialogueTrigger> triggers,
                                int priority) {
        this.id = id;
        this.name = name;
        this.line = line;
        this.index = index;
        this.total = total;
        this.speaker = speaker;
        this.criteria = List.copyOf(criteria);
        this.modifiers = List.copyOf(modifiers);
        this.triggers = List.copyOf(triggers);
        this.priority = priority;
    }

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    public String line() { return line; }
    public String rawLine() { return line; }
    public int index() { return index; }
    public int total() { return total; }
    public SpeakerEntry speaker() { return speaker; }
    @Override public List<DialogueCriteria> getCriteria() { return criteria; }
    @Override public List<DialogueModifier> getModifiers() { return modifiers; }
    @Override public List<DialogueTrigger> getTriggers() { return triggers; }
    @Override public int getPriority() { return priority; }

    @Override
    public DialogueMessenger createMessenger(Player player, InteractionContext context) {
        return new MessageMessenger(player, this, context);
    }
}
