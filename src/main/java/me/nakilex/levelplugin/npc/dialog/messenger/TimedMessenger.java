package me.nakilex.levelplugin.npc.dialog.messenger;

import java.time.Duration;
import me.nakilex.levelplugin.npc.dialog.PlaceholderResolver;
import me.nakilex.levelplugin.npc.dialog.entry.TimedDialogueEntry;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Messenger that reveals a message over time and then finishes after a wait period. */
public final class TimedMessenger extends DialogueMessenger {
    private final TimedDialogueEntry timedEntry;
    private Duration playTime = Duration.ZERO;
    private int lastVisibleCharacters = -1;

    public TimedMessenger(Player player, TimedDialogueEntry entry, InteractionContext context) {
        super(player, entry, context);
        this.timedEntry = entry;
    }

    @Override public void init() { super.init(); render(0); }

    @Override public void tick(Duration deltaTime) {
        if (state() != State.RUNNING) return;
        playTime = playTime.plus(deltaTime == null ? Duration.ZERO : deltaTime);
        long typingMs = Math.max(0L, timedEntry.typingDuration().toMillis());
        long elapsedMs = playTime.toMillis();
        String message = resolvedMessage();
        int visible = typingMs <= 0L ? message.length()
                : (int) Math.min(message.length(), Math.floor(message.length() * (elapsedMs / (double) typingMs)));
        render(visible);
        if (elapsedMs >= typingMs + Math.max(0L, timedEntry.waitDuration().toMillis())) finish();
    }

    @Override public void requestNextOrSkip() {
        if (timedEntry.allowSkip() && lastVisibleCharacters < resolvedMessage().length()) {
            render(resolvedMessage().length());
            playTime = timedEntry.typingDuration();
            return;
        }
        finish();
    }

    private void render(int visibleCharacters) {
        String message = resolvedMessage();
        int safeVisible = Math.max(0, Math.min(message.length(), visibleCharacters));
        if (safeVisible == lastVisibleCharacters) return;
        lastVisibleCharacters = safeVisible;
        String speaker = timedEntry.speakerEntry() == null ? timedEntry.speaker() : timedEntry.speakerEntry().displayName();
        if (speaker == null || speaker.isBlank()) speaker = context.npc() == null ? "NPC" : context.npc().name();
        player.sendMessage(ChatColor.YELLOW + PlaceholderResolver.resolve(speaker, context)
                + ChatColor.WHITE + ": " + message.substring(0, safeVisible));
        player.playSound(player.getLocation(), timedEntry.speakerEntry() == null || timedEntry.speakerEntry().sound() == null
                ? Sound.UI_BUTTON_CLICK : timedEntry.speakerEntry().sound(), 0.5f, 1.4f);
    }

    private String resolvedMessage() { return PlaceholderResolver.resolve(timedEntry.message(), context); }
}
