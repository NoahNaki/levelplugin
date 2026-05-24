package me.nakilex.levelplugin.npc.system.trait;

import me.nakilex.levelplugin.npc.system.NPC;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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
    private int randomLookCooldownTicks;

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
        randomLookCooldownTicks = randomLookDelayTicks;
    }

    @Override
    public void onTick(NPC npc) {
        if (npc == null || npc.getEntity() == null || !enabled || npc.getNavigator().isNavigating() && disableWhileNavigating) {
            return;
        }
        List<Player> targets = new ArrayList<>();
        for (Entity nearby : npc.getEntity().getNearbyEntities(range, range, range)) {
            if (!(nearby instanceof Player player) || player.isInvisible() || !passesFilter(player)) {
                continue;
            }
            if (realisticLooking && npc.getEntity() instanceof LivingEntity living && !living.hasLineOfSight(player)) {
                continue;
            }
            targets.add(player);
        }
        if (targets.isEmpty()) {
            handleRandomLook(npc);
            return;
        }
        targets.sort(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(npc.getEntity().getLocation())));
        Player target = targets.get(0);
        if (randomSwitchTargets && targets.size() > 1 && ThreadLocalRandom.current().nextDouble() < 0.1D) {
            target = targets.get(ThreadLocalRandom.current().nextInt(targets.size()));
        }
        RotationTrait rotation = npc.getOrAddTrait(RotationTrait.class);
        rotation.setHeadOnly(headOnly);
        rotation.setLinkedBody(linkedBody);
        if (perPlayer) {
            for (Player player : targets) {
                rotation.rotateToFace(player.getEyeLocation(), npc);
            }
        } else {
            rotation.rotateToFace(target.getEyeLocation(), npc);
        }
    }

    private void handleRandomLook(NPC npc) {
        if (!randomLookEnabled) {
            return;
        }
        if (randomLookCooldownTicks-- > 0) {
            return;
        }
        randomLookCooldownTicks = Math.max(1, randomLookDelayTicks);
        float yaw = ThreadLocalRandom.current().nextFloat(randomYawMin, randomYawMax);
        float pitch = ThreadLocalRandom.current().nextFloat(randomPitchMin, randomPitchMax);
        npc.getOrAddTrait(RotationTrait.class).rotateTo(yaw, pitch);
    }

    private boolean passesFilter(Player player) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        String lowered = filter.toLowerCase();
        if (lowered.startsWith("name:")) {
            return player.getName().toLowerCase().contains(lowered.substring("name:".length()).trim());
        }
        return true;
    }
}
