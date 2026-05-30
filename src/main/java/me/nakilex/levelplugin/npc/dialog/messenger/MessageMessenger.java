package me.nakilex.levelplugin.npc.dialog.messenger;

import me.nakilex.levelplugin.npc.dialog.entry.MessageDialogueEntry;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;
import me.nakilex.levelplugin.utils.ChatFormatter;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;

/** Messenger for immediate one-line dialogue messages. */
public final class MessageMessenger extends DialogueMessenger {
    private final MessageDialogueEntry messageEntry;

    public MessageMessenger(Player player, MessageDialogueEntry entry, InteractionContext context) {
        super(player, entry, context);
        this.messageEntry = entry;
    }

    @Override
    public void init() {
        super.init();
        sendMessageLine();
        finish();
    }

    private void sendMessageLine() {
        String raw = messageEntry.rawLine();
        String speaker = context.npc() != null ? context.npc().name() : "NPC";
        String line = raw;
        int bar = raw.indexOf('|');
        if (bar >= 0) {
            speaker = raw.substring(0, bar);
            line = raw.substring(bar + 1);
            if ("<player>".equalsIgnoreCase(speaker)) {
                speaker = player.getName();
            }
        }
        line = line.replaceAll("(?i)<player>", Matcher.quoteReplacement(player.getName()));

        if (messageEntry.index() == 0) {
            ChatFormatter.constructDivider(player, " ", 45);
        }
        String msg = ChatColor.DARK_GRAY + "[" + ChatColor.GRAY + (messageEntry.index() + 1)
                + "/" + messageEntry.total() + ChatColor.DARK_GRAY + "] "
                + ChatColor.YELLOW + speaker
                + ChatColor.WHITE + ": " + line;
        player.sendMessage(msg);
        ChatFormatter.constructDivider(player, " ", 45);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }
}
