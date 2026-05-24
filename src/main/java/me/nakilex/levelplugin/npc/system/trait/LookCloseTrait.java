package me.nakilex.levelplugin.npc.system.trait;

import me.nakilex.levelplugin.npc.system.NPC;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class LookCloseTrait implements NpcTrait {
    private boolean enabled;
    private double range = 5.0;
    private boolean perPlayer;
    private boolean randomLookEnabled;
    private int randomLookDelayTicks = 40;
    private float randomYawMin = -180F;
    private float randomYawMax = 180F;
    private float randomPitchMin = -30F;
    private float randomPitchMax = 30F;
    private boolean randomSwitchTargets;
    private boolean realisticLooking;
    private boolean headOnly;
    private boolean linkedBody = true;
    private boolean disableWhileNavigating;
    private boolean targetNpcs;
    private String filter;

    public void lookClose(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setRange(double range) {
        this.range = range;
    }

    public double getRange() {
        return range;
    }

    public boolean isPerPlayer() {
        return perPlayer;
    }

    public void setPerPlayer(boolean perPlayer) {
        this.perPlayer = perPlayer;
    }

    public boolean isRandomLookEnabled() {
        return randomLookEnabled;
    }

    public void setRandomLookEnabled(boolean randomLookEnabled) {
        this.randomLookEnabled = randomLookEnabled;
    }

    public int getRandomLookDelayTicks() {
        return randomLookDelayTicks;
    }

    public void setRandomLookDelayTicks(int randomLookDelayTicks) {
        this.randomLookDelayTicks = Math.max(1, randomLookDelayTicks);
    }

    public float getRandomYawMin() {
        return randomYawMin;
    }

    public float getRandomYawMax() {
        return randomYawMax;
    }

    public void setRandomYawRange(float min, float max) {
        this.randomYawMin = Math.min(min, max);
        this.randomYawMax = Math.max(min, max);
    }

    public float getRandomPitchMin() {
        return randomPitchMin;
    }

    public float getRandomPitchMax() {
        return randomPitchMax;
    }

    public void setRandomPitchRange(float min, float max) {
        this.randomPitchMin = Math.min(min, max);
        this.randomPitchMax = Math.max(min, max);
    }

    public boolean isRandomSwitchTargets() {
        return randomSwitchTargets;
    }

    public void setRandomSwitchTargets(boolean randomSwitchTargets) {
        this.randomSwitchTargets = randomSwitchTargets;
    }

    public boolean isRealisticLooking() {
        return realisticLooking;
    }

    public void setRealisticLooking(boolean realisticLooking) {
        this.realisticLooking = realisticLooking;
    }

    public boolean isHeadOnly() {
        return headOnly;
    }

    public void setHeadOnly(boolean headOnly) {
        this.headOnly = headOnly;
    }

    public boolean isLinkedBody() {
        return linkedBody;
    }

    public void setLinkedBody(boolean linkedBody) {
        this.linkedBody = linkedBody;
    }

    public boolean isDisableWhileNavigating() {
        return disableWhileNavigating;
    }

    public void setDisableWhileNavigating(boolean disableWhileNavigating) {
        this.disableWhileNavigating = disableWhileNavigating;
    }

    public boolean isTargetNpcs() {
        return targetNpcs;
    }

    public void setTargetNpcs(boolean targetNpcs) {
        this.targetNpcs = targetNpcs;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter == null || filter.isBlank() ? null : filter.trim();
    }

    @Override
    public void onSpawn(NPC npc) {
        applyToCitizens(npc);
    }

    @Override
    public void onTick(NPC npc) {
        if (npc == null || npc.getEntity() == null || !enabled || npc.getNavigator().isNavigating() && disableWhileNavigating) {
            return;
        }
        if (npc.getCitizensNpc() != null) {
            applyToCitizens(npc);
            return;
        }
        Player closest = null;
        double bestDistanceSq = range * range;
        for (Entity nearby : npc.getEntity().getNearbyEntities(range, range, range)) {
            if (!(nearby instanceof Player player) || player.isInvisible()) {
                continue;
            }
            double distanceSq = player.getLocation().distanceSquared(npc.getEntity().getLocation());
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                closest = player;
            }
        }
        if (closest == null) {
            return;
        }
        RotationTrait rotation = npc.getOrAddTrait(RotationTrait.class);
        rotation.rotateToFace(closest.getEyeLocation(), npc);
    }

    private void applyToCitizens(NPC npc) {
        if (npc == null || npc.getCitizensNpc() == null) {
            return;
        }
        net.citizensnpcs.trait.LookClose trait = npc.getCitizensNpc().getOrAddTrait(net.citizensnpcs.trait.LookClose.class);
        trait.lookClose(enabled);
        trait.setRange(range);
        trait.setPerPlayer(perPlayer);
        trait.setRandomLook(randomLookEnabled);
        trait.setRandomlySwitchTargets(randomSwitchTargets);
        trait.setHeadOnly(headOnly);
        trait.setLinkedBody(linkedBody);
        trait.setDisableWhileNavigating(disableWhileNavigating);
        trait.setTargetNPCs(targetNpcs);
        // Citizens LookClose filter API differs across versions; keep local filter stored
        // and apply only where available in this plugin's own tick fallback.
    }
}
