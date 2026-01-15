package me.nakilex.levelplugin.npc.system;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.npc.nms.NmsImpl;
import me.nakilex.levelplugin.npc.system.trait.SkinTrait;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class NpcPacketService {
    private static final AtomicInteger ENTITY_ID_COUNTER = new AtomicInteger(200_000);
    private static final long TAB_LIST_REMOVE_DELAY_TICKS = 20L;

    private NpcPacketService() {
    }

    public static NpcPlayer createPlayerNpc(NPC npc, Location location) {
        if (npc == null || location == null) {
            return null;
        }
        UUID uuid = npc.getOrCreateNpcUuid();
        String name = sanitizeProfileName(npc.getName(), npc.getId());
        Object profile = NmsImpl.createGameProfile(uuid, name);
        if (profile == null) {
            return null;
        }
        applySkinToProfile(profile, npc.getTrait(SkinTrait.class));
        int entityId = ENTITY_ID_COUNTER.getAndIncrement();
        return new NpcPlayer(uuid, entityId, name, profile, location);
    }

    public static void showToWorld(NpcPlayer npc, World world) {
        if (npc == null || world == null) {
            return;
        }
        for (Player viewer : world.getPlayers()) {
            showTo(viewer, npc);
        }
    }

    public static void hideFromAll(NpcPlayer npc) {
        if (npc == null) {
            return;
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            hideFrom(viewer, npc);
        }
    }

    public static void showTo(Player viewer, NpcPlayer npc) {
        if (viewer == null || npc == null) {
            return;
        }
        ProtocolManager protocol = ProtocolLibrary.getProtocolManager();
        PacketContainer infoAdd = buildPlayerInfoAdd(npc);
        if (infoAdd != null) {
            sendPacket(protocol, viewer, infoAdd);
        }
        PacketContainer spawn = buildSpawnPacket(npc);
        if (spawn != null) {
            sendPacket(protocol, viewer, spawn);
        }
        PacketContainer head = buildHeadRotation(npc);
        if (head != null) {
            sendPacket(protocol, viewer, head);
        }
        if (infoAdd != null) {
            Bukkit.getScheduler().runTaskLater(Main.getInstance(),
                    () -> sendPacket(protocol, viewer, buildPlayerInfoRemove(npc)),
                    TAB_LIST_REMOVE_DELAY_TICKS);
        }
    }

    public static void hideFrom(Player viewer, NpcPlayer npc) {
        if (viewer == null || npc == null) {
            return;
        }
        ProtocolManager protocol = ProtocolLibrary.getProtocolManager();
        sendPacket(protocol, viewer, buildRemoveEntities(npc));
        sendPacket(protocol, viewer, buildPlayerInfoRemove(npc));
    }

    public static void teleportForAll(NpcPlayer npc, Location location) {
        if (npc == null || location == null) {
            return;
        }
        npc.setLocation(location);
        ProtocolManager protocol = ProtocolLibrary.getProtocolManager();
        PacketContainer teleport = buildTeleportPacket(npc);
        if (teleport == null) {
            return;
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            sendPacket(protocol, viewer, teleport);
        }
    }

    public static void lookFor(Player viewer, NpcPlayer npc, float yaw, float pitch) {
        if (viewer == null || npc == null) {
            return;
        }
        npc.setLocation(new Location(npc.getLocation().getWorld(), npc.getLocation().getX(),
                npc.getLocation().getY(), npc.getLocation().getZ(), yaw, pitch));
        ProtocolManager protocol = ProtocolLibrary.getProtocolManager();
        PacketContainer head = buildHeadRotation(npc);
        if (head != null) {
            sendPacket(protocol, viewer, head);
        }
    }

    private static PacketContainer buildPlayerInfoAdd(NpcPlayer npc) {
        try {
            PacketContainer packet = ProtocolLibrary.getProtocolManager()
                    .createPacket(PacketType.Play.Server.PLAYER_INFO_UPDATE);
            EnumSet<EnumWrappers.PlayerInfoAction> actions = EnumSet.of(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            packet.getPlayerInfoActions().write(0, actions);
            PlayerInfoData data = new PlayerInfoData(
                    createWrappedProfile(npc),
                    0,
                    EnumWrappers.NativeGameMode.SURVIVAL,
                    WrappedChatComponent.fromText(npc.getName()));
            packet.getPlayerInfoDataLists().write(0, Collections.singletonList(data));
            return packet;
        } catch (Exception ex) {
            return null;
        }
    }

    private static PacketContainer buildPlayerInfoRemove(NpcPlayer npc) {
        try {
            PacketContainer packet = ProtocolLibrary.getProtocolManager()
                    .createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
            packet.getUUIDLists().write(0, List.of(npc.getUuid()));
            return packet;
        } catch (Exception ex) {
            return null;
        }
    }

    private static PacketContainer buildSpawnPacket(NpcPlayer npc) {
        try {
            PacketContainer packet = ProtocolLibrary.getProtocolManager()
                    .createPacket(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
            Location loc = npc.getLocation();
            packet.getIntegers().write(0, npc.getEntityId());
            packet.getUUIDs().write(0, npc.getUuid());
            packet.getDoubles().write(0, loc.getX());
            packet.getDoubles().write(1, loc.getY());
            packet.getDoubles().write(2, loc.getZ());
            packet.getBytes().write(0, toAngleByte(loc.getYaw()));
            packet.getBytes().write(1, toAngleByte(loc.getPitch()));
            return packet;
        } catch (Exception ex) {
            return null;
        }
    }

    private static PacketContainer buildHeadRotation(NpcPlayer npc) {
        try {
            PacketContainer packet = ProtocolLibrary.getProtocolManager()
                    .createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
            packet.getIntegers().write(0, npc.getEntityId());
            packet.getBytes().write(0, toAngleByte(npc.getLocation().getYaw()));
            return packet;
        } catch (Exception ex) {
            return null;
        }
    }

    private static PacketContainer buildTeleportPacket(NpcPlayer npc) {
        try {
            PacketContainer packet = ProtocolLibrary.getProtocolManager()
                    .createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
            Location loc = npc.getLocation();
            packet.getIntegers().write(0, npc.getEntityId());
            packet.getDoubles().write(0, loc.getX());
            packet.getDoubles().write(1, loc.getY());
            packet.getDoubles().write(2, loc.getZ());
            packet.getBytes().write(0, toAngleByte(loc.getYaw()));
            packet.getBytes().write(1, toAngleByte(loc.getPitch()));
            packet.getBooleans().write(0, false);
            return packet;
        } catch (Exception ex) {
            return null;
        }
    }

    private static PacketContainer buildRemoveEntities(NpcPlayer npc) {
        try {
            PacketContainer packet = ProtocolLibrary.getProtocolManager()
                    .createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            packet.getIntLists().write(0, List.of(npc.getEntityId()));
            return packet;
        } catch (Exception ex) {
            return null;
        }
    }

    private static WrappedGameProfile createWrappedProfile(NpcPlayer npc) {
        Object profile = npc.getGameProfile();
        if (profile != null) {
            try {
                return WrappedGameProfile.fromHandle(profile);
            } catch (Exception ignored) {
            }
        }
        return new WrappedGameProfile(npc.getUuid(), npc.getName());
    }

    private static void applySkinToProfile(Object profile, SkinTrait skin) {
        if (profile == null || skin == null || skin.getTexture() == null || skin.getSignature() == null) {
            return;
        }
        try {
            Method getProperties = profile.getClass().getMethod("getProperties");
            Object properties = getProperties.invoke(profile);
            if (properties == null) {
                return;
            }
            Method removeAll = properties.getClass().getMethod("removeAll", String.class);
            removeAll.invoke(properties, "textures");
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Object property = propertyClass
                    .getConstructor(String.class, String.class, String.class)
                    .newInstance("textures", skin.getTexture(), skin.getSignature());
            Method put = properties.getClass().getMethod("put", String.class, propertyClass);
            put.invoke(properties, "textures", property);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static byte toAngleByte(float angle) {
        return (byte) ((angle * 256.0F) / 360.0F);
    }

    private static String sanitizeProfileName(String name, int id) {
        String stripped = name == null ? "" : ChatColor.stripColor(name).trim();
        if (stripped.isBlank()) {
            stripped = "NPC-" + id;
        }
        if (stripped.length() > 16) {
            stripped = stripped.substring(0, 16);
        }
        return stripped;
    }

    private static void sendPacket(ProtocolManager protocol, Player viewer, PacketContainer packet) {
        if (protocol == null || viewer == null || packet == null) {
            return;
        }
        try {
            protocol.sendServerPacket(viewer, packet);
        } catch (Exception ignored) {
        }
    }
}
