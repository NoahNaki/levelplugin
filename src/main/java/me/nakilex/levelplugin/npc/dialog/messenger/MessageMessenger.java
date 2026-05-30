package me.nakilex.levelplugin.npc.dialog.messenger;

import java.time.Duration;
import me.nakilex.levelplugin.npc.dialog.PlaceholderResolver;
import me.nakilex.levelplugin.npc.dialog.display.DialogueDisplay;
import me.nakilex.levelplugin.npc.dialog.display.DialogueDisplays;
import me.nakilex.levelplugin.npc.dialog.display.DialogueFrame;
import me.nakilex.levelplugin.npc.dialog.entry.MessageDialogueEntry;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;
import me.nakilex.levelplugin.npc.dialog.model.SpeakerEntry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Player-advanced dialogue messenger that renders replaceable animated frames instead of raw partial chat lines. */
public final class MessageMessenger extends DialogueMessenger {
    private static final int RENDER_INTERVAL_TICKS = 2;
    private final MessageDialogueEntry messageEntry;
    private final TypingAnimation typingAnimation;
    private final DialogueDisplay display = DialogueDisplays.defaultDisplay();
    private String resolvedSpeaker;
    private String resolvedText;
    private int ticksSinceRender;
    private int lastVisibleCharacters = -1;
    private boolean renderedFinalLine;

    public MessageMessenger(Player player, MessageDialogueEntry entry, InteractionContext context) {
        super(player, entry, context);
        this.messageEntry = entry;
        this.typingAnimation = new TypingAnimation(entry.typingDuration());
    }

    @Override
    public void init() {
        super.init();
        resolvedSpeaker = resolveSpeaker();
        resolvedText = resolveText();
        if (!messageEntry.animated()) typingAnimation.complete();
        renderCurrentFrame();
    }

    @Override
    public void tick(Duration deltaTime) {
        if (state() != State.RUNNING || isAnimationComplete()) return;
        typingAnimation.tick(deltaTime);
        ticksSinceRender++;
        if (ticksSinceRender >= RENDER_INTERVAL_TICKS || isAnimationComplete()) renderCurrentFrame();
    }

    @Override
    public void requestNextOrSkip() {
        if (!isAnimationComplete()) {
            if (!messageEntry.allowSkip()) return;
            typingAnimation.complete();
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
        boolean complete = isAnimationComplete();
        display.show(player, new DialogueFrame(resolvedSpeaker, resolvedText.substring(0, visibleCharacters),
                messageEntry.index(), messageEntry.total(), complete));
        if (complete && !renderedFinalLine) {
            renderedFinalLine = true;
            SpeakerEntry speakerEntry = messageEntry.speaker();
            player.playSound(player.getLocation(), speakerEntry == null || speakerEntry.sound() == null
                    ? Sound.UI_BUTTON_CLICK : speakerEntry.sound(), 1f, 1f);
        }
    }

    private boolean isAnimationComplete() { return typingAnimation.isComplete(resolvedText); }

    private String resolveSpeaker() {
        SpeakerEntry speakerEntry = messageEntry.speaker();
        if (speakerEntry != null) return PlaceholderResolver.resolve(speakerEntry.displayName(), context);
        String raw = messageEntry.line();
        int bar = raw.indexOf('|');
        String speaker = bar >= 0 ? raw.substring(0, bar) : context.npc() == null ? "NPC" : context.npc().name();
        return PlaceholderResolver.resolve(speaker, context);
    }

    private String resolveText() {
        String raw = messageEntry.line();
        int bar = messageEntry.speaker() == null ? raw.indexOf('|') : -1;
        return PlaceholderResolver.resolve(bar >= 0 ? raw.substring(bar + 1) : raw, context);
    }
}
