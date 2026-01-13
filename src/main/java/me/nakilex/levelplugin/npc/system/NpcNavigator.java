package me.nakilex.levelplugin.npc.system;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

public class NpcNavigator {
    private final NPC npc;
    private final NpcNavigationParameters parameters = new NpcNavigationParameters();
    private boolean navigating;

    NpcNavigator(NPC npc) {
        this.npc = npc;
    }

    public void cancelNavigation() {
        navigating = false;
    }

    public boolean isNavigating() {
        return navigating;
    }

    public NpcNavigationParameters getDefaultParameters() {
        return parameters;
    }

    public void setTarget(Location location) {
        if (npc.getEntity() == null || location == null) {
            navigating = false;
            return;
        }
        navigating = true;
        npc.getEntity().teleport(location);
        navigating = false;
    }

    public void setTarget(Entity target, boolean aggressive) {
        if (target == null) {
            navigating = false;
            return;
        }
        setTarget(target.getLocation());
    }

    public void setTarget(Entity target) {
        setTarget(target, false);
    }
}
