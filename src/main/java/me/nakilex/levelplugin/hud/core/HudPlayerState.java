package me.nakilex.levelplugin.hud.core;

public class HudPlayerState {
    private String lastRendered = "";

    public String getLastRendered() {
        return lastRendered;
    }

    public void setLastRendered(String lastRendered) {
        this.lastRendered = lastRendered == null ? "" : lastRendered;
    }
}
