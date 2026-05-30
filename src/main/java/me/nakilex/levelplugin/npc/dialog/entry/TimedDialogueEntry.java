package me.nakilex.levelplugin.npc.dialog.entry;

import java.time.Duration;
import java.util.List;
import me.nakilex.levelplugin.npc.dialog.messenger.DialogueMessenger;
import me.nakilex.levelplugin.npc.dialog.messenger.TimedMessenger;
import me.nakilex.levelplugin.npc.dialog.model.DialogueCriteria;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;
import me.nakilex.levelplugin.npc.dialog.model.SpeakerEntry;
import org.bukkit.entity.Player;

public final class TimedDialogueEntry implements DialogueEntry {
    private final String id;
    private final String name;
    private final String speaker;
    private final SpeakerEntry speakerEntry;
    private final String message;
    private final Duration typingDuration;
    private final Duration waitDuration;
    private final boolean allowSkip;
    private final List<DialogueCriteria> criteria;
    private final int priority;

    public TimedDialogueEntry(String id, String name, String speaker, String message,
                              Duration typingDuration, Duration waitDuration, boolean allowSkip) {
        this(id, name, speaker, null, message, typingDuration, waitDuration, allowSkip, List.of(), 0);
    }

    public TimedDialogueEntry(String id, String name, SpeakerEntry speaker, String message,
                              Duration typingDuration, Duration waitDuration, boolean allowSkip,
                              List<DialogueCriteria> criteria, int priority) {
        this(id, name, speaker == null ? "" : speaker.displayName(), speaker, message,
                typingDuration, waitDuration, allowSkip, criteria, priority);
    }

    private TimedDialogueEntry(String id, String name, String speaker, SpeakerEntry speakerEntry,
                               String message, Duration typingDuration, Duration waitDuration,
                               boolean allowSkip, List<DialogueCriteria> criteria, int priority) {
        this.id = id;
        this.name = name;
        this.speaker = speaker;
        this.speakerEntry = speakerEntry;
        this.message = message;
        this.typingDuration = typingDuration;
        this.waitDuration = waitDuration;
        this.allowSkip = allowSkip;
        this.criteria = List.copyOf(criteria);
        this.priority = priority;
    }

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    public String speaker() { return speaker; }
    public SpeakerEntry speakerEntry() { return speakerEntry; }
    public String message() { return message; }
    public Duration typingDuration() { return typingDuration; }
    public Duration waitDuration() { return waitDuration; }
    public boolean allowSkip() { return allowSkip; }
    @Override public List<DialogueCriteria> getCriteria() { return criteria; }
    @Override public int getPriority() { return priority; }

    @Override
    public DialogueMessenger createMessenger(Player player, InteractionContext context) {
        return new TimedMessenger(player, this, context);
    }
}
