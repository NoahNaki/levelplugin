package me.nakilex.levelplugin.npc.nms;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.nakilex.levelplugin.npc.system.trait.SkinTrait;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.List;

public final class NmsImpl {
    private NmsImpl() {
    }

    public static ServerLevel getServerLevel(World world) {
        try {
            Method handle = world.getClass().getMethod("getHandle");
            return (ServerLevel) handle.invoke(world);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to access ServerLevel handle", ex);
        }
    }

    public static ServerPlayer getServerPlayer(Player player) {
        try {
            Method handle = player.getClass().getMethod("getHandle");
            return (ServerPlayer) handle.invoke(player);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to access ServerPlayer handle", ex);
        }
    }

    public static ServerPlayer getServerPlayer(org.bukkit.entity.Entity entity) {
        if (!(entity instanceof Player player)) {
            return null;
        }
        return getServerPlayer(player);
    }

    public static boolean addEntityToWorld(World world, ServerLevel level, Entity entity) {
        try {
            Method addEntityToWorld = world.getClass().getMethod("addEntityToWorld", Entity.class, SpawnReason.class);
            return (boolean) addEntityToWorld.invoke(world, entity, SpawnReason.CUSTOM);
        } catch (ReflectiveOperationException ignored) {
            return level.addFreshEntity(entity, SpawnReason.CUSTOM);
        }
    }

    public static void addOrRemoveFromPlayerList(ServerLevel level, ServerPlayer player, boolean add) {
        List<ServerPlayer> players = level.players();
        if (add) {
            if (!players.contains(player)) {
                players.add(player);
            }
        } else {
            players.remove(player);
        }
        updateChunkMapPlayerStatus(level, player, add);
    }

    private static void updateChunkMapPlayerStatus(ServerLevel level, ServerPlayer player, boolean add) {
        ServerChunkCache chunkSource = level.getChunkSource();
        try {
            Field chunkMapField = chunkSource.getClass().getDeclaredField("chunkMap");
            chunkMapField.setAccessible(true);
            Object chunkMap = chunkMapField.get(chunkSource);
            invokeChunkMapPlayerMethod(chunkMap, player, add ? "addPlayer" : "removePlayer");
            invokeChunkMapPlayerMethod(chunkMap, player, add ? "addEntity" : "removeEntity");
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void invokeChunkMapPlayerMethod(Object chunkMap, ServerPlayer player, String methodName) {
        if (chunkMap == null) {
            return;
        }
        try {
            Method method = chunkMap.getClass().getMethod(methodName, ServerPlayer.class);
            method.invoke(chunkMap, player);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static void applySkin(Player npcPlayer, SkinTrait skin) {
        if (npcPlayer == null || skin == null) {
            return;
        }
        ServerPlayer handle = getServerPlayer(npcPlayer);
        GameProfile profile = handle.getGameProfile();
        profile.getProperties().removeAll("textures");
        profile.getProperties().put("textures", new Property("textures", skin.getTexture(), skin.getSignature()));
    }

    public static void sendTabListAdd(Player viewer, Player npcPlayer) {
        ServerPlayer viewerHandle = getServerPlayer(viewer);
        ServerPlayer npcHandle = getServerPlayer(npcPlayer);
        ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(Action.ADD_PLAYER),
                List.of(npcHandle));
        viewerHandle.connection.send(packet);
    }

    public static void sendTabListRemove(Player viewer, Player npcPlayer) {
        ServerPlayer viewerHandle = getServerPlayer(viewer);
        ServerPlayer npcHandle = getServerPlayer(npcPlayer);
        ClientboundPlayerInfoRemovePacket packet = new ClientboundPlayerInfoRemovePacket(List.of(npcHandle.getUUID()));
        viewerHandle.connection.send(packet);
    }
}
