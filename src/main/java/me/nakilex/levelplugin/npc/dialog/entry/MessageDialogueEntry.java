package me.nakilex.levelplugin.npc.dialog.entry;

import me.nakilex.levelplugin.npc.dialog.messenger.DialogueMessenger;
import me.nakilex.levelplugin.npc.dialog.messenger.MessageMessenger;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;
import org.bukkit.entity.Player;

/** Simple one-line NPC message. */
public final class MessageDialogueEntry implements DialogueEntry {
    private final String id;
    private final String name;
    private final String rawLine;
    private final int index;
    private final int total;

    public MessageDialogueEntry(String id, String name, String rawLine, int index, int total) {
        this.id = id;
        this.name = name;
        this.rawLine = rawLine;
        this.index = index;
        this.total = total;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getName() { return name; }

    public String rawLine() { return rawLine; }
    public int index() { return index; }
    public int total() { return total; }

    @Override
    public DialogueMessenger createMessenger(Player player, InteractionContext context) {
        return new MessageMessenger(player, this, context);
    }
}
