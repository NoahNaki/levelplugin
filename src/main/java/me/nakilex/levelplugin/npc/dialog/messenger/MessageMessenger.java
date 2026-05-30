package me.nakilex.levelplugin.npc.dialog.messenger;

import java.time.Duration;
import me.nakilex.levelplugin.npc.dialog.PlaceholderResolver;
import me.nakilex.levelplugin.npc.dialog.entry.MessageDialogueEntry;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;
import me.nakilex.levelplugin.npc.dialog.model.SpeakerEntry;
import me.nakilex.levelplugin.utils.ChatFormatter;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Player-advanced dialogue messenger that reveals normal NPC text gradually by default. */
public final class MessageMessenger extends DialogueMessenger {
    private static final int RENDER_INTERVAL_TICKS = 3;
    private final MessageDialogueEntry messageEntry;
    private final TypingAnimation typingAnimation;
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
        if (messageEntry.index() == 0) ChatFormatter.constructDivider(player, " ", 45);
        if (!messageEntry.animated()) typingAnimation.complete();
        if (isAnimationComplete()) renderCurrentText();
    }

    @Override
    public void tick(Duration deltaTime) {
        if (state() != State.RUNNING || isAnimationComplete()) return;
        typingAnimation.tick(deltaTime);
        ticksSinceRender++;
        if (ticksSinceRender >= RENDER_INTERVAL_TICKS || isAnimationComplete()) renderCurrentText();
    }

    @Override
    public void requestNextOrSkip() {
        if (!isAnimationComplete()) {
            if (!messageEntry.allowSkip()) return;
            typingAnimation.complete();
            renderCurrentText();
            return;
        }
        finish();
    }

    private void renderCurrentText() {
        ticksSinceRender = 0;
        String line = resolvedLine();
        int visibleCharacters = typingAnimation.visibleCharacters(line);
        if (visibleCharacters == lastVisibleCharacters) return;
        lastVisibleCharacters = visibleCharacters;
        SpeakerEntry speakerEntry = messageEntry.speaker();
        String speaker = speakerEntry == null ? resolvedLegacySpeaker() : PlaceholderResolver.resolve(speakerEntry.displayName(), context);
        ChatColor speakerColor = speakerEntry == null ? ChatColor.YELLOW : speakerEntry.color();
        player.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GRAY + (messageEntry.index() + 1)
                + "/" + messageEntry.total() + ChatColor.DARK_GRAY + "] "
                + speakerColor + speaker + ChatColor.WHITE + ": " + line.substring(0, visibleCharacters));
        if (isAnimationComplete() && !renderedFinalLine) {
            renderedFinalLine = true;
            ChatFormatter.constructDivider(player, " ", 45);
            player.playSound(player.getLocation(), speakerEntry == null || speakerEntry.sound() == null
                    ? Sound.UI_BUTTON_CLICK : speakerEntry.sound(), 1f, 1f);
        }
    }

    private boolean isAnimationComplete() { return typingAnimation.isComplete(resolvedLine()); }

    private String resolvedLegacySpeaker() {
        String raw = messageEntry.line();
        int bar = raw.indexOf('|');
        String speaker = bar >= 0 ? raw.substring(0, bar) : context.npc() == null ? "NPC" : context.npc().name();
        return PlaceholderResolver.resolve(speaker, context);
    }

    private String resolvedLine() {
        String raw = messageEntry.line();
        int bar = messageEntry.speaker() == null ? raw.indexOf('|') : -1;
        return PlaceholderResolver.resolve(bar >= 0 ? raw.substring(bar + 1) : raw, context);
    }
}
