package me.nakilex.levelplugin.npc.nms.entity;

import com.mojang.authlib.GameProfile;
import me.nakilex.levelplugin.npc.nms.network.EmptyConnection;
import me.nakilex.levelplugin.npc.system.NPC;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.stats.ServerStatsCounter;

import java.io.File;
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
        ServerGamePacketListenerImpl listener = new ServerGamePacketListenerImpl(server, connection, this);
        connection.setListener(listener);
        this.connection = listener;
    }

    private void setupStatsAndAdvancements(MinecraftServer server) {
        this.stats = new ServerStatsCounter(server, new File("npc_stats_" + getUUID() + ".json"));
        this.advancements = new PlayerAdvancements(server, new File("npc_advancements_" + getUUID() + ".json"), this);
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
