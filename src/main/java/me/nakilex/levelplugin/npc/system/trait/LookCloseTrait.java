package me.nakilex.levelplugin.npc.system.trait;

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
}
