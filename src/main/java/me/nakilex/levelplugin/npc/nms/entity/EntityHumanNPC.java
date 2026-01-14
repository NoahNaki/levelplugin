package me.nakilex.levelplugin.npc.nms.entity;

import com.mojang.authlib.GameProfile;
import me.nakilex.levelplugin.npc.nms.network.EmptyConnection;
import me.nakilex.levelplugin.npc.nms.network.EmptyPacketListener;
import me.nakilex.levelplugin.npc.system.NPC;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ClientInformation;
import net.minecraft.stats.ServerStatsCounter;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class EntityHumanNPC extends ServerPlayer {
    private final NPC npc;

    public EntityHumanNPC(MinecraftServer server,
                          ServerLevel level,
                          GameProfile profile,
                          ClientInformation clientInformation,
                          NPC npc) {
        super(server, level, profile, clientInformation);
        this.npc = npc;
        setupNetworkStack(server);
        setupStatsAndAdvancements(server);
        markClientLoaded();
    }

    public NPC getNpc() {
        return npc;
    }

    private void setupNetworkStack(MinecraftServer server) {
        Connection connection = new EmptyConnection(PacketFlow.CLIENTBOUND);
        EmptyPacketListener packetListener = new EmptyPacketListener(server, connection, this);
        connection.setListener(packetListener);
        this.connection = packetListener;
    }

    private void setupStatsAndAdvancements(MinecraftServer server) {
        this.stats = createStats(server);
        this.advancements = createAdvancements(server);
    }

    private ServerStatsCounter createStats(MinecraftServer server) {
        try {
            Constructor<ServerStatsCounter> ctor = ServerStatsCounter.class.getConstructor(MinecraftServer.class, File.class);
            return ctor.newInstance(server, new File("npc_stats_" + getUUID() + ".json"));
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to create dummy stats counter", ex);
        }
    }

    private PlayerAdvancements createAdvancements(MinecraftServer server) {
        File file = new File("npc_advancements_" + getUUID() + ".json");
        try {
            Constructor<PlayerAdvancements> ctor = PlayerAdvancements.class.getConstructor(MinecraftServer.class, File.class, ServerPlayer.class);
            return ctor.newInstance(server, file, this);
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Constructor<?>[] ctors = PlayerAdvancements.class.getConstructors();
            for (Constructor<?> ctor : ctors) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length == 2 && params[0].equals(MinecraftServer.class) && params[1].equals(ServerPlayer.class)) {
                    return (PlayerAdvancements) ctor.newInstance(server, this);
                }
            }
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to create dummy advancements", ex);
        }
        throw new IllegalStateException("No suitable PlayerAdvancements constructor found");
    }

    private void markClientLoaded() {
        try {
            Method method = ServerPlayer.class.getDeclaredMethod("setClientLoaded");
            method.setAccessible(true);
            method.invoke(this);
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Field field = ServerPlayer.class.getDeclaredField("clientLoaded");
            field.setAccessible(true);
            field.setBoolean(this, true);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
