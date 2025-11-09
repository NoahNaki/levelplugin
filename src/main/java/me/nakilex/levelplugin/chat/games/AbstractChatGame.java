package me.nakilex.levelplugin.chat.games;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.entity.Player;

/**
 * Base implementation that handles lifecycle toggles for chat games.
 */
public abstract class AbstractChatGame implements ChatGame {

    private final String id;
    private final String displayName;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile boolean enabled = true;

    protected AbstractChatGame(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean canPlay() {
        return true;
    }

    @Override
    public final void start(ChatGameManager manager) {
        if (!enabled) {
            return;
        }
        if (running.compareAndSet(false, true)) {
            onStart(manager);
        }
    }

    @Override
    public final void stop(ChatGameManager manager) {
        if (running.compareAndSet(true, false)) {
            onStop(manager);
        }
    }

    @Override
    public final Optional<ChatGameResult> handleChat(Player player, String message) {
        if (!enabled || !isRunning()) {
            return Optional.empty();
        }
        return onChat(player, message);
    }

    /** Called when the game transitions to an active state. */
    protected abstract void onStart(ChatGameManager manager);

    /** Called after {@link #stop(ChatGameManager)} succeeds. */
    protected void onStop(ChatGameManager manager) {
        // default no-op
    }

    /** Evaluate incoming chat for a winning answer. */
    protected abstract Optional<ChatGameResult> onChat(Player player, String message);
}
