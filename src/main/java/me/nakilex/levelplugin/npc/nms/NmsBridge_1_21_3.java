package me.nakilex.levelplugin.npc.nms;

import com.mojang.authlib.GameProfile;
import me.nakilex.levelplugin.npc.core.PlayerNpc;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
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
    public ServerPlayer createNpcHandle(UUID uuid, String name, Location location) {
        CraftServer craftServer = (CraftServer) Bukkit.getServer();
        MinecraftServer nmsServer = craftServer.getServer();
        ServerLevel level = ((CraftWorld) location.getWorld()).getHandle();
        GameProfile profile = new GameProfile(uuid, name);
        ServerPlayer npc = createServerPlayer(nmsServer, level, profile);
        npc.moveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        npc.setYHeadRot(location.getYaw());
        npc.setYBodyRot(location.getYaw());
        return npc;
    }

    @Override
    public void spawnNpcForViewer(PlayerNpc npc, Player viewer, boolean removeFromTabLater) {
        if (npc == null || viewer == null) {
            return;
        }
        ServerPlayer handle = npc.getHandle();
        var connection = ((CraftPlayer) viewer).getHandle().connection;
        connection.send(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, handle));
        connection.send(createAddEntityPacket(handle));
        List<SynchedEntityData.DataValue<?>> dataValues = handle.getEntityData().getNonDefaultValues();
        if (dataValues == null || dataValues.isEmpty()) {
            dataValues = handle.getEntityData().getAll();
        }
        if (dataValues != null && !dataValues.isEmpty()) {
            connection.send(new ClientboundSetEntityDataPacket(handle.getId(), dataValues));
        }
        if (removeFromTabLater) {
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    connection.send(new ClientboundPlayerInfoUpdatePacket(
                            ClientboundPlayerInfoUpdatePacket.Action.REMOVE_PLAYER, handle)), 3L);
        }
    }

    @Override
    public void despawnNpcForViewer(PlayerNpc npc, Player viewer) {
        if (npc == null || viewer == null) {
            return;
        }
        ServerPlayer handle = npc.getHandle();
        var connection = ((CraftPlayer) viewer).getHandle().connection;
        connection.send(new ClientboundRemoveEntitiesPacket(handle.getId()));
        connection.send(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.REMOVE_PLAYER, handle));
    }

    private Packet<?> createAddEntityPacket(ServerPlayer npc) {
        Packet<?> packet = createAddEntityPacketWithTracker(npc);
        if (packet != null) {
            return packet;
        }
        return createAddEntityPacketDirect(npc);
    }

    private Packet<?> createAddEntityPacketWithTracker(ServerPlayer npc) {
        try {
            Object serverEntity = createServerEntity(npc);
            if (serverEntity == null) {
                return null;
            }
            Constructor<ClientboundAddEntityPacket> ctor = ClientboundAddEntityPacket.class
                    .getConstructor(Entity.class, serverEntity.getClass());
            return ctor.newInstance(npc, serverEntity);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private Packet<?> createAddEntityPacketDirect(ServerPlayer npc) {
        try {
            Constructor<ClientboundAddEntityPacket> ctor = ClientboundAddEntityPacket.class
                    .getConstructor(Entity.class);
            return ctor.newInstance(npc);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to build ClientboundAddEntityPacket", ex);
        }
    }

    private Object createServerEntity(ServerPlayer npc) {
        try {
            Class<?> serverEntityClass = Class.forName("net.minecraft.server.level.ServerEntity");
            for (Constructor<?> ctor : serverEntityClass.getConstructors()) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length < 5) {
                    continue;
                }
                if (!ServerLevel.class.isAssignableFrom(params[0])) {
                    continue;
                }
                if (!Entity.class.isAssignableFrom(params[1])) {
                    continue;
                }
                if (!int.class.equals(params[2]) || !boolean.class.equals(params[3])) {
                    continue;
                }
                Object[] args = new Object[params.length];
                args[0] = npc.serverLevel();
                args[1] = npc;
                args[2] = 0;
                args[3] = true;
                for (int i = 4; i < params.length; i++) {
                    if (Consumer.class.isAssignableFrom(params[i])) {
                        args[i] = (Consumer<Packet<?>>) packet -> {};
                    } else if (BiConsumer.class.isAssignableFrom(params[i])) {
                        args[i] = (BiConsumer<Packet<?>, Packet<?>>) (packet, packet2) -> {};
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

    private ServerPlayer createServerPlayer(MinecraftServer server, ServerLevel level, GameProfile profile) {
        Object clientInformation = createClientInformation();
        for (Constructor<?> ctor : ServerPlayer.class.getConstructors()) {
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length == 4
                    && params[0].isAssignableFrom(MinecraftServer.class)
                    && params[1].isAssignableFrom(ServerLevel.class)
                    && params[2].isAssignableFrom(GameProfile.class)) {
                if (clientInformation != null && params[3].isInstance(clientInformation)) {
                    return (ServerPlayer) invokeServerPlayerCtor(ctor, server, level, profile, clientInformation);
                }
            }
            if (params.length == 3
                    && params[0].isAssignableFrom(MinecraftServer.class)
                    && params[1].isAssignableFrom(ServerLevel.class)
                    && params[2].isAssignableFrom(GameProfile.class)) {
                return (ServerPlayer) invokeServerPlayerCtor(ctor, server, level, profile);
            }
        }
        throw new IllegalStateException("Unable to construct ServerPlayer for NPC");
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
