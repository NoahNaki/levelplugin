package me.nakilex.levelplugin.mob.custom.spawner;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CustomMobSpawner {
    private final String name;
    private String mobId;
    private String world;
    private String spawnerGroup;
    private double x;
    private double y;
    private double z;
    private double radius;
    private double radiusY;
    private boolean useTimer;
    private int maxMobs;
    private Integer mobLevel;
    private int mobsPerSpawn;
    private int cooldown;
    private int cooldownTimer;
    private int warmup;
    private int warmupTimer;
    private boolean checkForPlayers;
    private double activationRange;
    private double leashRange;
    private boolean healOnLeash;
    private boolean resetThreatOnLeash;
    private boolean showFlames;
    private boolean breakable;
    private boolean fieldBoss;
    private List<String> conditions;
    private final Set<UUID> activeMobs = new HashSet<>();

    public CustomMobSpawner(String name, String mobId, Location location) {
        this.name = name;
        this.mobId = mobId;
        this.world = location.getWorld() != null ? location.getWorld().getName() : "world";
        this.spawnerGroup = "default";
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.radius = 0.0;
        this.radiusY = 0.0;
        this.useTimer = true;
        this.maxMobs = 1;
        this.mobLevel = null;
        this.mobsPerSpawn = 1;
        this.cooldown = 0;
        this.cooldownTimer = 0;
        this.warmup = 0;
        this.warmupTimer = 0;
        this.checkForPlayers = true;
        this.activationRange = 40.0;
        this.leashRange = 32.0;
        this.healOnLeash = false;
        this.resetThreatOnLeash = false;
        this.showFlames = false;
        this.breakable = false;
        this.fieldBoss = false;
        this.conditions = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getMobId() {
        return mobId;
    }

    public void setMobId(String mobId) {
        this.mobId = mobId;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public String getSpawnerGroup() {
        return spawnerGroup;
    }

    public void setSpawnerGroup(String spawnerGroup) {
        this.spawnerGroup = spawnerGroup;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public double getRadiusY() {
        return radiusY;
    }

    public void setRadiusY(double radiusY) {
        this.radiusY = radiusY;
    }

    public boolean isUseTimer() {
        return useTimer;
    }

    public void setUseTimer(boolean useTimer) {
        this.useTimer = useTimer;
    }

    public int getMaxMobs() {
        return maxMobs;
    }

    public void setMaxMobs(int maxMobs) {
        this.maxMobs = Math.max(0, maxMobs);
    }

    public Integer getMobLevel() {
        return mobLevel;
    }

    public void setMobLevel(Integer mobLevel) {
        this.mobLevel = mobLevel;
    }

    public int getMobsPerSpawn() {
        return mobsPerSpawn;
    }

    public void setMobsPerSpawn(int mobsPerSpawn) {
        this.mobsPerSpawn = Math.max(1, mobsPerSpawn);
    }

    public int getCooldown() {
        return cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = Math.max(0, cooldown);
    }

    public int getCooldownTimer() {
        return cooldownTimer;
    }

    public void setCooldownTimer(int cooldownTimer) {
        this.cooldownTimer = Math.max(0, cooldownTimer);
    }

    public int getWarmup() {
        return warmup;
    }

    public void setWarmup(int warmup) {
        this.warmup = Math.max(0, warmup);
    }

    public int getWarmupTimer() {
        return warmupTimer;
    }

    public void setWarmupTimer(int warmupTimer) {
        this.warmupTimer = Math.max(0, warmupTimer);
    }

    public boolean isCheckForPlayers() {
        return checkForPlayers;
    }

    public void setCheckForPlayers(boolean checkForPlayers) {
        this.checkForPlayers = checkForPlayers;
    }

    public double getActivationRange() {
        return activationRange;
    }

    public void setActivationRange(double activationRange) {
        this.activationRange = Math.max(0.0, activationRange);
    }

    public double getLeashRange() {
        return leashRange;
    }

    public void setLeashRange(double leashRange) {
        this.leashRange = Math.max(0.0, leashRange);
    }

    public boolean isHealOnLeash() {
        return healOnLeash;
    }

    public void setHealOnLeash(boolean healOnLeash) {
        this.healOnLeash = healOnLeash;
    }

    public boolean isResetThreatOnLeash() {
        return resetThreatOnLeash;
    }

    public void setResetThreatOnLeash(boolean resetThreatOnLeash) {
        this.resetThreatOnLeash = resetThreatOnLeash;
    }

    public boolean isShowFlames() {
        return showFlames;
    }

    public void setShowFlames(boolean showFlames) {
        this.showFlames = showFlames;
    }

    public boolean isBreakable() {
        return breakable;
    }

    public void setBreakable(boolean breakable) {
        this.breakable = breakable;
    }

    public boolean isFieldBoss() {
        return fieldBoss;
    }

    public void setFieldBoss(boolean fieldBoss) {
        this.fieldBoss = fieldBoss;
    }

    public List<String> getConditions() {
        return conditions;
    }

    public void setConditions(List<String> conditions) {
        this.conditions = conditions == null ? new ArrayList<>() : new ArrayList<>(conditions);
    }

    public Set<UUID> getActiveMobs() {
        return activeMobs;
    }
}
