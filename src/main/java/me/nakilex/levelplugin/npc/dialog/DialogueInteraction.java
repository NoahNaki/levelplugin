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

/** Owns the mutable runtime state for one player's linear or graph-routed dialogue. */
public final class DialogueInteraction {
    private final Main plugin;
    private final Player player;
    private final DialogNpcRef npc;
    private final Quest quest;
    private final InteractionContext context;
    private final DialogueDefinition definition;
    private final Consumer<DialogueEntry> entryStarted;
    private DialogueEntry currentEntry;
    private DialogueMessenger currentMessenger;
    private String currentEntryId;
    private String requestedNextEntryId;
    private boolean endRequested;
    private boolean active;
    private boolean paused;
    private boolean finished;
    private boolean cancelled;

    public DialogueInteraction(Main plugin, Player player, DialogNpcRef npc, Quest quest,
                               List<DialogueEntry> entries, Runnable finish,
                               Consumer<DialogueEntry> entryStarted) {
        this(plugin, player, npc, quest,
                new DialogueDefinition("legacy:" + player.getUniqueId(), "Legacy dialogue", npc, 0, List.of(), entries),
                finish, entryStarted);
    }

    public DialogueInteraction(Main plugin, Player player, DialogNpcRef npc, Quest quest,
                               DialogueDefinition definition, Runnable finish,
                               Consumer<DialogueEntry> entryStarted) {
        this.plugin = plugin;
        this.player = player;
        this.npc = npc;
        this.quest = quest;
        this.definition = definition;
        this.context = new InteractionContext(plugin, player, npc, quest, finish);
        this.context.attachInteraction(this);
        this.entryStarted = entryStarted == null ? entry -> { } : entryStarted;
    }

    public void start() {
        if (active || finished || cancelled) return;
        active = true;
        debug("starting dialogue definition '" + definition.id() + "'");
        next();
    }

    public void tick(Duration deltaTime) {
        if (!active || paused || currentMessenger == null || currentMessenger.isFinished()) return;
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
        String completedEntryId = completeCurrentEntry();
        if (endRequested) { finish(); return; }
        String nextEntryId = consumeRequestedEntryId();
        if (nextEntryId == null) {
            nextEntryId = completedEntryId == null ? definition.startEntryId()
                    : definition.getEntryAfter(completedEntryId).flatMap(definition::getEntryId).orElse(null);
        }
        startNextMatchingEntry(nextEntryId);
    }

    /** Requests a graph jump after the currently completing entry or option. */
    public void goTo(String entryId) {
        requestedNextEntryId = entryId;
        debug("requested go-to entry '" + entryId + "'");
    }

    /** Requests a safe interaction end after the currently completing entry or option. */
    public void requestEnd() {
        endRequested = true;
        debug("requested interaction end");
    }

    /** Explicitly requests the ordered successor used by backwards-compatible linear dialogues. */
    public void nextLinear() {
        requestedNextEntryId = currentEntryId == null ? definition.startEntryId()
                : definition.getEntryAfter(currentEntryId).flatMap(definition::getEntryId).orElse(null);
        if (requestedNextEntryId == null) requestEnd();
    }

    public void executeTrigger(DialogueTrigger trigger) {
        debug("executing trigger " + trigger.getClass().getSimpleName());
        trigger.execute(this, context);
    }

    private void startNextMatchingEntry(String nextEntryId) {
        while (nextEntryId != null) {
            DialogueEntry candidate = definition.getEntry(nextEntryId).orElse(null);
            if (candidate == null) {
                plugin.getLogger().warning("[DialogDebug] dialogue '" + definition.id()
                        + "' requested missing entry '" + nextEntryId + "'; ending safely");
                requestEnd();
                finish();
                return;
            }
            if (!candidate.matches(context)) {
                nextEntryId = definition.getEntryAfter(nextEntryId).flatMap(definition::getEntryId).orElse(null);
                continue;
            }
            currentEntryId = nextEntryId;
            currentEntry = candidate;
            currentMessenger = candidate.createMessenger(player, context);
            debug("starting entry '" + currentEntryId + "'");
            entryStarted.accept(candidate);
            currentMessenger.init();
            if (candidate instanceof OptionDialogueEntry && currentMessenger.isFinished()) {
                String completedEntryId = completeCurrentEntry();
                if (endRequested) { finish(); return; }
                nextEntryId = consumeRequestedEntryId();
                if (nextEntryId == null) {
                    nextEntryId = definition.getEntryAfter(completedEntryId).flatMap(definition::getEntryId).orElse(null);
                }
                continue;
            }
            return;
        }
        finish();
    }

    private String completeCurrentEntry() {
        if (currentEntry == null || currentMessenger == null || !currentMessenger.isFinished()) return null;
        String completedEntryId = currentEntryId;
        debug("finished entry '" + completedEntryId + "'");
        for (DialogueModifier modifier : currentEntry.getModifiers()) modifier.apply(context);
        for (DialogueTrigger trigger : currentEntry.getTriggers()) executeTrigger(trigger);
        currentMessenger.dispose();
        currentMessenger = null;
        currentEntry = null;
        currentEntryId = null;
        return completedEntryId;
    }

    private String consumeRequestedEntryId() {
        String requested = requestedNextEntryId;
        requestedNextEntryId = null;
        return requested;
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
        debug("ending interaction");
        if (currentMessenger != null) currentMessenger.dispose();
        context.finish().run();
    }

    private void debug(String message) {
        if (plugin.getQuestManager() != null && plugin.getQuestManager().isDebug()) {
            plugin.getLogger().info("[DialogDebug] " + message + " player=" + player.getName());
        }
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
