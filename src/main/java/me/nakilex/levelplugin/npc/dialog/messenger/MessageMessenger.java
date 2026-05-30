package me.nakilex.levelplugin.npc.dialog.messenger;

import me.nakilex.levelplugin.npc.dialog.PlaceholderResolver;
import me.nakilex.levelplugin.npc.dialog.entry.MessageDialogueEntry;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;
import me.nakilex.levelplugin.npc.dialog.model.SpeakerEntry;
import me.nakilex.levelplugin.utils.ChatFormatter;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

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
        String raw = messageEntry.line();
        SpeakerEntry speakerEntry = messageEntry.speaker();
        String speaker = speakerEntry == null
                ? (context.npc() == null ? "NPC" : context.npc().name())
                : speakerEntry.displayName();
        String line = raw;
        int bar = raw.indexOf('|');
        if (speakerEntry == null && bar >= 0) {
            speaker = raw.substring(0, bar);
            line = raw.substring(bar + 1);
        }
        speaker = PlaceholderResolver.resolve(speaker, context);
        line = PlaceholderResolver.resolve(line, context);

        if (messageEntry.index() == 0) ChatFormatter.constructDivider(player, " ", 45);
        ChatColor speakerColor = speakerEntry == null ? ChatColor.YELLOW : speakerEntry.color();
        player.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GRAY + (messageEntry.index() + 1)
                + "/" + messageEntry.total() + ChatColor.DARK_GRAY + "] "
                + speakerColor + speaker + ChatColor.WHITE + ": " + line);
        ChatFormatter.constructDivider(player, " ", 45);
        player.playSound(player.getLocation(), speakerEntry == null || speakerEntry.sound() == null
                ? Sound.UI_BUTTON_CLICK : speakerEntry.sound(), 1f, 1f);
    }
}
