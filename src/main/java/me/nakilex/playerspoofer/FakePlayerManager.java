package me.nakilex.playerspoofer;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.chat.RemoteChatSession;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

final class FakePlayerManager {
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final AtomicInteger NEXT_ENTITY_ID = new AtomicInteger(2_000_000_000);
    private static final List<String> PERSONALITIES = List.of("balanced", "quiet", "social", "grinder", "explorer", "afk-prone");

    private static final EnumSet<WrapperPlayServerPlayerInfoUpdate.Action> TAB_ACTIONS = EnumSet.of(
            WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
            WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_GAME_MODE,
            WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED,
            WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LATENCY,
            WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_DISPLAY_NAME,
            WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LIST_ORDER,
            WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_HAT
    );

    private static final EnumSet<WrapperPlayServerPlayerInfoUpdate.Action> LATENCY_ACTION = EnumSet.of(
            WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LATENCY
    );

    private final PlayerSpooferPlugin plugin;
    private final Map<String, FakePlayer> fakePlayers = new LinkedHashMap<>();

    FakePlayerManager(PlayerSpooferPlugin plugin) {
        this.plugin = plugin;
    }

    void load() {
        fakePlayers.clear();
        Map<String, Location> savedLocations = parseSavedLocations();
        Location fallback = defaultLocation();

        for (String rawName : plugin.getConfig().getStringList("fake-players")) {
            String name = rawName == null ? "" : rawName.trim();
            if (!isValidName(name) || fakePlayers.containsKey(key(name))) continue;

            Location location = savedLocations.getOrDefault(key(name), fallback);
            if (location == null || location.getWorld() == null) {
                plugin.getLogger().warning("Could not restore fake player " + name + ": no loaded world exists.");
                continue;
            }

            String path = "fake-player-meta." + key(name);
            int legacyPing = plugin.getConfig().getInt(path + ".ping", 80);
            boolean legacyBulk = plugin.getConfig().getBoolean(path + ".bulk", false);
            BotOrigin origin = BotOrigin.fromStored(plugin.getConfig().getString(path + ".origin"), legacyBulk);
            boolean visible = plugin.getConfig().getBoolean(path + ".world-visible", origin == BotOrigin.MANUAL_SINGLE);
            String texture = blankToNull(plugin.getConfig().getString(path + ".skin-texture"));
            String signature = blankToNull(plugin.getConfig().getString(path + ".skin-signature"));
            String skinSource = blankToNull(plugin.getConfig().getString(path + ".skin-source"));

            ConnectionProfile connectionProfile = ConnectionProfile.fromStored(
                    plugin.getConfig().getString(path + ".connection-profile"), legacyPing);
            int basePing = plugin.getConfig().getInt(path + ".base-ping", legacyPing);
            int ping = Math.max(0, legacyPing);
            long joinedAt = plugin.getConfig().getLong(path + ".joined-at", System.currentTimeMillis());
            long sessionEndsAt = plugin.getConfig().getLong(path + ".session-ends-at", 0L);
            String personality = blankToNull(plugin.getConfig().getString(path + ".personality"));
            String activity = blankToNull(plugin.getConfig().getString(path + ".activity-state"));

            fakePlayers.put(key(name), new FakePlayer(
                    name, deterministicUuid(name), nextEntityId(), location, ping, basePing, connectionProfile,
                    origin, visible, texture, signature, skinSource, joinedAt, sessionEndsAt,
                    personality == null ? randomPersonality() : personality,
                    activity == null ? "idle" : activity
            ));
        }
        save();
    }

    int size() { return fakePlayers.size(); }
    int visibleSize() { return (int) fakePlayers.values().stream().filter(FakePlayer::hasSkin).count(); }
    int bulkSize() { return (int) fakePlayers.values().stream().filter(fake -> fake.origin() == BotOrigin.MANUAL_MULTI).count(); }
    int targetManagedSize() { return (int) fakePlayers.values().stream().filter(FakePlayer::targetManaged).count(); }
    int manualSize() { return size() - targetManagedSize(); }
    int worldVisibleSize() { return (int) fakePlayers.values().stream().filter(FakePlayer::worldVisible).count(); }
    int usedSkinCount() { return usedSkinTexturesExcept(null).size(); }
    Collection<FakePlayer> all() { return List.copyOf(fakePlayers.values()); }

    boolean isValidName(String name) {
        return name != null && VALID_NAME.matcher(name).matches();
    }

    boolean contains(String name) { return fakePlayers.containsKey(key(name)); }

    FakePlayer add(String name, Location location, boolean bulk, boolean worldVisible) {
        return add(name, location, bulk ? BotOrigin.MANUAL_MULTI : BotOrigin.MANUAL_SINGLE, worldVisible, null, null, null);
    }

    FakePlayer add(String name, Location location, boolean bulk, boolean worldVisible,
                   String skinTexture, String skinSignature, String skinSourceName) {
        return add(name, location, bulk ? BotOrigin.MANUAL_MULTI : BotOrigin.MANUAL_SINGLE,
                worldVisible, skinTexture, skinSignature, skinSourceName);
    }

    FakePlayer add(String name, Location location, BotOrigin origin, boolean worldVisible,
                   String skinTexture, String skinSignature, String skinSourceName) {
        String cleanName = name.trim();
        if (!isValidName(cleanName)) {
            throw new IllegalArgumentException("Minecraft names must be 1-16 characters using only letters, numbers and underscores.");
        }
        if (contains(cleanName)) return null;
        if (location == null || location.getWorld() == null) throw new IllegalArgumentException("No valid spawn location is available.");
        if (skinTexture != null && skinInUseByOther(skinTexture, cleanName)) {
            throw new IllegalArgumentException("That skin texture is already assigned to another spoofed player.");
        }

        ConnectionProfile connectionProfile = ConnectionProfile.randomWeighted();
        int basePing = connectionProfile.randomBasePing();
        int ping = connectionProfile.initialPing(basePing);
        long joinedAt = System.currentTimeMillis();
        long sessionEndsAt = origin == BotOrigin.TARGET ? randomSessionEnd(joinedAt) : 0L;

        FakePlayer fake = new FakePlayer(
                cleanName, deterministicUuid(cleanName), nextEntityId(), location, ping, basePing, connectionProfile,
                origin, worldVisible, skinTexture, skinSignature, skinSourceName,
                joinedAt, sessionEndsAt, randomPersonality(), "idle"
        );
        fakePlayers.put(key(cleanName), fake);
        save();
        broadcastAdd(fake);
        return fake;
    }

    FakePlayer remove(String name) {
        FakePlayer fake = fakePlayers.remove(key(name));
        if (fake == null) return null;
        broadcastRemove(fake);
        save();
        return fake;
    }

    List<FakePlayer> removeBulk(int quantity) {
        List<FakePlayer> bulk = new ArrayList<>(fakePlayers.values().stream()
                .filter(fake -> fake.origin() == BotOrigin.MANUAL_MULTI).toList());
        Collections.reverse(bulk);
        List<FakePlayer> removed = new ArrayList<>();
        for (FakePlayer fake : bulk) {
            if (removed.size() >= quantity) break;
            FakePlayer result = remove(fake.name());
            if (result != null) removed.add(result);
        }
        return removed;
    }

    FakePlayer removeOneTargetManaged(boolean preferExpired) {
        long now = System.currentTimeMillis();
        List<FakePlayer> candidates = fakePlayers.values().stream().filter(FakePlayer::targetManaged).toList();
        if (candidates.isEmpty()) return null;
        FakePlayer selected = null;
        if (preferExpired) {
            selected = candidates.stream().filter(fake -> fake.sessionExpired(now))
                    .min(java.util.Comparator.comparingLong(FakePlayer::sessionEndsAtMillis)).orElse(null);
        }
        if (selected == null) {
            // Prefer bots that have been around longest so churn feels like sessions ending.
            selected = candidates.stream().min(java.util.Comparator.comparingLong(FakePlayer::joinedAtMillis)).orElse(null);
        }
        return selected == null ? null : remove(selected.name());
    }

    int clearTargetManaged() {
        List<String> names = fakePlayers.values().stream().filter(FakePlayer::targetManaged).map(FakePlayer::name).toList();
        int removed = 0;
        for (String name : names) if (remove(name) != null) removed++;
        return removed;
    }

    int clear() {
        int count = fakePlayers.size();
        if (count == 0) return 0;
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            destroyAllEntities(viewer);
            removeAllFromTab(viewer);
        }
        fakePlayers.clear();
        save();
        return count;
    }

    FakePlayer move(String name, Location location) {
        FakePlayer fake = fakePlayers.get(key(name));
        if (fake == null) return null;
        if (!fake.worldVisible()) return fake;
        if (location == null || location.getWorld() == null) throw new IllegalArgumentException("No valid location is available.");

        fake.setLocation(location);
        save();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            send(viewer, new WrapperPlayServerDestroyEntities(fake.entityId()));
            spawnEntityIfSameWorld(viewer, fake);
        }
        return fake;
    }

    boolean updateSkin(String name, String texture, String signature, String sourceName) {
        FakePlayer fake = fakePlayers.get(key(name));
        if (fake == null || texture == null || texture.isBlank()) return false;
        if (skinInUseByOther(texture, fake.name())) return false;
        fake.setSkin(texture, signature, sourceName);
        save();

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            send(viewer, new WrapperPlayServerPlayerInfoRemove(fake.uuid()));
            if (fake.worldVisible()) send(viewer, new WrapperPlayServerDestroyEntities(fake.entityId()));
            sendTabEntry(viewer, fake);
            spawnEntityIfSameWorld(viewer, fake);
        }
        return true;
    }

    boolean clearSkin(String name) {
        FakePlayer fake = fakePlayers.get(key(name));
        if (fake == null || !fake.hasSkin()) return false;
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            send(viewer, new WrapperPlayServerPlayerInfoRemove(fake.uuid()));
            if (fake.worldVisible()) send(viewer, new WrapperPlayServerDestroyEntities(fake.entityId()));
        }
        fake.setSkin(null, null, null);
        save();
        return true;
    }

    List<String> clearDuplicateSkinAssignments() {
        Set<String> seen = new HashSet<>();
        List<String> cleared = new ArrayList<>();
        for (FakePlayer fake : fakePlayers.values()) {
            if (!fake.hasSkin()) continue;
            if (seen.add(fake.skinTexture())) continue;
            fake.setSkin(null, null, null);
            cleared.add(fake.name());
        }
        if (!cleared.isEmpty()) save();
        return cleared;
    }

    Set<String> usedSkinTexturesExcept(String excludedName) {
        String excludedKey = key(excludedName);
        Set<String> used = new HashSet<>();
        for (FakePlayer fake : fakePlayers.values()) {
            if (!fake.hasSkin()) continue;
            if (excludedName != null && key(fake.name()).equals(excludedKey)) continue;
            used.add(fake.skinTexture());
        }
        return used;
    }

    boolean skinInUseByOther(String texture, String excludedName) {
        return texture != null && usedSkinTexturesExcept(excludedName).contains(texture);
    }

    void rerollLatencyProfiles() {
        for (FakePlayer fake : fakePlayers.values()) {
            ConnectionProfile profile = ConnectionProfile.randomWeighted();
            int base = profile.randomBasePing();
            fake.setConnectionProfile(profile, base, profile.initialPing(base));
        }
        save();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            for (FakePlayer fake : fakePlayers.values()) sendLatency(viewer, fake);
        }
    }

    void jitterLatencies() {
        if (fakePlayers.isEmpty()) return;
        for (FakePlayer fake : fakePlayers.values()) {
            fake.setPing(fake.connectionProfile().nextPing(fake.basePing()));
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            for (FakePlayer fake : fakePlayers.values()) sendLatency(viewer, fake);
        }
    }

    int[] pingBarCounts() {
        int[] counts = new int[5]; // 0=1bar ... 4=5bars
        for (FakePlayer fake : fakePlayers.values()) {
            if (!fake.hasSkin()) continue;
            int ping = fake.ping();
            if (ping < 150) counts[4]++;
            else if (ping < 300) counts[3]++;
            else if (ping < 600) counts[2]++;
            else if (ping < 1000) counts[1]++;
            else counts[0]++;
        }
        return counts;
    }

    void showAll(Player viewer) {
        if (viewer == null || !viewer.isOnline()) return;
        destroyAllEntities(viewer);
        for (FakePlayer fake : fakePlayers.values()) {
            sendTabEntry(viewer, fake);
            spawnEntityIfSameWorld(viewer, fake);
        }
    }

    void refreshEntities(Player viewer) {
        if (viewer == null || !viewer.isOnline()) return;
        destroyAllEntities(viewer);
        for (FakePlayer fake : fakePlayers.values()) spawnEntityIfSameWorld(viewer, fake);
    }

    void hideAll(Player viewer) {
        if (viewer == null || !viewer.isOnline()) return;
        destroyAllEntities(viewer);
        removeAllFromTab(viewer);
    }

    Location defaultLocation() {
        List<World> worlds = Bukkit.getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0).getSpawnLocation();
    }

    private void broadcastAdd(FakePlayer fake) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            sendTabEntry(viewer, fake);
            spawnEntityIfSameWorld(viewer, fake);
        }
    }

    private void broadcastRemove(FakePlayer fake) {
        WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(fake.entityId());
        WrapperPlayServerPlayerInfoRemove removeTab = new WrapperPlayServerPlayerInfoRemove(fake.uuid());
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (fake.worldVisible()) send(viewer, destroy);
            send(viewer, removeTab);
        }
    }

    private UserProfile profile(FakePlayer fake) {
        if (!fake.hasSkin()) return new UserProfile(fake.uuid(), fake.name());
        TextureProperty textures = new TextureProperty("textures", fake.skinTexture(), fake.skinSignature());
        return new UserProfile(fake.uuid(), fake.name(), List.of(textures));
    }

    private WrapperPlayServerPlayerInfoUpdate.PlayerInfo playerInfo(FakePlayer fake) {
        // A null display name makes the client fall back to the plain username. Supplying one
        // is what puts the rank prefix in front of a spoofed player in the tab list, the same
        // way DeluxeTags does it for real players.
        Component displayName = plugin.getConfig().getBoolean("chat.display.match-real-players", true)
                ? plugin.appearance().tabName(fake)
                : null;
        return new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                profile(fake), true, fake.ping(), GameMode.SURVIVAL, displayName,
                (RemoteChatSession) null, 0, true
        );
    }

    private void sendTabEntry(Player viewer, FakePlayer fake) {
        if (!fake.hasSkin()) return;
        send(viewer, new WrapperPlayServerPlayerInfoUpdate(TAB_ACTIONS, playerInfo(fake)));
    }

    private void sendLatency(Player viewer, FakePlayer fake) {
        if (!fake.hasSkin()) return;
        send(viewer, new WrapperPlayServerPlayerInfoUpdate(LATENCY_ACTION, playerInfo(fake)));
    }

    private void spawnEntityIfSameWorld(Player viewer, FakePlayer fake) {
        if (!fake.worldVisible() || !fake.hasSkin()) return;
        if (viewer.getWorld() == null || !viewer.getWorld().getName().equals(fake.worldName())) return;

        com.github.retrooper.packetevents.protocol.world.Location location =
                new com.github.retrooper.packetevents.protocol.world.Location(
                        fake.x(), fake.y(), fake.z(), fake.yaw(), fake.pitch()
                );
        send(viewer, new WrapperPlayServerSpawnEntity(
                fake.entityId(), fake.uuid(), EntityTypes.PLAYER, location, fake.yaw(), 0, null
        ));
    }

    private void destroyAllEntities(Player viewer) {
        int[] ids = fakePlayers.values().stream().filter(FakePlayer::worldVisible).mapToInt(FakePlayer::entityId).toArray();
        if (ids.length > 0) send(viewer, new WrapperPlayServerDestroyEntities(ids));
    }

    private void removeAllFromTab(Player viewer) {
        if (fakePlayers.isEmpty()) return;
        UUID[] uuids = fakePlayers.values().stream().map(FakePlayer::uuid).toArray(UUID[]::new);
        send(viewer, new WrapperPlayServerPlayerInfoRemove(uuids));
    }

    private void send(Player viewer, PacketWrapper<?> packet) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
    }

    private Map<String, Location> parseSavedLocations() {
        Map<String, Location> result = new LinkedHashMap<>();
        for (String record : plugin.getConfig().getStringList("fake-player-locations")) {
            if (record == null || record.isBlank()) continue;
            String[] parts = record.split("\\|", -1);
            if (parts.length != 7) continue;
            try {
                World world = Bukkit.getWorld(parts[1]);
                if (world == null) continue;
                result.put(key(parts[0]), new Location(
                        world, Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Double.parseDouble(parts[4]),
                        Float.parseFloat(parts[5]), Float.parseFloat(parts[6])
                ));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    void save() {
        List<String> names = new ArrayList<>();
        List<String> locations = new ArrayList<>();
        plugin.getConfig().set("fake-player-meta", null);

        for (FakePlayer fake : fakePlayers.values()) {
            names.add(fake.name());
            locations.add(fake.serializeLocation());
            String path = "fake-player-meta." + key(fake.name());
            plugin.getConfig().set(path + ".name", fake.name());
            plugin.getConfig().set(path + ".ping", fake.ping());
            plugin.getConfig().set(path + ".base-ping", fake.basePing());
            plugin.getConfig().set(path + ".connection-profile", fake.connectionProfile().name());
            plugin.getConfig().set(path + ".bulk", fake.bulk()); // backwards compatibility with <=1.4
            plugin.getConfig().set(path + ".origin", fake.origin().name());
            plugin.getConfig().set(path + ".world-visible", fake.worldVisible());
            plugin.getConfig().set(path + ".joined-at", fake.joinedAtMillis());
            plugin.getConfig().set(path + ".session-ends-at", fake.sessionEndsAtMillis());
            plugin.getConfig().set(path + ".personality", fake.personalityId());
            plugin.getConfig().set(path + ".activity-state", fake.activityState());
            if (fake.hasSkin()) {
                plugin.getConfig().set(path + ".skin-texture", fake.skinTexture());
                plugin.getConfig().set(path + ".skin-signature", fake.skinSignature());
                if (fake.skinSourceName() != null) plugin.getConfig().set(path + ".skin-source", fake.skinSourceName());
            }
        }
        plugin.getConfig().set("fake-players", names);
        plugin.getConfig().set("fake-player-locations", locations);
        plugin.saveConfig();
    }

    private long randomSessionEnd(long joinedAt) {
        long minMinutes = Math.max(1L, plugin.getConfig().getLong("population.session-minutes-min", 20L));
        long maxMinutes = Math.max(minMinutes, plugin.getConfig().getLong("population.session-minutes-max", 240L));
        long minutes = ThreadLocalRandom.current().nextLong(minMinutes, maxMinutes + 1L);
        return joinedAt + minutes * 60_000L;
    }

    private static String randomPersonality() {
        return PERSONALITIES.get(ThreadLocalRandom.current().nextInt(PERSONALITIES.size()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String key(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    private static UUID deterministicUuid(String name) {
        return UUID.nameUUIDFromBytes(("PlayerSpoofer:" + key(name)).getBytes(StandardCharsets.UTF_8));
    }

    private static int nextEntityId() { return NEXT_ENTITY_ID.getAndDecrement(); }
}
