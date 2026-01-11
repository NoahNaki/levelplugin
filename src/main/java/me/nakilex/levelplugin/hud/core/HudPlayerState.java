package me.nakilex.levelplugin.hud.core;

public class HudPlayerState {
    private boolean enabled = true;
    private boolean debugMode = false;
    private long lastDebugLogMs = 0L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public long getLastDebugLogMs() {
        return lastDebugLogMs;
    }

    public void setLastDebugLogMs(long lastDebugLogMs) {
        this.lastDebugLogMs = lastDebugLogMs;
    }
}
