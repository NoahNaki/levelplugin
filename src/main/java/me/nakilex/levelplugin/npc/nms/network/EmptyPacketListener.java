package me.nakilex.levelplugin.npc.nms.network;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ClientInformation;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class EmptyPacketListener extends ServerGamePacketListenerImpl {
    public EmptyPacketListener(MinecraftServer server, Connection connection, ServerPlayer player) {
        super(server, connection, player, createCookie(player.getGameProfile()));
    }

    private static CommonListenerCookie createCookie(GameProfile profile) {
        ClientInformation info = ClientInformation.createDefault();
        try {
            Method createInitial = CommonListenerCookie.class.getMethod("createInitial", GameProfile.class, ClientInformation.class);
            return (CommonListenerCookie) createInitial.invoke(null, profile, info);
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Constructor<CommonListenerCookie> ctor = CommonListenerCookie.class.getConstructor(GameProfile.class, int.class, ClientInformation.class, boolean.class);
            return ctor.newInstance(profile, 0, info, false);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to create CommonListenerCookie", ex);
        }
    }
}
