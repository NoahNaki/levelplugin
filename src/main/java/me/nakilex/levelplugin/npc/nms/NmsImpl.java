package me.nakilex.levelplugin.npc.nms;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.npc.system.trait.SkinTrait;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.world.entity.Entity;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public final class NmsImpl {
    private static final String CLASS_MINECRAFT_SERVER = "net.minecraft.server.MinecraftServer";
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

    public static ServerLevel getServerLevel(World world) {
        try {
            Method handle = world.getClass().getMethod("getHandle");
            return (ServerLevel) handle.invoke(world);
        } catch (ReflectiveOperationException ex) {
            logFailure("Unable to access ServerLevel handle", ex);
            return null;
        }
    }

    public static ServerPlayer getServerPlayer(Player player) {
        try {
            Method handle = player.getClass().getMethod("getHandle");
            return (ServerPlayer) handle.invoke(player);
        } catch (ReflectiveOperationException ex) {
            logFailure("Unable to access ServerPlayer handle", ex);
            return null;
        }
    }

    public static ServerPlayer getServerPlayer(org.bukkit.entity.Entity entity) {
        if (!(entity instanceof Player player)) {
            return null;
        }
        return getServerPlayer(player);
    }

    public static void addEntityToWorld(ServerLevel level, Entity entity) {
        if (level == null || entity == null) {
            return;
        }
        Method method = resolveAddMethod(level, entity instanceof ServerPlayer);
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

    public static void addOrRemoveFromPlayerList(ServerLevel level, ServerPlayer player, boolean add) {
        if (level == null || player == null) {
            return;
        }
        List<ServerPlayer> players = getPlayers(level);
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

    private static List<ServerPlayer> getPlayers(ServerLevel level) {
        if (level == null) {
            return null;
        }
        try {
            Method players = level.getClass().getMethod("players");
            return (List<ServerPlayer>) players.invoke(level);
        } catch (ReflectiveOperationException ex) {
            logFailure("Unable to access level players list", ex);
            return null;
        }
    }

    private static void updateChunkMapPlayerStatus(ServerLevel level, ServerPlayer player, boolean add) {
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

    private static Object getChunkSource(ServerLevel level) {
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
        if (handle == null) {
            return;
        }
        GameProfile profile = handle.getGameProfile();
        if (profile == null) {
            return;
        }
        profile.getProperties().removeAll("textures");
        profile.getProperties().put("textures", new Property("textures", skin.getTexture(), skin.getSignature()));
    }

    public static void sendTabListAdd(Player viewer, Player npcPlayer) {
        ServerPlayer viewerHandle = getServerPlayer(viewer);
        ServerPlayer npcHandle = getServerPlayer(npcPlayer);
        if (viewerHandle == null || npcHandle == null) {
            return;
        }
        ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(Action.ADD_PLAYER),
                List.of(npcHandle));
        viewerHandle.connection.send(packet);
    }

    public static void sendTabListRemove(Player viewer, Player npcPlayer) {
        ServerPlayer viewerHandle = getServerPlayer(viewer);
        ServerPlayer npcHandle = getServerPlayer(npcPlayer);
        if (viewerHandle == null || npcHandle == null) {
            return;
        }
        ClientboundPlayerInfoRemovePacket packet = new ClientboundPlayerInfoRemovePacket(List.of(npcHandle.getUUID()));
        viewerHandle.connection.send(packet);
    }

    public static GameProfile createGameProfile(UUID uuid, String name) {
        return new GameProfile(uuid, name);
    }

    public static ClientInformation createClientInformation() {
        return ClientInformation.createDefault();
    }

    public static void applyPosition(Entity entity, Location location) {
        if (entity == null || location == null) {
            return;
        }
        entity.setPos(location.getX(), location.getY(), location.getZ());
        entity.setYRot(location.getYaw());
        entity.setXRot(location.getPitch());
        entity.setYHeadRot(location.getYaw());
    }

    private static void addPlayer(List<ServerPlayer> players, ServerPlayer player) {
        players.add(player);
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

    private static Method resolveAddMethod(ServerLevel level, boolean isPlayer) {
        if (isPlayer) {
            Method method = addPlayerMethod;
            if (method != null) {
                return method;
            }
            method = findMethod(level.getClass(), new String[]{"addNewPlayer", "addPlayer"}, ServerPlayer.class);
            addPlayerMethod = method;
            if (method != null) {
                return method;
            }
        }
        Method method = addEntityMethod;
        if (method != null) {
            return method;
        }
        method = findMethod(level.getClass(), new String[]{"addFreshEntity", "addEntity"}, Entity.class);
        addEntityMethod = method;
        return method;
    }

    private static Method findMethod(Class<?> owner, String[] names, Class<?> paramType) {
        for (String name : names) {
            try {
                return owner.getMethod(name, paramType);
            } catch (NoSuchMethodException ignored) {
            }
        }
        for (Method method : owner.getMethods()) {
            if (!matchesName(method.getName(), names)) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 1 && params[0].isAssignableFrom(paramType)) {
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
}
