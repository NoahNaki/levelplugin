package me.nakilex.levelplugin.npc.dialog.messenger;

import java.time.Duration;
import me.nakilex.levelplugin.npc.dialog.PlaceholderResolver;
import me.nakilex.levelplugin.npc.dialog.entry.TimedDialogueEntry;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Auto-advancing messenger that reveals text and then waits for its configured duration. */
public final class TimedMessenger extends DialogueMessenger {
    private static final int RENDER_INTERVAL_TICKS = 3;
    private final TimedDialogueEntry timedEntry;
    private final TypingAnimation typingAnimation;
    private Duration playTime = Duration.ZERO;
    private int ticksSinceRender;
    private int lastVisibleCharacters = -1;

    public TimedMessenger(Player player, TimedDialogueEntry entry, InteractionContext context) {
        super(player, entry, context);
        this.timedEntry = entry;
        this.typingAnimation = new TypingAnimation(entry.typingDuration());
    }

    @Override public void init() { super.init(); render(0); }

    @Override public void tick(Duration deltaTime) {
        if (state() != State.RUNNING) return;
        Duration safeDelta = deltaTime == null || deltaTime.isNegative() ? Duration.ZERO : deltaTime;
        playTime = playTime.plus(safeDelta);
        typingAnimation.tick(safeDelta);
        ticksSinceRender++;
        if (ticksSinceRender >= RENDER_INTERVAL_TICKS || typingAnimation.isComplete(resolvedMessage())) {
            render(typingAnimation.visibleCharacters(resolvedMessage()));
        }
        if (playTime.toMillis() >= Math.max(0L, timedEntry.typingDuration().toMillis())
                + Math.max(0L, timedEntry.waitDuration().toMillis())) finish();
    }

    @Override public void requestNextOrSkip() {
        if (timedEntry.allowSkip() && !typingAnimation.isComplete(resolvedMessage())) {
            typingAnimation.complete();
            playTime = typingAnimation.duration();
            render(resolvedMessage().length());
            return;
        }
        finish();
    }

    private void render(int visibleCharacters) {
        ticksSinceRender = 0;
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
