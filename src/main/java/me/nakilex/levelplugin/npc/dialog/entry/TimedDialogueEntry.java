package me.nakilex.levelplugin.npc.dialog.entry;

import me.nakilex.levelplugin.npc.dialog.messenger.DialogueMessenger;
import me.nakilex.levelplugin.npc.dialog.messenger.TimedMessenger;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;
import org.bukkit.entity.Player;

import java.time.Duration;

/** Message entry that can be revealed over time by its messenger. */
public final class TimedDialogueEntry implements DialogueEntry {
    private final String id;
    private final String name;
    private final String speaker;
    private final String message;
    private final Duration typingDuration;
    private final Duration waitDuration;
    private final boolean allowSkip;

    public TimedDialogueEntry(String id, String name, String speaker, String message,
                              Duration typingDuration, Duration waitDuration, boolean allowSkip) {
        this.id = id;
        this.name = name;
        this.speaker = speaker;
        this.message = message;
        this.typingDuration = typingDuration == null ? Duration.ZERO : typingDuration;
        this.waitDuration = waitDuration == null ? Duration.ZERO : waitDuration;
        this.allowSkip = allowSkip;
    }

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    public String speaker() { return speaker; }
    public String message() { return message; }
    public Duration typingDuration() { return typingDuration; }
    public Duration waitDuration() { return waitDuration; }
    public boolean allowSkip() { return allowSkip; }

    @Override
    public DialogueMessenger createMessenger(Player player, InteractionContext context) {
        return new TimedMessenger(player, this, context);
    }
}
