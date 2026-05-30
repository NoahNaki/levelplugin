package me.nakilex.levelplugin.npc.dialog.entry;

import java.util.List;
import me.nakilex.levelplugin.npc.dialog.messenger.DialogueMessenger;
import me.nakilex.levelplugin.npc.dialog.model.DialogueCriteria;
import me.nakilex.levelplugin.npc.dialog.model.DialogueModifier;
import me.nakilex.levelplugin.npc.dialog.model.DialogueTrigger;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;
import org.bukkit.entity.Player;

public interface DialogueEntry {
    String getId();

    String getName();

    DialogueMessenger createMessenger(Player player, InteractionContext context);

    default List<DialogueCriteria> getCriteria() {
        return List.of();
    }

    default boolean matches(InteractionContext context) {
        return getCriteria().stream().allMatch(criteria -> criteria.matches(context));
    }

    default int getPriority() {
        return 0;
    }

    default List<DialogueTrigger> getTriggers() {
        return List.of();
    }

    default List<DialogueModifier> getModifiers() {
        return List.of();
    }
}
