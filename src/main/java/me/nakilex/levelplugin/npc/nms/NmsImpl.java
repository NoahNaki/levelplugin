package me.nakilex.levelplugin.npc.nms;

import me.nakilex.levelplugin.Main;
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
    private static final String CLASS_CLIENT_INFO_NETWORK = "net.minecraft.server.network.ClientInformation";
    private static final String CLASS_CLIENT_INFO_LEVEL = "net.minecraft.server.level.ClientInformation";
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
            logFailure("Unable to access MinecraftServer", ex);
            return null;
        }
    }

    public static Object getServerLevel(World world) {
        try {
            Method handle = world.getClass().getMethod("getHandle");
            return handle.invoke(world);
        } catch (ReflectiveOperationException ex) {
            logFailure("Unable to access ServerLevel handle", ex);
            return null;
        }
    }

    public static Object getServerPlayer(Player player) {
        try {
            Method handle = player.getClass().getMethod("getHandle");
            return handle.invoke(player);
        } catch (ReflectiveOperationException ex) {
            logFailure("Unable to access ServerPlayer handle", ex);
            return null;
        }
    }

    public static Object getServerPlayer(org.bukkit.entity.Entity entity) {
        if (!(entity instanceof Player player)) {
            return null;
        }
        return getServerPlayer(player);
    }

    public static boolean addEntityToWorld(World world, Object level, Object entity) {
        if (world == null || level == null || entity == null) {
            return false;
        }
        try {
            Class<?> nmsEntityClass = Class.forName("net.minecraft.world.entity.Entity");
            Method addEntityToWorld = world.getClass().getMethod("addEntityToWorld", nmsEntityClass, SpawnReason.class);
            return (boolean) addEntityToWorld.invoke(world, entity, SpawnReason.CUSTOM);
        } catch (ReflectiveOperationException ignored) {
            try {
                Method addFreshEntity = level.getClass().getMethod("addFreshEntity", Class.forName("net.minecraft.world.entity.Entity"), SpawnReason.class);
                return (boolean) addFreshEntity.invoke(level, entity, SpawnReason.CUSTOM);
            } catch (ReflectiveOperationException ex) {
                logFailure("Unable to add NPC entity to world", ex);
                return false;
            }
        }
    }

    public static void addOrRemoveFromPlayerList(Object level, Object player, boolean add) {
        if (level == null || player == null) {
            return;
        }
        List<?> players = getPlayers(level);
        if (players == null) {
            return;
        }
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
        if (level == null) {
            return null;
        }
        try {
            Method players = level.getClass().getMethod("players");
            return (List<?>) players.invoke(level);
        } catch (ReflectiveOperationException ex) {
            logFailure("Unable to access level players list", ex);
            return null;
        }
    }

    private static void updateChunkMapPlayerStatus(Object level, Object player, boolean add) {
        Object chunkSource = getChunkSource(level);
        if (chunkSource == null) {
            return;
        }
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
        if (level == null) {
            return null;
        }
        try {
            Method chunkSource = level.getClass().getMethod("getChunkSource");
            return chunkSource.invoke(level);
        } catch (ReflectiveOperationException ex) {
            logFailure("Unable to access chunk source", ex);
            return null;
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
        if (handle == null) {
            return;
        }
        try {
            Method getProfile = handle.getClass().getMethod("getGameProfile");
            Object profile = getProfile.invoke(handle);
            if (profile == null) {
                return;
            }
            Method getProperties = profile.getClass().getMethod("getProperties");
            Object properties = getProperties.invoke(profile);
            if (properties == null) {
                return;
            }
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
        if (viewerHandle == null || npcHandle == null) {
            return;
        }
        try {
            Class<?> actionClass = Class.forName(CLASS_INFO_ACTION);
            Object addAction = Enum.valueOf((Class<Enum>) actionClass, "ADD_PLAYER");
            Class<?> packetClass = Class.forName(CLASS_INFO_UPDATE);
            Constructor<?> ctor = packetClass.getConstructor(EnumSet.class, List.class);
            EnumSet<?> actions = createActionSet(actionClass, addAction);
            if (actions == null) {
                return;
            }
            Object packet = ctor.newInstance(actions, List.of(npcHandle));
            sendPacket(viewerHandle, packet);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static void sendTabListRemove(Player viewer, Player npcPlayer) {
        Object viewerHandle = getServerPlayer(viewer);
        Object npcHandle = getServerPlayer(npcPlayer);
        if (viewerHandle == null || npcHandle == null) {
            return;
        }
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
            logFailure("Unable to create GameProfile", ex);
            return null;
        }
    }

    public static Object createClientInformation() {
        try {
            Class<?> clientInfoClass = findClass(CLASS_CLIENT_INFO_NETWORK, CLASS_CLIENT_INFO_LEVEL);
            if (clientInfoClass == null) {
                logFailure("Unable to find ClientInformation class", new ClassNotFoundException(CLASS_CLIENT_INFO_NETWORK));
                return null;
            }
            Method createDefault = clientInfoClass.getMethod("createDefault");
            return createDefault.invoke(null);
        } catch (ReflectiveOperationException ex) {
            logFailure("Unable to create ClientInformation", ex);
            return null;
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
        if (serverPlayer == null || packet == null) {
            return;
        }
        Field connectionField = getField(serverPlayer.getClass(), "connection");
        Object connection = connectionField.get(serverPlayer);
        if (connection == null) {
            return;
        }
        Method send = connection.getClass().getMethod("send", Class.forName("net.minecraft.network.protocol.Packet"));
        send.invoke(connection, packet);
    }

    private static void addPlayer(List<?> players, Object player) {
        @SuppressWarnings("unchecked")
        List<Object> mutable = (List<Object>) players;
        mutable.add(player);
    }

    private static EnumSet<?> createActionSet(Class<?> actionClass, Object action) {
        if (actionClass == null || action == null || !actionClass.isEnum() || !(action instanceof Enum)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Class<? extends Enum> enumClass = (Class<? extends Enum>) actionClass;
        EnumSet<?> set = EnumSet.noneOf(enumClass);
        @SuppressWarnings("unchecked")
        EnumSet raw = set;
        raw.add(action);
        return set;
    }

    private static void logFailure(String message, Exception ex) {
        try {
            if (Main.getInstance() != null) {
                Main.getInstance().getLogger().warning(message + ": " + ex.getMessage());
            } else {
                org.bukkit.Bukkit.getLogger().warning(message + ": " + ex.getMessage());
            }
        } catch (Exception ignored) {
            org.bukkit.Bukkit.getLogger().warning(message + ": " + ex.getMessage());
        }
    }

    private static Class<?> findClass(String... names) {
        for (String name : names) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
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
