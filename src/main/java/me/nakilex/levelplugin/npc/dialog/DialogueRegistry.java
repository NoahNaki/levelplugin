package me.nakilex.levelplugin.npc.dialog;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.npc.dialog.model.DialogNpcRef;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;
import org.bukkit.entity.Player;

/** Registry for progressively migrating NPCs to criteria-driven dialogue definitions. */
public final class DialogueRegistry {
    private final Main plugin;
    private final Map<String, DialogueDefinition> definitions = new LinkedHashMap<>();

    public DialogueRegistry(Main plugin) { this.plugin = plugin; }

    public void register(DialogueDefinition definition) { definitions.put(definition.id(), definition); }

    public Optional<DialogueDefinition> findBest(Player player, DialogNpcRef npc) {
        InteractionContext context = new InteractionContext(plugin, player, npc, null, () -> { });
        return definitions.values().stream()
                .filter(definition -> definition.npc().matches(npc))
                .filter(definition -> definition.matches(context))
                .sorted((left, right) -> Integer.compare(right.priority(), left.priority()))
                .findFirst();
    }
}
