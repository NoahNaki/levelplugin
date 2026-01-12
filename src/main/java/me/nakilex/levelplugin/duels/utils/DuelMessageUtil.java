package me.nakilex.levelplugin.duels.utils;

import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Utility methods for sending duel related chat components.
 */
public final class DuelMessageUtil {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private DuelMessageUtil() {}

    /**
     * Send a duel request prompt to the target and notify the requester.
     *
     * @param requester the player initiating the duel
     * @param target    the player receiving the request
     */
    public static void sendRequest(Player requester, Player target) {
        // Inform the target with centered text and clickable accept/decline buttons
        ChatFormatter.sendCenteredMessage(target,
            ChatColor.YELLOW + requester.getName() + " has challenged you! Click below:");

        Component acceptBtn = LEGACY.deserialize("\u00a7a\u00a7l[ACCEPT]")
                .clickEvent(ClickEvent.runCommand("/duel accept"))
                .hoverEvent(HoverEvent.showText(Component.text("Click to accept the duel")));

        Component declineBtn = LEGACY.deserialize(" \u00a7c\u00a7l[DECLINE]")
                .clickEvent(ClickEvent.runCommand("/duel decline"))
                .hoverEvent(HoverEvent.showText(Component.text("Click to decline the duel")));

        Component finalMessage = Component.text("                     ")
                .append(acceptBtn)
                .append(Component.text("   "))
                .append(declineBtn);
        target.sendMessage(finalMessage);

        // Notify the requester that the request was sent
        ChatMessageUtil.send(requester, ChatMessageUtil.MessageType.SUCCESS,
            "Duel request sent to " + target.getName() + "!");
    }
}
