package me.nakilex.playerspoofer;

import org.bukkit.Location;

import java.util.UUID;

final class FakePlayer {
    private final String name;
    private final UUID uuid;
    private final int entityId;
    private final BotOrigin origin;
    private final boolean worldVisible;
    private String worldName;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private int ping;
    private int basePing;
    private ConnectionProfile connectionProfile;
    private String skinTexture;
    private String skinSignature;
    private String skinSourceName;
    private long joinedAtMillis;
    private long sessionEndsAtMillis;
    private String personalityId;
    private String activityState;

    FakePlayer(String name, UUID uuid, int entityId, Location location, int ping, int basePing,
               ConnectionProfile connectionProfile, BotOrigin origin, boolean worldVisible,
               String skinTexture, String skinSignature, String skinSourceName,
               long joinedAtMillis, long sessionEndsAtMillis, String personalityId, String activityState) {
        this.name = name;
        this.uuid = uuid;
        this.entityId = entityId;
        this.ping = Math.max(0, ping);
        this.basePing = Math.max(0, basePing);
        this.connectionProfile = connectionProfile == null ? ConnectionProfile.GOOD : connectionProfile;
        this.origin = origin == null ? BotOrigin.MANUAL_SINGLE : origin;
        this.worldVisible = worldVisible;
        this.skinTexture = skinTexture;
        this.skinSignature = skinSignature;
        this.skinSourceName = skinSourceName;
        this.joinedAtMillis = joinedAtMillis > 0 ? joinedAtMillis : System.currentTimeMillis();
        this.sessionEndsAtMillis = Math.max(0L, sessionEndsAtMillis);
        this.personalityId = personalityId == null || personalityId.isBlank() ? "balanced" : personalityId;
        this.activityState = activityState == null || activityState.isBlank() ? "idle" : activityState;
        setLocation(location);
    }

    String name() { return name; }
    UUID uuid() { return uuid; }
    int entityId() { return entityId; }
    String worldName() { return worldName; }
    double x() { return x; }
    double y() { return y; }
    double z() { return z; }
    float yaw() { return yaw; }
    float pitch() { return pitch; }
    int ping() { return ping; }
    int basePing() { return basePing; }
    ConnectionProfile connectionProfile() { return connectionProfile; }
    BotOrigin origin() { return origin; }
    boolean bulk() { return origin == BotOrigin.MANUAL_MULTI; }
    boolean targetManaged() { return origin == BotOrigin.TARGET; }
    boolean worldVisible() { return worldVisible; }
    String skinTexture() { return skinTexture; }
    String skinSignature() { return skinSignature; }
    String skinSourceName() { return skinSourceName; }
    boolean hasSkin() { return skinTexture != null && !skinTexture.isBlank(); }
    long joinedAtMillis() { return joinedAtMillis; }
    long sessionEndsAtMillis() { return sessionEndsAtMillis; }
    String personalityId() { return personalityId; }
    String activityState() { return activityState; }
    boolean sessionExpired(long now) { return targetManaged() && sessionEndsAtMillis > 0 && now >= sessionEndsAtMillis; }

    void setPing(int ping) { this.ping = Math.max(0, ping); }
    void setConnectionProfile(ConnectionProfile profile, int basePing, int ping) {
        this.connectionProfile = profile;
        this.basePing = Math.max(0, basePing);
        this.ping = Math.max(0, ping);
    }
    void setActivityState(String activityState) {
        if (activityState != null && !activityState.isBlank()) this.activityState = activityState;
    }

    void setSkin(String texture, String signature, String sourceName) {
        this.skinTexture = texture;
        this.skinSignature = signature;
        this.skinSourceName = sourceName;
    }

    void setLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            throw new IllegalArgumentException("Fake player location must have a world");
        }
        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
    }

    String serializeLocation() {
        return name + "|" + worldName + "|" + x + "|" + y + "|" + z + "|" + yaw + "|" + pitch;
    }
}
