package me.nakilex.levelplugin.npc.nms;

import me.nakilex.levelplugin.npc.system.trait.SkinTrait;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public final class NmsImpl {
    private static final String CLASS_MINECRAFT_SERVER = "net.minecraft.server.MinecraftServer";
    private static final String CLASS_SERVER_PLAYER = "net.minecraft.server.level.ServerPlayer";
    private static final String CLASS_GAME_PROFILE = "com.mojang.authlib.GameProfile";
    private static final String CLASS_PROPERTY = "com.mojang.authlib.properties.Property";
    private static final String CLASS_CLIENT_INFO = "net.minecraft.server.network.ClientInformation";
    private static final String CLASS_INFO_UPDATE = "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket";
    private static final String CLASS_INFO_REMOVE = "net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket";
    private static final String CLASS_INFO_ACTION = "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action";

    private NmsImpl() {
    }

    public static Object getMinecraftServer() {
        try {
            Class<?> serverClass = Class.forName(CLASS_MINECRAFT_SERVER);
            Method getServer = serverClass.getMethod("getServer");
            return getServer.invoke(null);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to access MinecraftServer", ex);
        }
    }

    public static Object getServerLevel(World world) {
        try {
            Method handle = world.getClass().getMethod("getHandle");
            return handle.invoke(world);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to access ServerLevel handle", ex);
        }
    }

    public static Object getServerPlayer(Player player) {
        try {
            Method handle = player.getClass().getMethod("getHandle");
            return handle.invoke(player);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to access ServerPlayer handle", ex);
        }
    }

    public static Object getServerPlayer(org.bukkit.entity.Entity entity) {
        if (!(entity instanceof Player player)) {
            return null;
        }
        return getServerPlayer(player);
    }

    public static boolean addEntityToWorld(World world, Object level, Object entity) {
        try {
            Class<?> nmsEntityClass = Class.forName("net.minecraft.world.entity.Entity");
            Method addEntityToWorld = world.getClass().getMethod("addEntityToWorld", nmsEntityClass, SpawnReason.class);
            return (boolean) addEntityToWorld.invoke(world, entity, SpawnReason.CUSTOM);
        } catch (ReflectiveOperationException ignored) {
            try {
                Method addFreshEntity = level.getClass().getMethod("addFreshEntity", Class.forName("net.minecraft.world.entity.Entity"), SpawnReason.class);
                return (boolean) addFreshEntity.invoke(level, entity, SpawnReason.CUSTOM);
            } catch (ReflectiveOperationException ex) {
                return false;
            }
        }
    }

    public static void addOrRemoveFromPlayerList(Object level, Object player, boolean add) {
        List<?> players = getPlayers(level);
        if (add) {
            if (!players.contains(player)) {
                addPlayer(players, player);
            }
        } else {
            players.remove(player);
        }
        updateChunkMapPlayerStatus(level, player, add);
    }

    private static List<?> getPlayers(Object level) {
        try {
            Method players = level.getClass().getMethod("players");
            return (List<?>) players.invoke(level);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to access level players list", ex);
        }
    }

    private static void updateChunkMapPlayerStatus(Object level, Object player, boolean add) {
        Object chunkSource = getChunkSource(level);
        try {
            Field chunkMapField = chunkSource.getClass().getDeclaredField("chunkMap");
            chunkMapField.setAccessible(true);
            Object chunkMap = chunkMapField.get(chunkSource);
            invokeChunkMapPlayerMethod(chunkMap, player, add ? "addPlayer" : "removePlayer");
            invokeChunkMapPlayerMethod(chunkMap, player, add ? "addEntity" : "removeEntity");
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Object getChunkSource(Object level) {
        try {
            Method chunkSource = level.getClass().getMethod("getChunkSource");
            return chunkSource.invoke(level);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to access chunk source", ex);
        }
    }

    private static void invokeChunkMapPlayerMethod(Object chunkMap, Object player, String methodName) {
        if (chunkMap == null) {
            return;
        }
        try {
            Method method = chunkMap.getClass().getMethod(methodName, Class.forName(CLASS_SERVER_PLAYER));
            method.invoke(chunkMap, player);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static void applySkin(Player npcPlayer, SkinTrait skin) {
        if (npcPlayer == null || skin == null) {
            return;
        }
        Object handle = getServerPlayer(npcPlayer);
        try {
            Method getProfile = handle.getClass().getMethod("getGameProfile");
            Object profile = getProfile.invoke(handle);
            Method getProperties = profile.getClass().getMethod("getProperties");
            Object properties = getProperties.invoke(profile);
            Method removeAll = properties.getClass().getMethod("removeAll", String.class);
            removeAll.invoke(properties, "textures");
            Class<?> propertyClass = Class.forName(CLASS_PROPERTY);
            Constructor<?> propertyCtor = propertyClass.getConstructor(String.class, String.class, String.class);
            Object property = propertyCtor.newInstance("textures", skin.getTexture(), skin.getSignature());
            Method put = properties.getClass().getMethod("put", String.class, propertyClass);
            put.invoke(properties, "textures", property);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static void sendTabListAdd(Player viewer, Player npcPlayer) {
        Object viewerHandle = getServerPlayer(viewer);
        Object npcHandle = getServerPlayer(npcPlayer);
        try {
            Class<?> actionClass = Class.forName(CLASS_INFO_ACTION);
            Object addAction = Enum.valueOf((Class<Enum>) actionClass, "ADD_PLAYER");
            Class<?> packetClass = Class.forName(CLASS_INFO_UPDATE);
            Constructor<?> ctor = packetClass.getConstructor(EnumSet.class, List.class);
            Object packet = ctor.newInstance(EnumSet.of((Enum<?>) addAction), List.of(npcHandle));
            sendPacket(viewerHandle, packet);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static void sendTabListRemove(Player viewer, Player npcPlayer) {
        Object viewerHandle = getServerPlayer(viewer);
        Object npcHandle = getServerPlayer(npcPlayer);
        try {
            Method getUuid = npcHandle.getClass().getMethod("getUUID");
            Object uuid = getUuid.invoke(npcHandle);
            Class<?> packetClass = Class.forName(CLASS_INFO_REMOVE);
            Constructor<?> ctor = packetClass.getConstructor(List.class);
            Object packet = ctor.newInstance(List.of(uuid));
            sendPacket(viewerHandle, packet);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static Object createGameProfile(UUID uuid, String name) {
        try {
            Class<?> profileClass = Class.forName(CLASS_GAME_PROFILE);
            Constructor<?> ctor = profileClass.getConstructor(UUID.class, String.class);
            return ctor.newInstance(uuid, name);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to create GameProfile", ex);
        }
    }

    public static Object createClientInformation() {
        try {
            Class<?> clientInfoClass = Class.forName(CLASS_CLIENT_INFO);
            Method createDefault = clientInfoClass.getMethod("createDefault");
            return createDefault.invoke(null);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to create ClientInformation", ex);
        }
    }

    public static void applyPosition(Object entity, Location location) {
        if (entity == null || location == null) {
            return;
        }
        try {
            Method setPos = entity.getClass().getMethod("setPos", double.class, double.class, double.class);
            setPos.invoke(entity, location.getX(), location.getY(), location.getZ());
            Method setYRot = entity.getClass().getMethod("setYRot", float.class);
            setYRot.invoke(entity, location.getYaw());
            Method setXRot = entity.getClass().getMethod("setXRot", float.class);
            setXRot.invoke(entity, location.getPitch());
            Method setYHeadRot = entity.getClass().getMethod("setYHeadRot", float.class);
            setYHeadRot.invoke(entity, location.getYaw());
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void sendPacket(Object serverPlayer, Object packet) throws ReflectiveOperationException {
        Field connectionField = getField(serverPlayer.getClass(), "connection");
        Object connection = connectionField.get(serverPlayer);
        Method send = connection.getClass().getMethod("send", Class.forName("net.minecraft.network.protocol.Packet"));
        send.invoke(connection, packet);
    }

    private static void addPlayer(List<?> players, Object player) {
        @SuppressWarnings("unchecked")
        List<Object> mutable = (List<Object>) players;
        mutable.add(player);
    }

    private static Field getField(Class<?> type, String name) throws ReflectiveOperationException {
        try {
            Field field = type.getField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException ignored) {
        }
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
