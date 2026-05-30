package me.nakilex.levelplugin.npc.dialog;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.npc.dialog.entry.DialogueEntry;
import me.nakilex.levelplugin.npc.dialog.entry.OptionDialogueEntry;
import me.nakilex.levelplugin.npc.dialog.messenger.DialogueMessenger;
import me.nakilex.levelplugin.npc.dialog.model.DialogNpcRef;
import me.nakilex.levelplugin.npc.dialog.model.DialogueModifier;
import me.nakilex.levelplugin.npc.dialog.model.DialogueTrigger;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;
import me.nakilex.levelplugin.quests.data.Quest;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Owns the mutable runtime state for one player's dialogue chain. */
public final class DialogueInteraction {
    private final Player player;
    private final DialogNpcRef npc;
    private final Quest quest;
    private final InteractionContext context;
    private final List<DialogueEntry> entries;
    private final Consumer<DialogueEntry> entryStarted;
    private DialogueEntry currentEntry;
    private DialogueMessenger currentMessenger;
    private int index;
    private boolean active;
    private boolean paused;
    private boolean finished;
    private boolean cancelled;

    public DialogueInteraction(Main plugin, Player player, DialogNpcRef npc, Quest quest,
                               List<DialogueEntry> entries, Runnable finish,
                               Consumer<DialogueEntry> entryStarted) {
        this.player = player;
        this.npc = npc;
        this.quest = quest;
        this.entries = List.copyOf(entries);
        this.context = new InteractionContext(plugin, player, npc, quest, finish);
        this.entryStarted = entryStarted == null ? entry -> { } : entryStarted;
    }

    public void start() { if (!active && !finished && !cancelled) { active = true; next(); } }

    public void tick(Duration deltaTime) {
        if (!active || paused || currentMessenger == null) return;
        currentMessenger.tick(deltaTime);
        if (currentMessenger.isFinished()) next();
    }

    public void nextOrSkip() {
        if (!active) return;
        if (paused) resume();
        if (currentMessenger == null) { next(); return; }
        if (!currentMessenger.isFinished()) currentMessenger.requestNextOrSkip();
        if (currentMessenger.isFinished()) next();
    }

    public void next() {
        if (!active || paused) return;
        completeCurrentEntry();
        while (index < entries.size()) {
            DialogueEntry candidate = entries.get(index++);
            if (!candidate.matches(context)) continue;
            currentEntry = candidate;
            currentMessenger = candidate.createMessenger(player, context);
            entryStarted.accept(candidate);
            currentMessenger.init();
            if (candidate instanceof OptionDialogueEntry && currentMessenger.isFinished()) {
                completeCurrentEntry();
                continue;
            }
            return;
        }
        finish();
    }

    private void completeCurrentEntry() {
        if (currentEntry == null || currentMessenger == null || !currentMessenger.isFinished()) return;
        for (DialogueModifier modifier : currentEntry.getModifiers()) modifier.apply(context);
        for (DialogueTrigger trigger : currentEntry.getTriggers()) trigger.execute(context);
        currentMessenger.dispose();
        currentMessenger = null;
        currentEntry = null;
    }

    public void cancel() {
        if (!active || finished) return;
        cancelled = true;
        active = false;
        if (currentMessenger != null) currentMessenger.cancel();
    }

    public void pause() { if (active) paused = true; }
    public void resume() { if (active) paused = false; }

    public void finish() {
        if (!active || cancelled || finished) return;
        completeCurrentEntry();
        finished = true;
        active = false;
        if (currentMessenger != null) currentMessenger.dispose();
        context.finish().run();
    }

    public boolean isActive() { return active; }
    public boolean isFinished() { return finished; }
    public boolean isCancelled() { return cancelled; }
    public int getNpcId() { return npc == null ? -1 : npc.id(); }
    public Location getNpcLocation() { return npc == null ? null : npc.location(); }
    public DialogNpcRef npc() { return npc; }
    public InteractionContext context() { return context; }
    public DialogueMessenger currentMessenger() { return currentMessenger; }
}
