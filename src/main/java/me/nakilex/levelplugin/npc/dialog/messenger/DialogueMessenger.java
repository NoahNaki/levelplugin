package me.nakilex.levelplugin.npc.dialog.messenger;

import me.nakilex.levelplugin.npc.dialog.entry.DialogueEntry;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;
import org.bukkit.entity.Player;

import java.time.Duration;

/** Runtime presenter for a dialogue entry. */
public abstract class DialogueMessenger {
    public enum State { READY, RUNNING, FINISHED, CANCELLED }

    protected final Player player;
    protected final DialogueEntry entry;
    protected final InteractionContext context;
    private State state = State.READY;

    protected DialogueMessenger(Player player, DialogueEntry entry, InteractionContext context) {
        this.player = player;
        this.entry = entry;
        this.context = context;
    }

    public void init() {
        state = State.RUNNING;
    }

    public void tick(Duration deltaTime) {
    }

    public void requestNextOrSkip() {
        finish();
    }

    public void dispose() {
    }

    public void cancel() {
        state = State.CANCELLED;
        dispose();
    }

    protected void finish() {
        if (state != State.CANCELLED) {
            state = State.FINISHED;
        }
    }

    public State state() {
        return state;
    }

    public boolean isFinished() {
        return state == State.FINISHED;
    }

    public boolean isCancelled() {
        return state == State.CANCELLED;
    }
}
