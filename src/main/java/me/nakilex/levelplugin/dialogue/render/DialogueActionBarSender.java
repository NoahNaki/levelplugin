package me.nakilex.levelplugin.dialogue.render;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

/**
 * Isolates dialogue actionbar delivery so it can later move from Bukkit API to packet-level sending.
 */
public class DialogueActionBarSender {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public void send(Player player, Component component) {
        if (player == null || component == null) {
            return;
        }
        player.sendActionBar(component);
    }

    public void sendMiniMessage(Player player, String miniMessage) {
        if (player == null || miniMessage == null) {
            return;
        }
        player.sendActionBar(MINI_MESSAGE.deserialize(miniMessage));
    }

    public void clear(Player player) {
        if (player == null) {
            return;
        }
        player.sendActionBar(Component.empty());
    }
}
