package me.nakilex.levelplugin.npc.dialog.entry;

import me.nakilex.levelplugin.npc.dialog.messenger.DialogueMessenger;
import me.nakilex.levelplugin.npc.dialog.model.DialogueModifier;
import me.nakilex.levelplugin.npc.dialog.model.DialogueTrigger;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;
import org.bukkit.entity.Player;

import java.util.List;

/** Data object that creates a messenger responsible for presenting one dialogue step. */
public interface DialogueEntry {
    String getId();
    String getName();
    DialogueMessenger createMessenger(Player player, InteractionContext context);

    default List<DialogueTrigger> getTriggers() {
        return List.of();
    }

    default List<DialogueModifier> getModifiers() {
        return List.of();
    }
}
