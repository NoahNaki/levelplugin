package me.nakilex.levelplugin.duels.utils;

import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Utility methods for sending duel related chat components.
 */
public final class DuelMessageUtil {
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

        TextComponent acceptBtn = new TextComponent("\u00a7a\u00a7l[ACCEPT]");
        acceptBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel accept"));
        acceptBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new ComponentBuilder("Click to accept the duel").create()));

        TextComponent declineBtn = new TextComponent(" \u00a7c\u00a7l[DECLINE]");
        declineBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel decline"));
        declineBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new ComponentBuilder("Click to decline the duel").create()));

        TextComponent finalMessage = new TextComponent("                     ");
        finalMessage.addExtra(acceptBtn);
        finalMessage.addExtra("   ");
        finalMessage.addExtra(declineBtn);
        target.spigot().sendMessage(finalMessage);

        // Notify the requester that the request was sent
        ChatMessageUtil.send(requester, ChatMessageUtil.MessageType.SUCCESS,
            "Duel request sent to " + target.getName() + "!");
    }
}

