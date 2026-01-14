package me.nakilex.levelplugin.npc.nms.entity;

import me.nakilex.levelplugin.npc.system.NPC;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class EntityHumanNPC {
    private static final String CLASS_SERVER_PLAYER = "net.minecraft.server.level.ServerPlayer";
    private static final String CLASS_CONNECTION = "net.minecraft.network.Connection";
    private static final String CLASS_PACKET_FLOW = "net.minecraft.network.PacketFlow";
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
        try {
            Method method = handle.getClass().getMethod("getBukkitEntity");
            return (org.bukkit.entity.Entity) method.invoke(handle);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to access Bukkit entity from ServerPlayer", ex);
        }
    }

    private Object createServerPlayer(Object server, Object level, Object profile, Object clientInformation) {
        try {
            Class<?> serverPlayerClass = Class.forName(CLASS_SERVER_PLAYER);
            Class<?> serverClass = server.getClass();
            Class<?> levelClass = level.getClass();
            for (Constructor<?> ctor : serverPlayerClass.getConstructors()) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length == 4
                        && params[0].isAssignableFrom(serverClass)
                        && params[1].isAssignableFrom(levelClass)
                        && params[2].isInstance(profile)
                        && params[3].isInstance(clientInformation)) {
                    return ctor.newInstance(server, level, profile, clientInformation);
                }
            }
            throw new IllegalStateException("No ServerPlayer constructor found");
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to construct ServerPlayer", ex);
        }
    }

    private void setupNetworkStack(Object server, Object profile, Object clientInformation) {
        try {
            Class<?> connectionClass = Class.forName(CLASS_CONNECTION);
            Class<?> packetFlowClass = Class.forName(CLASS_PACKET_FLOW);
            Object clientbound = Enum.valueOf((Class<Enum>) packetFlowClass, "CLIENTBOUND");
            Constructor<?> connectionCtor = connectionClass.getConstructor(packetFlowClass);
            Object connection = connectionCtor.newInstance(clientbound);

            Object cookie = createListenerCookie(profile, clientInformation);
            Class<?> listenerClass = Class.forName(CLASS_LISTENER);
            Class<?> serverClass = Class.forName("net.minecraft.server.MinecraftServer");
            Constructor<?> listenerCtor = listenerClass.getConstructor(
                    serverClass, connectionClass, Class.forName(CLASS_SERVER_PLAYER), cookie.getClass());
            Object listener = listenerCtor.newInstance(server, connection, handle, cookie);

            Method setListener = connectionClass.getMethod("setListener", Class.forName("net.minecraft.network.PacketListener"));
            setListener.invoke(connection, listener);

            Field connectionField = getField(handle.getClass(), "connection");
            connectionField.set(handle, listener);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to setup NPC network stack", ex);
        }
    }

    private Object createListenerCookie(Object profile, Object clientInformation) throws ReflectiveOperationException {
        Class<?> cookieClass = Class.forName(CLASS_COOKIE);
        try {
            Method createInitial = cookieClass.getMethod("createInitial", profile.getClass(), clientInformation.getClass());
            return createInitial.invoke(null, profile, clientInformation);
        } catch (NoSuchMethodException ignored) {
        }
        for (Constructor<?> ctor : cookieClass.getConstructors()) {
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length >= 4) {
                return ctor.newInstance(profile, 0, clientInformation, false);
            }
        }
        throw new IllegalStateException("No CommonListenerCookie constructor found");
    }

    private void setupStatsAndAdvancements(Object server) {
        setField("stats", createStats(server));
        setField("advancements", createAdvancements(server));
    }

    private Object createStats(Object server) {
        try {
            Class<?> statsClass = Class.forName(CLASS_STATS);
            Constructor<?> ctor = statsClass.getConstructor(server.getClass(), File.class);
            return ctor.newInstance(server, new File("npc_stats_" + getUuidString() + ".json"));
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to create dummy stats counter", ex);
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
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to create dummy advancements", ex);
        }
        throw new IllegalStateException("No suitable PlayerAdvancements constructor found");
    }

    private void markClientLoaded() {
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
