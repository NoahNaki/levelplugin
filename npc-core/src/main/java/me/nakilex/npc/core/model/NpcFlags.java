package me.nakilex.npc.core.model;

public class NpcFlags {
    private boolean invulnerable = true;
    private boolean pushable = false;
    private boolean collidable = false;
    private boolean visible = true;
    private boolean tablist = false;
    private boolean glowing = false;

    public boolean isInvulnerable() {
        return invulnerable;
    }

    public void setInvulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
    }

    public boolean isPushable() {
        return pushable;
    }

    public void setPushable(boolean pushable) {
        this.pushable = pushable;
    }

    public boolean isCollidable() {
        return collidable;
    }

    public void setCollidable(boolean collidable) {
        this.collidable = collidable;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isTablist() {
        return tablist;
    }

    public void setTablist(boolean tablist) {
        this.tablist = tablist;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
    }
}
