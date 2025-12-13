package me.nakilex.levelplugin.mob.managers;

/**
 * Tracks which players have damage‑chat toggled ON.
 */
public class ChatToggleManager extends PlayerToggleManager {
    private static final ChatToggleManager instance = new ChatToggleManager();
    public static ChatToggleManager getInstance() { return instance; }

    private ChatToggleManager() {
        // Default to disabled; players can opt in to combat chat if desired.
        super(false);
    }
}
