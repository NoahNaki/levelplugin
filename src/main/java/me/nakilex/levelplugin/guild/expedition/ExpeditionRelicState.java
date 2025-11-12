package me.nakilex.levelplugin.guild.expedition;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Persistent per-guild state for expedition relic progression. */
public final class ExpeditionRelicState {
    private String targetRelicId;
    private int progress;
    private final Map<String, Integer> contributions = new HashMap<>();
    private boolean pendingLaunch;
    private String activeRelicId;
    private long activeRelicExpiryEpochDay;
    private int maintenanceBufferDays;
    private final Map<String, Integer> maintenanceContributors = new HashMap<>();
    private final Set<String> unlockedRelics = new HashSet<>();
    private long rotationEpoch = Long.MIN_VALUE;

    public static ExpeditionRelicState load(ConfigurationSection section) {
        ExpeditionRelicState state = new ExpeditionRelicState();
        if (section == null) {
            return state;
        }
        state.targetRelicId = section.getString("target", null);
        state.progress = section.getInt("progress", 0);
        state.pendingLaunch = section.getBoolean("pending", false);
        state.activeRelicId = section.getString("active.id", null);
        state.activeRelicExpiryEpochDay = section.getLong("active.expires", 0L);
        state.maintenanceBufferDays = section.getInt("maintenance_buffer", 0);
        state.rotationEpoch = section.getLong("rotation_epoch", Long.MIN_VALUE);
        state.unlockedRelics.addAll(section.getStringList("unlocked"));
        ConfigurationSection contrib = section.getConfigurationSection("contributions");
        if (contrib != null) {
            for (String key : contrib.getKeys(false)) {
                state.contributions.put(key, contrib.getInt(key, 0));
            }
        }
        ConfigurationSection upkeep = section.getConfigurationSection("maintenance");
        if (upkeep != null) {
            for (String key : upkeep.getKeys(false)) {
                state.maintenanceContributors.put(key, upkeep.getInt(key, 0));
            }
        }
        return state;
    }

    public void save(ConfigurationSection section) {
        section.set("target", targetRelicId);
        section.set("progress", progress);
        section.set("pending", pendingLaunch);
        section.set("active.id", activeRelicId);
        section.set("active.expires", activeRelicExpiryEpochDay);
        section.set("maintenance_buffer", maintenanceBufferDays);
        section.set("rotation_epoch", rotationEpoch);
        section.set("unlocked", unlockedRelics.isEmpty() ? null : unlockedRelics.toArray(new String[0]));
        ConfigurationSection contrib = section.createSection("contributions");
        for (Map.Entry<String, Integer> e : contributions.entrySet()) {
            contrib.set(e.getKey(), e.getValue());
        }
        ConfigurationSection upkeep = section.createSection("maintenance");
        for (Map.Entry<String, Integer> e : maintenanceContributors.entrySet()) {
            upkeep.set(e.getKey(), e.getValue());
        }
    }

    public String getTargetRelicId() {
        return targetRelicId;
    }

    public void setTargetRelicId(String targetRelicId) {
        this.targetRelicId = targetRelicId;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, progress);
    }

    public void addProgress(int amount) {
        progress = Math.max(0, progress + amount);
    }

    public Map<String, Integer> getContributions() {
        return contributions;
    }

    public boolean isPendingLaunch() {
        return pendingLaunch;
    }

    public void setPendingLaunch(boolean pendingLaunch) {
        this.pendingLaunch = pendingLaunch;
    }

    public String getActiveRelicId() {
        return activeRelicId;
    }

    public void setActiveRelicId(String activeRelicId) {
        this.activeRelicId = activeRelicId;
    }

    public long getActiveRelicExpiryEpochDay() {
        return activeRelicExpiryEpochDay;
    }

    public void setActiveRelicExpiryEpochDay(long activeRelicExpiryEpochDay) {
        this.activeRelicExpiryEpochDay = activeRelicExpiryEpochDay;
    }

    public int getMaintenanceBufferDays() {
        return maintenanceBufferDays;
    }

    public void setMaintenanceBufferDays(int maintenanceBufferDays) {
        this.maintenanceBufferDays = Math.max(0, maintenanceBufferDays);
    }

    public void addMaintenanceBufferDays(int amount) {
        maintenanceBufferDays = Math.max(0, maintenanceBufferDays + amount);
    }

    public Map<String, Integer> getMaintenanceContributors() {
        return maintenanceContributors;
    }

    public Set<String> getUnlockedRelics() {
        return unlockedRelics;
    }

    public long getRotationEpoch() {
        return rotationEpoch;
    }

    public void setRotationEpoch(long rotationEpoch) {
        this.rotationEpoch = rotationEpoch;
    }
}
