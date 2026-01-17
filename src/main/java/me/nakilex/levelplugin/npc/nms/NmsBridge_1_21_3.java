package me.nakilex.levelplugin.npc.nms;

import me.nakilex.levelplugin.npc.core.PlayerNpc;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class NmsBridge_1_21_3 implements NmsBridge {
    private final Plugin plugin;

    public NmsBridge_1_21_3(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Object createNpcHandle(UUID uuid, String name, Location location) {
        Object craftServer = Bukkit.getServer();
        Object nmsServer = invoke(craftServer, "getServer");
        Object craftWorld = location.getWorld();
        Object level = invoke(craftWorld, "getHandle");
        Object profile = createGameProfile(uuid, name);
        Object npc = createServerPlayer(nmsServer, level, profile);
        invoke(npc, "moveTo",
                new Class<?>[]{double.class, double.class, double.class, float.class, float.class},
                location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        invoke(npc, "setYHeadRot", new Class<?>[]{float.class}, location.getYaw());
        invoke(npc, "setYBodyRot", new Class<?>[]{float.class}, location.getYaw());
        return npc;
    }

    @Override
    public void spawnNpcForViewer(PlayerNpc npc, Player viewer, boolean removeFromTabLater) {
        if (npc == null || viewer == null) {
            return;
        }
        Object handle = npc.getHandle();
        Object connection = getConnection(viewer);
        if (connection == null) {
            return;
        }
        Object addPlayerPacket = createPlayerInfoPacket("ADD_PLAYER", handle);
        if (addPlayerPacket != null) {
            sendPacket(connection, addPlayerPacket);
        }
        Object addEntityPacket = createAddEntityPacket(handle);
        if (addEntityPacket != null) {
            sendPacket(connection, addEntityPacket);
        }
        Object entityData = invoke(handle, "getEntityData");
        List<?> dataValues = getEntityDataValues(entityData);
        if (dataValues != null && !dataValues.isEmpty()) {
            Object metadataPacket = createSetEntityDataPacket(getEntityId(handle), dataValues);
            if (metadataPacket != null) {
                sendPacket(connection, metadataPacket);
            }
        }
        if (removeFromTabLater) {
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    sendPacket(connection, createPlayerInfoPacket("REMOVE_PLAYER", handle)), 3L);
        }
    }

    @Override
    public void despawnNpcForViewer(PlayerNpc npc, Player viewer) {
        if (npc == null || viewer == null) {
            return;
        }
        Object handle = npc.getHandle();
        Object connection = getConnection(viewer);
        if (connection == null) {
            return;
        }
        Object removePacket = createRemoveEntitiesPacket(getEntityId(handle));
        if (removePacket != null) {
            sendPacket(connection, removePacket);
        }
        Object removeInfoPacket = createPlayerInfoPacket("REMOVE_PLAYER", handle);
        if (removeInfoPacket != null) {
            sendPacket(connection, removeInfoPacket);
        }
    }

    private Object createAddEntityPacket(Object npc) {
        Object packet = createAddEntityPacketWithTracker(npc);
        if (packet != null) {
            return packet;
        }
        return createAddEntityPacketDirect(npc);
    }

    private Object createAddEntityPacketWithTracker(Object npc) {
        try {
            Object serverEntity = createServerEntity(npc);
            if (serverEntity == null) {
                return null;
            }
            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundAddEntityPacket");
            for (Constructor<?> ctor : packetClass.getConstructors()) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length == 2 && params[1].isAssignableFrom(serverEntity.getClass()) && params[0].isInstance(npc)) {
                    return ctor.newInstance(npc, serverEntity);
                }
            }
        } catch (ReflectiveOperationException ex) {
            return null;
        }
        return null;
    }

    private Object createAddEntityPacketDirect(Object npc) {
        try {
            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundAddEntityPacket");
            for (Constructor<?> ctor : packetClass.getConstructors()) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length == 1 && params[0].isInstance(npc)) {
                    return ctor.newInstance(npc);
                }
            }
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to build ClientboundAddEntityPacket", ex);
        }
        throw new IllegalStateException("Unable to build ClientboundAddEntityPacket");
    }

    private Object createServerEntity(Object npc) {
        try {
            Class<?> serverEntityClass = Class.forName("net.minecraft.server.level.ServerEntity");
            for (Constructor<?> ctor : serverEntityClass.getConstructors()) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length < 5) {
                    continue;
                }
                Object serverLevel = invoke(npc, "serverLevel");
                if (serverLevel == null || !params[0].isAssignableFrom(serverLevel.getClass())) {
                    continue;
                }
                if (!int.class.equals(params[2]) || !boolean.class.equals(params[3])) {
                    continue;
                }
                Object[] args = new Object[params.length];
                args[0] = serverLevel;
                args[1] = npc;
                args[2] = 0;
                args[3] = true;
                for (int i = 4; i < params.length; i++) {
                    if (Consumer.class.isAssignableFrom(params[i])) {
                        args[i] = (Consumer<Object>) packet -> {};
                    } else if (BiConsumer.class.isAssignableFrom(params[i])) {
                        args[i] = (BiConsumer<Object, Object>) (packet, packet2) -> {};
                    } else {
                        args[i] = null;
                    }
                }
                return ctor.newInstance(args);
            }
        } catch (ReflectiveOperationException ex) {
            return null;
        }
        return null;
    }

    private Object createServerPlayer(Object server, Object level, Object profile) {
        Object clientInformation = createClientInformation();
        try {
            Class<?> serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");
            for (Constructor<?> ctor : serverPlayerClass.getConstructors()) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length == 4
                        && params[0].isAssignableFrom(server.getClass())
                        && params[1].isAssignableFrom(level.getClass())
                        && params[2].isAssignableFrom(profile.getClass())) {
                    if (clientInformation != null && params[3].isInstance(clientInformation)) {
                        return invokeServerPlayerCtor(ctor, server, level, profile, clientInformation);
                    }
                }
                if (params.length == 3
                        && params[0].isAssignableFrom(server.getClass())
                        && params[1].isAssignableFrom(level.getClass())
                        && params[2].isAssignableFrom(profile.getClass())) {
                    return invokeServerPlayerCtor(ctor, server, level, profile);
                }
            }
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to construct ServerPlayer for NPC", ex);
        }
        throw new IllegalStateException("Unable to construct ServerPlayer for NPC");
    }

    private Object createGameProfile(UUID uuid, String name) {
        try {
            Class<?> profileClass = Class.forName("com.mojang.authlib.GameProfile");
            Constructor<?> ctor = profileClass.getConstructor(UUID.class, String.class);
            return ctor.newInstance(uuid, name);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to construct GameProfile", ex);
        }
    }

    private Object getConnection(Player viewer) {
        Object craftHandle = invoke(viewer, "getHandle");
        if (craftHandle == null) {
            return null;
        }
        Field connectionField = getField(craftHandle.getClass(), "connection");
        if (connectionField == null) {
            return null;
        }
        try {
            connectionField.setAccessible(true);
            return connectionField.get(craftHandle);
        } catch (IllegalAccessException ex) {
            return null;
        }
    }

    private Object createPlayerInfoPacket(String actionName, Object npc) {
        try {
            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
            Class<?> actionClass = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action");
            Object action = Enum.valueOf((Class<Enum>) actionClass, actionName);
            for (Constructor<?> ctor : packetClass.getConstructors()) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length == 2 && params[0].isAssignableFrom(actionClass)) {
                    return ctor.newInstance(action, npc);
                }
            }
        } catch (ReflectiveOperationException ex) {
            return null;
        }
        return null;
    }

    private Object createSetEntityDataPacket(int entityId, List<?> dataValues) {
        try {
            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket");
            for (Constructor<?> ctor : packetClass.getConstructors()) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length == 2 && params[0] == int.class && List.class.isAssignableFrom(params[1])) {
                    return ctor.newInstance(entityId, dataValues);
                }
            }
        } catch (ReflectiveOperationException ex) {
            return null;
        }
        return null;
    }

    private Object createRemoveEntitiesPacket(int entityId) {
        try {
            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket");
            for (Constructor<?> ctor : packetClass.getConstructors()) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length == 1 && params[0] == int.class) {
                    return ctor.newInstance(entityId);
                }
            }
        } catch (ReflectiveOperationException ex) {
            return null;
        }
        return null;
    }

    private List<?> getEntityDataValues(Object entityData) {
        if (entityData == null) {
            return null;
        }
        List<?> dataValues = asList(invoke(entityData, "getNonDefaultValues"));
        if (dataValues == null || dataValues.isEmpty()) {
            dataValues = asList(invoke(entityData, "getAll"));
        }
        return dataValues;
    }

    private List<?> asList(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }
        return null;
    }

    private int getEntityId(Object entity) {
        Object id = invoke(entity, "getId");
        if (id instanceof Integer intId) {
            return intId;
        }
        return -1;
    }

    private void sendPacket(Object connection, Object packet) {
        if (connection == null || packet == null) {
            return;
        }
        Method sendMethod = Arrays.stream(connection.getClass().getMethods())
                .filter(method -> method.getName().equals("send") && method.getParameterCount() == 1)
                .findFirst()
                .orElse(null);
        if (sendMethod == null) {
            return;
        }
        try {
            sendMethod.invoke(connection, packet);
        } catch (ReflectiveOperationException ex) {
            // ignore failed sends
        }
    }

    private Object invoke(Object target, String methodName, Class<?>[] paramTypes, Object... args) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName, paramTypes);
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private Object invoke(Object target, String methodName) {
        return invoke(target, methodName, new Class<?>[0]);
    }

    private Field getField(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ex) {
                // continue
            }
        }
        return null;
    }
    private Object createClientInformation() {
        Object info = createClientInformation("net.minecraft.server.network.ClientInformation");
        if (info != null) {
            return info;
        }
        return createClientInformation("net.minecraft.server.level.ClientInformation");
    }

    private Object createClientInformation(String className) {
        try {
            Class<?> infoClass = Class.forName(className);
            Method createDefault = infoClass.getMethod("createDefault");
            return createDefault.invoke(null);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private Object invokeServerPlayerCtor(Constructor<?> ctor, Object... args) {
        try {
            return ctor.newInstance(args);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to construct ServerPlayer", ex);
        }
    }
}
