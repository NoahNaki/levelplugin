package me.nakilex.levelplugin.dialogue.render;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Isolates dialogue actionbar delivery so it can later move from Bukkit API to packet-level sending.
 */
public class DialogueActionBarSender {
    public void send(Player player, Component component) {
        if (player == null || component == null) {
            return;
        }
        player.sendActionBar(component);
    }

    public void clear(Player player) {
        if (player == null) {
            return;
        }
        player.sendActionBar(Component.empty());
    }
}
