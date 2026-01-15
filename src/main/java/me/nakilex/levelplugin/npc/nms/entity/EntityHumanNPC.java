package me.nakilex.levelplugin.npc.nms.entity;

import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.npc.system.NpcTagUtil;
import io.netty.channel.embedded.EmbeddedChannel;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class EntityHumanNPC {
    private static final String CLASS_SERVER_PLAYER = "net.minecraft.server.level.ServerPlayer";
    private static final String CLASS_CONNECTION = "net.minecraft.network.Connection";
    private static final String CLASS_PACKET_FLOW_PROTOCOL = "net.minecraft.network.protocol.PacketFlow";
    private static final String CLASS_PACKET_FLOW_NETWORK = "net.minecraft.network.PacketFlow";
    private static final String CLASS_LISTENER = "net.minecraft.server.network.ServerGamePacketListenerImpl";
    private static final String CLASS_COOKIE = "net.minecraft.server.network.CommonListenerCookie";
    private static final String CLASS_STATS = "net.minecraft.stats.ServerStatsCounter";
    private static final String CLASS_ADVANCEMENTS = "net.minecraft.server.PlayerAdvancements";

    private final NPC npc;
    private final Object handle;

    public EntityHumanNPC(Object server,
                          Object level,
                          Object profile,
                          Object clientInformation,
                          NPC npc) {
        this.npc = npc;
        this.handle = createServerPlayer(server, level, profile, clientInformation);
        if (this.handle == null) {
            return;
        }
        setupNetworkStack(server, profile, clientInformation);
        setupStatsAndAdvancements(server);
        markClientLoaded();
    }

    public NPC getNpc() {
        return npc;
    }

    public Object getHandle() {
        return handle;
    }

    public org.bukkit.entity.Entity getBukkitEntity() {
        if (handle == null) {
            return null;
        }
        try {
            Method method = handle.getClass().getMethod("getBukkitEntity");
            return (org.bukkit.entity.Entity) method.invoke(handle);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private Object createServerPlayer(Object server, Object level, Object profile, Object clientInformation) {
        if (server == null || level == null || profile == null || clientInformation == null) {
            return null;
        }
        try {
            Class<?> serverPlayerClass = Class.forName(CLASS_SERVER_PLAYER);
            for (Constructor<?> ctor : serverPlayerClass.getConstructors()) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length == 4
                        && params[0].isInstance(server)
                        && params[1].isInstance(level)
                        && params[2].isInstance(profile)
                        && params[3].isInstance(clientInformation)) {
                    return ctor.newInstance(server, level, profile, clientInformation);
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private void setupNetworkStack(Object server, Object profile, Object clientInformation) {
        if (server == null || profile == null || clientInformation == null || handle == null) {
            return;
        }
        try {
            Object connection = createConnection();
            Object cookie = createListenerCookie(profile, clientInformation);
            Object listener = createListener(server, connection, handle, cookie);

            try {
                attachListenerToConnection(connection, listener);
            } catch (ReflectiveOperationException ignored) {
                // best-effort
            }

            Field connectionField = getField(handle.getClass(), "connection");
            connectionField.set(handle, listener);

            NpcTagUtil.tagNpc(getBukkitEntity());
            placeNewPlayer(server, connection, handle, cookie);
        } catch (ReflectiveOperationException ex) {
            System.out.println("[NPC] setupNetworkStack failed: " + ex.getClass().getSimpleName()
                    + ": " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private Object createConnection() throws ReflectiveOperationException {
        Class<?> connectionClass = Class.forName(CLASS_CONNECTION);
        Class<?> packetFlowClass = findPacketFlowClass();
        Object serverbound = resolvePacketFlowServerbound(packetFlowClass);
        Constructor<?> connectionCtor = connectionClass.getConstructor(packetFlowClass);
        Object connection = connectionCtor.newInstance(serverbound);
        setChannel(connection);
        return connection;
    }

    private Class<?> findPacketFlowClass() throws ClassNotFoundException {
        try {
            return Class.forName(CLASS_PACKET_FLOW_PROTOCOL);
        } catch (ClassNotFoundException ex) {
            return Class.forName(CLASS_PACKET_FLOW_NETWORK);
        }
    }

    private Object resolvePacketFlowServerbound(Class<?> packetFlowClass) {
        try {
            return Enum.valueOf((Class<Enum>) packetFlowClass.asSubclass(Enum.class), "SERVERBOUND");
        } catch (IllegalArgumentException ex) {
            Object[] constants = packetFlowClass.getEnumConstants();
            if (constants != null && constants.length > 0) {
                return constants[0];
            }
            throw ex;
        }
    }

    private Object createListenerCookie(Object profile, Object clientInformation) throws ReflectiveOperationException {
        Class<?> cookieClass = Class.forName(CLASS_COOKIE);

        for (Method method : cookieClass.getMethods()) {
            if (!method.getName().equals("createInitial")) {
                continue;
            }
            if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            Object built = tryInvokeCookieFactory(method, profile, clientInformation);
            if (built != null) {
                return built;
            }
        }

        for (Constructor<?> ctor : cookieClass.getConstructors()) {
            Object built = tryConstructCookie(ctor, profile, clientInformation);
            if (built != null) {
                return built;
            }
        }

        throw new ReflectiveOperationException("No compatible CommonListenerCookie factory/constructor found");
    }

    private Object tryInvokeCookieFactory(Method method, Object profile, Object clientInformation) {
        try {
            Class<?>[] params = method.getParameterTypes();
            Object[] args = new Object[params.length];

            for (int i = 0; i < params.length; i++) {
                Class<?> type = params[i];

                if (type.isInstance(profile)) {
                    args[i] = profile;
                } else if (type.isInstance(clientInformation)) {
                    args[i] = clientInformation;
                } else if (type == boolean.class || type == Boolean.class) {
                    args[i] = false;
                } else if (type == int.class || type == Integer.class) {
                    args[i] = 0;
                } else {
                    return null;
                }
            }

            return method.invoke(null, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object tryConstructCookie(Constructor<?> ctor, Object profile, Object clientInformation) {
        try {
            Class<?>[] params = ctor.getParameterTypes();
            Object[] args = new Object[params.length];

            for (int i = 0; i < params.length; i++) {
                Class<?> type = params[i];

                if (type.isInstance(profile)) {
                    args[i] = profile;
                } else if (type.isInstance(clientInformation)) {
                    args[i] = clientInformation;
                } else if (type == boolean.class || type == Boolean.class) {
                    args[i] = false;
                } else if (type == int.class || type == Integer.class) {
                    args[i] = 0;
                } else {
                    return null;
                }
            }

            return ctor.newInstance(args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void attachListenerToConnection(Object connection, Object listener) throws ReflectiveOperationException {
        Method target = null;
        for (Method method : connection.getClass().getMethods()) {
            if (!method.getName().equals("setListener")) {
                continue;
            }
            if (method.getParameterCount() != 1) {
                continue;
            }
            Class<?> param = method.getParameterTypes()[0];
            if (param.isInstance(listener) || param.isAssignableFrom(listener.getClass())) {
                target = method;
                break;
            }
        }
        if (target == null) {
            throw new NoSuchMethodException("No compatible Connection#setListener found");
        }
        target.invoke(connection, listener);
    }

    private void placeNewPlayer(Object server, Object connection, Object serverPlayer, Object cookie)
            throws ReflectiveOperationException {
        Object playerList = server.getClass().getMethod("getPlayerList").invoke(server);

        Method place = null;
        for (Method method : playerList.getClass().getMethods()) {
            if (!method.getName().equals("placeNewPlayer")) {
                continue;
            }
            if (method.getParameterCount() != 3) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (!params[0].isAssignableFrom(connection.getClass())) {
                continue;
            }
            if (!params[1].isAssignableFrom(serverPlayer.getClass())) {
                continue;
            }
            if (!params[2].isAssignableFrom(cookie.getClass())) {
                continue;
            }
            place = method;
            break;
        }

        if (place == null) {
            throw new NoSuchMethodException(
                    "Could not find PlayerList#placeNewPlayer(Connection, ServerPlayer, CommonListenerCookie)");
        }

        place.invoke(playerList, connection, serverPlayer, cookie);
    }

    private Object createListener(Object server, Object connection, Object player, Object cookie) throws ReflectiveOperationException {
        Class<?> listenerClass = Class.forName(CLASS_LISTENER);
        for (Constructor<?> ctor : listenerClass.getConstructors()) {
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length == 4
                    && params[0].isInstance(server)
                    && params[1].isInstance(connection)
                    && params[2].isInstance(player)
                    && params[3].isInstance(cookie)) {
                return ctor.newInstance(server, connection, player, cookie);
            }
        }
        throw new ReflectiveOperationException("No ServerGamePacketListenerImpl constructor found");
    }

    private void setChannel(Object connection) {
        if (connection == null) {
            return;
        }
        try {
            Field channelField = getField(connection.getClass(), "channel");
            channelField.set(connection, new EmbeddedChannel());
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private void setupStatsAndAdvancements(Object server) {
        if (server == null || handle == null) {
            return;
        }
        setField("stats", createStats(server));
        setField("advancements", createAdvancements(server));
    }

    private Object createStats(Object server) {
        try {
            Class<?> statsClass = Class.forName(CLASS_STATS);
            Constructor<?> ctor = statsClass.getConstructor(server.getClass(), File.class);
            return ctor.newInstance(server, new File("npc_stats_" + getUuidString() + ".json"));
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private Object createAdvancements(Object server) {
        File file = new File("npc_advancements_" + getUuidString() + ".json");
        try {
            Class<?> advancementsClass = Class.forName(CLASS_ADVANCEMENTS);
            for (Constructor<?> ctor : advancementsClass.getConstructors()) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length == 3 && params[0].isInstance(server) && params[2].isInstance(handle)) {
                    return ctor.newInstance(server, file, handle);
                }
                if (params.length == 2 && params[0].isInstance(server) && params[1].isInstance(handle)) {
                    return ctor.newInstance(server, handle);
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private void markClientLoaded() {
        if (handle == null) {
            return;
        }
        try {
            Method method = handle.getClass().getDeclaredMethod("setClientLoaded");
            method.setAccessible(true);
            method.invoke(handle);
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Field field = getField(handle.getClass(), "clientLoaded");
            field.setAccessible(true);
            field.setBoolean(handle, true);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private String getUuidString() {
        try {
            Method method = handle.getClass().getMethod("getUUID");
            Object uuid = method.invoke(handle);
            return String.valueOf(uuid);
        } catch (ReflectiveOperationException ex) {
            return "npc";
        }
    }

    private void setField(String name, Object value) {
        if (handle == null) {
            return;
        }
        try {
            Field field = getField(handle.getClass(), name);
            field.setAccessible(true);
            field.set(handle, value);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private Field getField(Class<?> type, String name) throws ReflectiveOperationException {
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
