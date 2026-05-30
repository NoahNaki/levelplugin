package me.nakilex.levelplugin.npc.dialog.messenger;

import java.time.Duration;
import me.nakilex.levelplugin.npc.dialog.PlaceholderResolver;
import me.nakilex.levelplugin.npc.dialog.display.DialogueDisplay;
import me.nakilex.levelplugin.npc.dialog.display.DialogueDisplays;
import me.nakilex.levelplugin.npc.dialog.display.DialogueFrame;
import me.nakilex.levelplugin.npc.dialog.entry.TimedDialogueEntry;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Auto-advancing messenger that renders replaceable animated frames and waits for its configured duration. */
public final class TimedMessenger extends DialogueMessenger {
    private static final int RENDER_INTERVAL_TICKS = 2;
    private final TimedDialogueEntry timedEntry;
    private final TypingAnimation typingAnimation;
    private final DialogueDisplay display = DialogueDisplays.defaultDisplay();
    private Duration playTime = Duration.ZERO;
    private String resolvedSpeaker;
    private String resolvedText;
    private int ticksSinceRender;
    private int lastVisibleCharacters = -1;

    public TimedMessenger(Player player, TimedDialogueEntry entry, InteractionContext context) {
        super(player, entry, context);
        this.timedEntry = entry;
        this.typingAnimation = new TypingAnimation(entry.typingDuration());
    }

    @Override
    public void init() {
        super.init();
        resolvedSpeaker = resolveSpeaker();
        resolvedText = PlaceholderResolver.resolve(timedEntry.message(), context);
        renderCurrentFrame();
    }

    @Override public void tick(Duration deltaTime) {
        if (state() != State.RUNNING) return;
        Duration safeDelta = deltaTime == null || deltaTime.isNegative() ? Duration.ZERO : deltaTime;
        playTime = playTime.plus(safeDelta);
        typingAnimation.tick(safeDelta);
        ticksSinceRender++;
        if (ticksSinceRender >= RENDER_INTERVAL_TICKS || typingAnimation.isComplete(resolvedText)) renderCurrentFrame();
        if (playTime.toMillis() >= Math.max(0L, timedEntry.typingDuration().toMillis())
                + Math.max(0L, timedEntry.waitDuration().toMillis())) finish();
    }

    @Override public void requestNextOrSkip() {
        if (timedEntry.allowSkip() && !typingAnimation.isComplete(resolvedText)) {
            typingAnimation.complete();
            playTime = typingAnimation.duration();
            renderCurrentFrame();
            return;
        }
        finish();
    }

    @Override public void dispose() { display.clear(player); }

    private void renderCurrentFrame() {
        ticksSinceRender = 0;
        int visibleCharacters = typingAnimation.visibleCharacters(resolvedText);
        if (visibleCharacters == lastVisibleCharacters) return;
        lastVisibleCharacters = visibleCharacters;
        display.show(player, new DialogueFrame(resolvedSpeaker, resolvedText.substring(0, visibleCharacters),
                0, 1, typingAnimation.isComplete(resolvedText)));
        player.playSound(player.getLocation(), timedEntry.speakerEntry() == null || timedEntry.speakerEntry().sound() == null
                ? Sound.UI_BUTTON_CLICK : timedEntry.speakerEntry().sound(), 0.5f, 1.4f);
    }

    private String resolveSpeaker() {
        String speaker = timedEntry.speakerEntry() == null ? timedEntry.speaker() : timedEntry.speakerEntry().displayName();
        if (speaker == null || speaker.isBlank()) speaker = context.npc() == null ? "NPC" : context.npc().name();
        return PlaceholderResolver.resolve(speaker, context);
    }
}
