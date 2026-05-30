package me.nakilex.levelplugin.npc.dialog.display;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/** Clean Bukkit display strategy: update the replaceable action bar instead of appending partial chat lines. */
public final class ActionBarDialogueDisplay implements DialogueDisplay {
    private final DialogueChatHistory history;

    public ActionBarDialogueDisplay(DialogueChatHistory history) { this.history = history; }

    @Override
    public void show(Player player, DialogueFrame frame) {
        if (player == null || frame == null) return;
        player.sendActionBar(Component.text(history.update(player, frame).composeMessage()));
    }

    @Override
    public void clear(Player player) {
        if (player == null) return;
        history.clear(player);
        player.sendActionBar(Component.empty());
    }

    @Override public boolean isActive(Player player) { return history.isActive(player); }
}
