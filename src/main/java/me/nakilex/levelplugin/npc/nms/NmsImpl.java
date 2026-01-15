package me.nakilex.levelplugin.npc.nms;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.npc.system.trait.SkinTrait;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public final class NmsImpl {
    private static final String CLASS_MINECRAFT_SERVER = "net.minecraft.server.MinecraftServer";
    private static final String CLASS_SERVER_PLAYER = "net.minecraft.server.level.ServerPlayer";
    private static final String CLASS_ENTITY = "net.minecraft.world.entity.Entity";
    private static final String CLASS_GAME_PROFILE = "com.mojang.authlib.GameProfile";
    private static final String CLASS_PROPERTY = "com.mojang.authlib.properties.Property";
    private static final String CLASS_CLIENT_INFO_NETWORK = "net.minecraft.server.network.ClientInformation";
    private static final String CLASS_CLIENT_INFO_LEVEL = "net.minecraft.server.level.ClientInformation";
    private static final String CLASS_INFO_UPDATE = "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket";
    private static final String CLASS_INFO_REMOVE = "net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket";
    private static final String CLASS_INFO_ACTION = "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action";
    private static final String CLASS_COMPONENT = "net.minecraft.network.chat.Component";
    private static final String CLASS_REMOVAL_REASON = "net.minecraft.world.entity.Entity$RemovalReason";

    private static volatile Method addPlayerMethod;
    private static volatile Method addEntityMethod;

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

    public static void addEntityToWorld(Object level, Object entity) {
        if (level == null || entity == null) {
            return;
        }
        Method method = resolveAddMethod(level, isServerPlayer(entity));
        if (method == null) {
            logFailure("Unable to resolve ServerLevel add method", new IllegalStateException("no add method"));
            return;
        }
        try {
            method.invoke(level, entity);
        } catch (ReflectiveOperationException ex) {
            logFailure("Unable to add NPC entity to world", ex);
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
            Constructor<?> ctor = packetClass.getConstructor(Class.forName("java.util.EnumSet"), List.class);
            Object actions = createActionSet(actionClass, addAction);
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

    public static Object getPlayerConnection(Object serverPlayer) {
        if (serverPlayer == null) {
            return null;
        }
        try {
            Field connectionField = serverPlayer.getClass().getField("connection");
            return connectionField.get(serverPlayer);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    public static void disconnectPlayer(Object serverPlayer, String reason) {
        Object connection = getPlayerConnection(serverPlayer);
        if (connection == null) {
            return;
        }
        if (invokeDisconnect(connection, reason)) {
            return;
        }
        Object rawConnection = getConnectionField(connection);
        if (rawConnection != null && invokeDisconnect(rawConnection, reason)) {
            return;
        }
        closeChannel(rawConnection);
    }

    public static void removePlayerFromWorld(Object level, Object player) {
        if (level == null || player == null) {
            return;
        }
        if (invokeLevelRemove(level, player, new String[]{"removePlayerImmediately", "removePlayer"})) {
            return;
        }
        removeEntityFromWorld(level, player);
    }

    public static void removeEntityFromWorld(Object level, Object entity) {
        if (entity == null) {
            return;
        }
        if (level != null && invokeLevelRemove(level, entity, new String[]{"removeEntity", "removeEntityImmediately"})) {
            return;
        }
        invokeEntityRemoval(entity);
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
        Object connection = getPlayerConnection(serverPlayer);
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

    private static Object createActionSet(Class<?> actionClass, Object action) {
        if (actionClass == null || action == null || !actionClass.isEnum() || !(action instanceof Enum)) {
            return null;
        }
        try {
            Method noneOf = Class.forName("java.util.EnumSet").getMethod("noneOf", Class.class);
            Object set = noneOf.invoke(null, actionClass);
            Method add = set.getClass().getMethod("add", Object.class);
            add.invoke(set, action);
            return set;
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private static boolean isServerPlayer(Object entity) {
        if (entity == null) {
            return false;
        }
        return entity.getClass().getName().equals(CLASS_SERVER_PLAYER)
                || isAssignableFrom(CLASS_SERVER_PLAYER, entity.getClass());
    }

    private static boolean isAssignableFrom(String className, Class<?> type) {
        try {
            Class<?> cls = Class.forName(className);
            return cls.isAssignableFrom(type);
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private static Method resolveAddMethod(Object level, boolean isPlayer) {
        if (level == null) {
            return null;
        }
        if (isPlayer) {
            Method method = addPlayerMethod;
            if (method != null) {
                return method;
            }
            method = findMethod(level.getClass(), new String[]{"addNewPlayer", "addPlayer"}, CLASS_SERVER_PLAYER);
            addPlayerMethod = method;
            if (method != null) {
                return method;
            }
        }
        Method method = addEntityMethod;
        if (method != null) {
            return method;
        }
        method = findMethod(level.getClass(), new String[]{"addFreshEntity", "addEntity"}, CLASS_ENTITY);
        addEntityMethod = method;
        return method;
    }

    private static boolean invokeLevelRemove(Object level, Object entity, String[] names) {
        Method method = findMethod(level.getClass(), names, entity.getClass().getName());
        if (method == null) {
            return false;
        }
        try {
            method.invoke(level, entity);
            return true;
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    private static void invokeEntityRemoval(Object entity) {
        if (invokeEntityMethod(entity, "discard")) {
            return;
        }
        if (invokeEntityMethod(entity, "remove")) {
            return;
        }
        Object reason = resolveRemovalReason();
        if (reason == null) {
            return;
        }
        try {
            Method setRemoved = entity.getClass().getMethod("setRemoved", reason.getClass());
            setRemoved.invoke(entity, reason);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static boolean invokeEntityMethod(Object entity, String methodName) {
        try {
            Method method = entity.getClass().getMethod(methodName);
            method.invoke(entity);
            return true;
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    private static Object resolveRemovalReason() {
        try {
            Class<?> reasonClass = Class.forName(CLASS_REMOVAL_REASON);
            Object value = resolveEnumConstant(reasonClass, "DISCARDED");
            if (value != null) {
                return value;
            }
            Object[] constants = reasonClass.getEnumConstants();
            return constants != null && constants.length > 0 ? constants[0] : null;
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    private static Object resolveEnumConstant(Class<?> enumClass, String name) {
        if (enumClass == null || !enumClass.isEnum()) {
            return null;
        }
        try {
            return Enum.valueOf((Class<Enum>) enumClass, name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Object getConnectionField(Object listener) {
        if (listener == null) {
            return null;
        }
        try {
            Field field = listener.getClass().getDeclaredField("connection");
            field.setAccessible(true);
            return field.get(listener);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private static boolean invokeDisconnect(Object target, String reason) {
        if (target == null) {
            return false;
        }
        Object component = createComponentLiteral(reason);
        if (component != null && invokeDisconnect(target, component)) {
            return true;
        }
        try {
            Method disconnect = target.getClass().getMethod("disconnect");
            disconnect.invoke(target);
            return true;
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    private static boolean invokeDisconnect(Object target, Object component) {
        try {
            Method disconnect = target.getClass().getMethod("disconnect", component.getClass());
            disconnect.invoke(target, component);
            return true;
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    private static Object createComponentLiteral(String message) {
        try {
            Class<?> componentClass = Class.forName(CLASS_COMPONENT);
            Method literal = componentClass.getMethod("literal", String.class);
            return literal.invoke(null, message);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private static void closeChannel(Object connection) {
        if (connection == null) {
            return;
        }
        try {
            Field channelField = connection.getClass().getDeclaredField("channel");
            channelField.setAccessible(true);
            Object channel = channelField.get(connection);
            if (channel == null) {
                return;
            }
            Method close = channel.getClass().getMethod("close");
            close.invoke(channel);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Method findMethod(Class<?> owner, String[] names, String paramClassName) {
        Class<?> paramClass = findClass(paramClassName);
        if (paramClass == null) {
            return null;
        }
        for (String name : names) {
            try {
                return owner.getMethod(name, paramClass);
            } catch (NoSuchMethodException ignored) {
            }
        }
        for (Method method : owner.getMethods()) {
            if (!matchesName(method.getName(), names)) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 1 && params[0].isAssignableFrom(paramClass)) {
                return method;
            }
        }
        return null;
    }

    private static boolean matchesName(String value, String[] names) {
        for (String name : names) {
            if (name.equals(value)) {
                return true;
            }
        }
        return false;
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
}
